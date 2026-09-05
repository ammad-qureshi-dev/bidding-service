/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.services;

import java.util.*;
import java.util.stream.Collectors;

import com.bidder.bidding_service.http_client.CatalogServiceClient;
import com.bidder.bidding_service.mappers.BidMapper;
import com.bidder.bidding_service.repository.BidRepository;
import config.EventTopics;
import dtos.request.UpdatedBidRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.TemplateName;
import models.dtos.request.BidRequest;
import models.dtos.request.SendNotificationRequest;
import models.dtos.response.summary.BidSummaryResponse;
import models.entities.Bid;
import models.entities.BidStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import response.ApiMessage;
import response.ApiResponse;
import response.ResponseType;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

	private final BidRepository bidRepository;
	private final CatalogServiceClient catalogServiceClient;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Transactional
	public UUID createBid(BidRequest request, UUID bidderId) {
		var bidId = createNewBid(request, bidderId);

		if (bidId != null) {
			kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
					new SendNotificationRequest(bidderId, TemplateName.BID_REQUEST_SENT, Map.of(), Map.of()));
		}

		return bidId;
	}

	@Transactional
	public UUID updateBid(UUID bidId, BidRequest request, UUID bidderId) {
		var previousBid = getBidById(bidId);

		if (!isOriginalBidder(previousBid, bidderId)) {
			throw new IllegalStateException("The original bidder can only place this bid");
		}

		var newBidId = createNewBid(request, bidderId);

		if (newBidId != null) {
			kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
					new SendNotificationRequest(bidderId, TemplateName.BID_REQUEST_SENT, Map.of(), Map.of()));
		}

		return newBidId;
	}

	private Optional<UUID> updateHighestBid(UpdatedBidRequest request, Bid bid) {
		Optional<UUID> previousHighestBidId = Optional.empty();

		try {
			var previousBid = catalogServiceClient.updateHighestBid(request);

			if (previousBid != null) {
				previousHighestBidId = Optional.of(previousBid);
			}

			bid.setStatus(BidStatus.ACTIVE);
		} catch (Exception e) {
			log.error("Error on updateHighestBid -- request: {}", request, e);
			bid.setStatus(BidStatus.REJECTED);
			bid.setStatusDescription(e.getMessage());
		} finally {
			bidRepository.save(bid);
		}

		return previousHighestBidId;
	}

	public Bid getBidById(UUID id) {
		return bidRepository.findById(id).orElseThrow(() -> new IllegalStateException("Bid cannot be found"));
	}

	public void rejectBid(UUID bidId, String rejectReason) {
		final var bid = getBidById(bidId);

		bid.setStatus(BidStatus.REJECTED);
		bid.setStatusDescription(rejectReason);
		bidRepository.save(bid);

		// ToDo: publish "bid rejected" notification event once Kafka wiring is added

		// If the bid that got rejected was the active bid, promote the next highest
		promoteNextHighestBid(bid.getItemId());
	}

	public Optional<Bid> findNextHighestUnexpiredBidForItem(UUID itemId) {
		var activeBids = bidRepository.findUnexpiredBids(itemId);

		if (activeBids == null || activeBids.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(activeBids.getFirst());
	}

	public void acceptBid(UUID bidId) {
		final var acceptedBid = getBidById(bidId);

		if (bidRepository.existsByItemIdAndStatus(acceptedBid.getItemId(), BidStatus.WINNER)) {
			throw new IllegalStateException("Cannot accept anymore bids, a bid for this item has been selected");
		}

		if (!BidStatus.ACTIVE.equals(acceptedBid.getStatus())) {
			throw new IllegalStateException("This bid cannot be accepted, it is not the highest bid");
		}

		acceptedBid.setStatus(BidStatus.WINNER);
		bidRepository.save(acceptedBid);

		// ToDo: publish "bid accepted" notification event once Kafka wiring is added

		// ToDo: close auction

	}

	public boolean isBidOwner(UUID bidId, UUID userId) {
		final var bid = getBidById(bidId);
		return bid.getBidderId().equals(userId);
	}

	@Deprecated(forRemoval = true)
	public BidSummaryResponse getBid(UUID bidId) {
		return BidMapper.entityToSummary(getBidById(bidId));
	}

	public ApiResponse<List<BidSummaryResponse>> getBids(List<UUID> bidIds) {
		if (bidIds == null || bidIds.isEmpty()) {
			return ApiResponse.<List<BidSummaryResponse>>builder().data(Collections.emptyList()).build();
		}

		var bids = getBidsById(bidIds);

		var foundIds = bids.stream().map(BidSummaryResponse::id).collect(Collectors.toSet());

		var missingIds = bidIds.stream().filter(id -> !foundIds.contains(id)).toList();

		var response = ApiResponse.<List<BidSummaryResponse>>builder().data(bids).build();

		if (!missingIds.isEmpty()) {
			var errorMessages = missingIds.stream()
					.map(e -> ApiMessage.builder().type(ResponseType.WARNING).content("Some bids not found").build())
					.toList();
			response.setMessages(errorMessages);
		}

		return response;
	}

	private List<BidSummaryResponse> getBidsById(List<UUID> bidIds) {
		if (bidIds == null || bidIds.isEmpty()) {
			return Collections.emptyList();
		}

		var bids = bidRepository.findAllById(bidIds);
		return bids.stream().map(BidMapper::entityToSummary).toList();
	}

	public List<BidSummaryResponse> getMyBids(UUID appUserId) {
		var myBids = bidRepository.findBidsByBidderId(appUserId);
		return myBids.stream().map(BidMapper::entityToSummary).toList();
	}

	public List<BidSummaryResponse> getBidsForItem(UUID itemId) {
		return bidRepository.findByItemId(itemId).stream().map(BidMapper::entityToSummary).toList();
	}

	private void promoteNextHighestBid(UUID itemId) {
		findNextHighestUnexpiredBidForItem(itemId).ifPresent(next -> {
			next.setStatus(BidStatus.ACTIVE);
			bidRepository.save(next);
		});
	}

	private static boolean isOriginalBidder(Bid bid, UUID bidderId) {
		return bid.getBidderId().equals(bidderId);
	}

	private UUID createNewBid(BidRequest request, UUID bidderId) {
		// Convert request to object
		var bid = BidMapper.requestToEntity(request);
		bid.setBidderId(bidderId);

		// Set bid to PENDING_VALIDATION
		bid.setStatus(BidStatus.PENDING_VALIDATION);
		bidRepository.save(bid);

		// Validate bid and set current bid as the highest bid for item
		var updateBidRequest = new UpdatedBidRequest(request.auctionId(), request.itemId(), bidderId, bid.getId(),
				request.amount());
		var previousActiveBid = updateHighestBid(updateBidRequest, bid);

		// Deactivate the previous highest bid
		if (previousActiveBid.isPresent()) {
			var prev = getBidById(previousActiveBid.get());
			prev.setStatus(BidStatus.OUTBID);
			bidRepository.save(prev);
		}

		return bid.getId();
	}

}
