/* (C) 2026
bidder.app */
package com.bidder.bidding_service.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.bidder.bidding_service.http_client.CatalogServiceClient;
import com.bidder.bidding_service.mappers.BidMapper;
import com.bidder.bidding_service.repository.BidRepository;
import com.bidder.bidding_service.utils.BidValidator;
import config.EventTopics;
import dtos.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import models.TemplateName;
import models.dtos.request.BidRequest;
import models.dtos.request.SendNotificationRequest;
import models.dtos.response.summary.BidSummaryResponse;
import models.entities.Bid;
import models.entities.BidStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BidService {

	private final BidRepository bidRepository;
	private final CatalogServiceClient catalogServiceClient;
	private final KafkaTemplate<String, Object> kafkaTemplate;


	@Transactional
	public UUID createBid(BidRequest request, UUID bidderId) {
		// ToDo: validate the auction is open and within its time window, and that the
		// bidder isn't the auction owner, via a call to catalog-service /
		// identity-auth-service once inter-service communication is wired up
		var bid = BidMapper.requestToEntity(request);
		bid.setBidderId(bidderId);

		validateBidCreation(bid, request.itemId());

		var previousActiveBid = bidRepository.findByItemIdAndStatus(bid.getItemId(), BidStatus.ACTIVE);
		previousActiveBid.ifPresent(prev -> {
			prev.setStatus(BidStatus.OUTBID);
			bidRepository.save(prev);
		});

		bid.setStatus(BidStatus.ACTIVE);
		bidRepository.save(bid);

		// ToDo: publish "bid placed" notification event once Kafka wiring is added
		kafkaTemplate.send(EventTopics.NOTIFICATION.getTopic(),
				new SendNotificationRequest(
						bidderId,
						TemplateName.BID_REQUEST_SENT,
						Map.of(),
						Map.of()
				));

		return bid.getId();
	}

	@Transactional
	public UUID updateBid(UUID bidId, BidRequest request, UUID bidderId) {
		var previousBid = getBidById(bidId);

		var newBid = BidMapper.requestToEntity(request);
		newBid.setBidderId(bidderId);

		validateBidUpdate(previousBid, bidderId, newBid, request.itemId());

		// De-activate old bid
		previousBid.setStatus(BidStatus.OUTBID);
		bidRepository.save(previousBid);

		newBid.setStatus(BidStatus.ACTIVE);
		bidRepository.save(newBid);

		// ToDo: publish "bid updated" notification event once Kafka wiring is added


		return newBid.getId();
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
	}

	public boolean isBidOwner(UUID bidId, UUID userId) {
		final var bid = getBidById(bidId);
		return bid.getBidderId().equals(userId);
	}

	public BidSummaryResponse getBid(UUID bidId) {
		return BidMapper.entityToSummary(getBidById(bidId));
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

	public void validateBidCreation(Bid newBid, UUID itemId) {
		var item = catalogServiceClient.getItemById(itemId);
		if (!isHighestBid(newBid, item)) {
			throw new IllegalStateException("Bid amount must be higher than the current highest bid");
		}

		if (!isAboveMinimumPrice(newBid, item)) {
			throw new IllegalStateException("Bid amount must be higher than the minimum price on the item");
		}
	}

	public void validateBidUpdate(Bid prevBid, UUID bidderId, Bid newBid, UUID itemId) {
		var item = catalogServiceClient.getItemById(itemId);
		if (!isHighestBid(newBid, item)) {
			throw new IllegalStateException("Bid amount must be higher than the current highest bid");
		}

		if (!isAboveMinimumPrice(newBid, item)) {
			throw new IllegalStateException("Bid amount must be higher than the minimum price on the item");
		}

		if (!isOriginalBidder(prevBid, bidderId)) {
			throw new IllegalStateException("The original bidder can only place this bid");
		}
	}

	private static boolean isOriginalBidder(Bid bid, UUID bidderId) {
		return bid.getBidderId().equals(bidderId);
	}

	private static boolean isHighestBid(Bid newBid, ItemResponse item) {
		if (item.highestBidId() == null || item.highestBidAmount() == null) {
			return true;
		}

		return item.highestBidAmount().compareTo(newBid.getAmount()) > 0;
	}

	private static boolean isAboveMinimumPrice(Bid newBid, ItemResponse item) {
		if (item.minimumPrice() == null) {
			return newBid.getAmount() != null;
		}

		return newBid.getAmount().compareTo(item.minimumPrice()) > -1;
	}
}
