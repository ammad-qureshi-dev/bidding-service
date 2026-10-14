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
import dtos.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.TemplateName;
import models.dtos.request.BidRejectRequest;
import models.dtos.request.BidRequest;
import models.dtos.request.NotifyRequest;
import models.dtos.response.summary.BidSummaryResponse;
import models.entities.Bid;
import models.entities.BidStatus;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${client-url}")
	private String clientUrl;

	private final BidRepository bidRepository;
	private final CatalogServiceClient catalogServiceClient;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final BidValidation bidValidation;

	@Transactional
	public UUID createBid(BidRequest request, UUID bidderId) throws IllegalAccessException {
		var bidId = createNewBid(request, bidderId);

		if (bidId != null) {
			kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
					new NotifyRequest(bidderId, TemplateName.BID_REQUEST_SENT, null,
							Map.of("bidAmount", request.amount(), "itemName", request.itemName(), "auctionUrl",
									generateAuctionUrl(request.auctionId(), request.itemId()))));
		}

		return bidId;
	}

	@Transactional
	public UUID updateBid(UUID bidId, BidRequest request, UUID bidderId) throws IllegalAccessException {
		var previousBid = getBidById(bidId);

		if (!isOriginalBidder(previousBid, bidderId)) {
			throw new IllegalStateException("The original bidder can only place this bid");
		}

		var newBidId = createNewBid(request, bidderId);

		if (newBidId != null) {
			kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
					new NotifyRequest(bidderId, TemplateName.BID_REQUEST_SENT, null, Map.of()));
		}

		return newBidId;
	}

	private Optional<UUID> updateHighestBid(UpdatedBidRequest request, Bid bid) {
		Optional<UUID> previousHighestBidId = Optional.empty();

		try {
			var previousBid = catalogServiceClient.setHighestBidForItem(request);

			if (previousBid != null) {
				previousHighestBidId = Optional.of(previousBid);
				bid.setStatus(BidStatus.ACTIVE);
			} else {
				bid.setStatus(BidStatus.REJECTED);
				bid.setStatusDescription("Item not accepting bids");
			}

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

	public void rejectBid(UUID bidId, UUID ownerId, BidRejectRequest request) throws IllegalAccessException {
		final var bid = getBidById(bidId);
		bidValidation.rejectBidValidation(bid, ownerId);
		rejectBidHelper(bid, request);
	}

	public void rejectBid(UUID bidId, BidRejectRequest request) {
		final var bid = getBidById(bidId);
		rejectBidHelper(bid, request);
	}

	public Optional<Bid> findNextHighestUnexpiredBidForItem(UUID itemId) {
		var activeBids = bidRepository.findMaxValidBidByStatus(itemId);

		if (activeBids == null || activeBids.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(activeBids.getFirst());
	}

	public void acceptBid(UUID bidId, UUID ownerId) throws IllegalAccessException {
		final var bid = getBidById(bidId);
		final var itemResponse = catalogServiceClient.getItemById(bid.getItemId());
		if (itemResponse.isEmpty()) {
			log.error("Item {} not found for bid {}", bid.getItemId(), bidId);
			throw new NoSuchElementException("Item not found. Cannot accept bid");
		}

		if (bid.getBidderId().equals(ownerId)) {
			throw new IllegalAccessException("Owner cannot accept their own bid");
		}

		final var auction = catalogServiceClient.getAuctionById(bid.getAuctionId());
		if (!auction.ownerId().equals(ownerId)) {
			throw new IllegalAccessException("Cannot accept this bid as you don't own this bid");
		}

		final var acceptedBid = getBidById(bidId);

		if (bidRepository.existsByItemIdAndStatus(acceptedBid.getItemId(), BidStatus.WINNER)) {
			throw new IllegalStateException("Cannot accept anymore bids, a bid for this item has been selected");
		}

		if (!BidStatus.ACTIVE.equals(acceptedBid.getStatus())) {
			throw new IllegalStateException("This bid cannot be accepted, it is not the highest bid");
		}

		acceptedBid.setStatus(BidStatus.WINNER);
		bidRepository.save(acceptedBid);

		final var item = itemResponse.get();

		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new NotifyRequest(bid.getBidderId(), TemplateName.BID_REQUEST_ACCEPTED, null,
						Map.of("title", item.title(), "auctionUrl", generateAuctionHomeUrl())));

		catalogServiceClient.setAcceptingBids(item.id(), false);
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

	public List<BidSummaryResponse> getMyBids(UUID appUserId) {
		var myBids = bidRepository.findBidsByBidderId(appUserId);
		return myBids.stream().map(BidMapper::entityToSummary).toList();
	}

	public List<BidSummaryResponse> getBidsForItem(UUID itemId) {
		return bidRepository.findByItemId(itemId).stream().map(BidMapper::entityToSummary).toList();
	}

	public List<BidSummaryResponse> getTopTenBidsForItem(UUID itemId) {
		return bidRepository.findTopTenBids(itemId).stream().map(BidMapper::entityToSummary).toList();
	}

	private static boolean isOriginalBidder(Bid bid, UUID bidderId) {
		return bid.getBidderId().equals(bidderId);
	}

	@Transactional
	private void rejectBidHelper(Bid bid, BidRejectRequest request) {
		bid.setStatus(BidStatus.REJECTED);
		bid.setStatusDescription(request.rejectReason());
		bidRepository.save(bid);

		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new NotifyRequest(bid.getBidderId(), TemplateName.BID_REQUEST_REJECTED, null, Map.of("itemName",
						request.itemName(), "bidAmount", bid.getAmount(), "appUrl", generateAuctionHomeUrl())));

		promoteBid(bid, request.itemName());
	}

	private void promoteBid(Bid bid, String title) {
		// If the bid that got rejected was the active bid, promote the next highest
		var nextBid = findNextHighestUnexpiredBidForItem(bid.getItemId());

		if (nextBid.isEmpty()) {
			catalogServiceClient
					.setHighestBidForItem(new UpdatedBidRequest(bid.getAuctionId(), bid.getItemId(), null, null, null));
			return;
		} else {
			catalogServiceClient.setHighestBidForItem(new UpdatedBidRequest(bid.getAuctionId(), bid.getItemId(),
					bid.getBidderId(), bid.getId(), nextBid.get().getAmount()));

			kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(), new NotifyRequest(bid.getBidderId(),
					TemplateName.LEADING_BID, null, Map.of("title", title, "auctionUrl", generateAuctionHomeUrl())));
		}

		var nextHighestBid = nextBid.get();
		nextHighestBid.setStatus(BidStatus.ACTIVE);
		bidRepository.save(nextHighestBid);
	}

	public void processExpiredBid(final Bid bid) {
		bid.setStatus(BidStatus.EXPIRED);

		var item = catalogServiceClient.getItemById(bid.getItemId());

		// If the item doesn't exist, we can skip promoting bids to avoid unnecessary
		// updates
		if (item.isEmpty()) {
			log.error("processExpiredBid - item {} not found", bid.getItemId());
			bid.setStatusDescription("Bid was expired -- item not found");
			bidRepository.save(bid);
			return;
		}

		bid.setStatusDescription("Bid was expired");
		bidRepository.save(bid);

		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new NotifyRequest(bid.getBidderId(), TemplateName.EXPIRED_BID, null,
						Map.of("amount", bid.getAmount(), "auctionUrl", generateAuctionHomeUrl())));

		promoteBid(bid, item.get().title());
	}

	private UUID createNewBid(BidRequest request, UUID bidderId) throws IllegalAccessException {
		ItemResponse item;
		var itemResponse = catalogServiceClient.getItemById(request.itemId());
		if (itemResponse.isEmpty()) {
			throw new IllegalAccessException("Item does not exist. Cannot place bid");
		}

		item = itemResponse.get();

		if (!item.acceptingBids()) {
			throw new IllegalStateException(
					"Item cannot accept anymore bids. A winner has been chosen already. Feel free to browse other items");
		}

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

	private List<BidSummaryResponse> getBidsById(List<UUID> bidIds) {
		if (bidIds == null || bidIds.isEmpty()) {
			return Collections.emptyList();
		}

		var bids = bidRepository.findAllById(bidIds);
		return bids.stream().map(BidMapper::entityToSummary).toList();
	}

	private String generateAuctionUrl(UUID auctionId, UUID itemId) {
		return clientUrl + "/auctions/" + auctionId + "/items/" + itemId;
	}

	private String generateAuctionHomeUrl() {
		return clientUrl + "/auctions";
	}

	public void internalBulkUpdateBidsStatuses(List<UUID> bidIds, BidStatus status) {
		if (bidIds == null || bidIds.isEmpty()) {
			log.info("No bids were provided to bulk update status");
			return;
		}

		var bids = bidRepository.findAllById(bidIds);
		bids.forEach(e -> e.setStatus(status));
		bidRepository.saveAll(bids);
	}
}
