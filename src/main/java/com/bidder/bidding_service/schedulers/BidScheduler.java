/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.schedulers;

import java.time.Instant;

import com.bidder.bidding_service.repository.BidRepository;
import com.bidder.bidding_service.services.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

		var expiredBids = bidRepository.findItemIdsWithExpiredBids();

		if (expiredBids.isEmpty()) {
			log.info("No items found with expired bids @ {}", Instant.now());
			return;
		}

		expiredBids.forEach(bidService::processExpiredBid);

		log.info("BidScheduler: recomputeHighestBidsAfterBidExpires job completed");
	}
}
