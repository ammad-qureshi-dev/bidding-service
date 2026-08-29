/* (C) 2026
bidder.app */
package com.bidder.bidding_service.schedulers;

import java.time.LocalDateTime;

import com.bidder.bidding_service.repository.BidRepository;
import com.bidder.bidding_service.services.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.entities.BidStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.bidder.bidding_service.utils.Constants.Messages.REJECT_REASON_BID_EXPIRED;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidScheduler {

	private final BidService bidService;
	private final BidRepository bidRepository;

	@Value("${flag.runRecomputeHighestBidsAfterBidExpires}")
	private boolean runRecomputeHighestBidsAfterBidExpires;

	@Transactional
	@Scheduled(fixedRate = 60000)
	public void recomputeHighestBidsAfterBidExpires() {

		if (!runRecomputeHighestBidsAfterBidExpires) {
			log.info("RecomputeHighestBidsAfterBidExpires flag off");
			return;
		}

		log.info("BidScheduler: starting recomputeHighestBidsAfterBidExpires job...");

		var itemIdsWithExpiredBids = bidRepository.findItemIdsWithExpiredBids();

		if (itemIdsWithExpiredBids.isEmpty()) {
			log.info("No items found with expired bids @ {}", LocalDateTime.now());
			return;
		}

		itemIdsWithExpiredBids.forEach(itemId -> bidRepository.findByItemIdAndStatus(itemId, BidStatus.ACTIVE)
				.filter(bid -> bid.getExpiresAt().isBefore(LocalDateTime.now()))
				.ifPresent(bid -> bidService.rejectBid(bid.getId(), REJECT_REASON_BID_EXPIRED)));

		log.info("BidScheduler: recomputeHighestBidsAfterBidExpires job completed");
	}
}
