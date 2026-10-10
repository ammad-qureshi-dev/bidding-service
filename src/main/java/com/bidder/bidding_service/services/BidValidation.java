/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.services;

import java.util.UUID;

import com.bidder.bidding_service.http_client.CatalogServiceClient;
import com.bidder.bidding_service.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import models.entities.Bid;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidValidation {
	private final BidRepository bidRepository;
	private final CatalogServiceClient catalogService;

	public void rejectBidValidation(Bid bid, UUID ownerId) throws IllegalAccessException {
		if (bid == null) {
			throw new NullPointerException("Null bid was provided");
		}

		if (bid.getBidderId().equals(ownerId)) {
			throw new IllegalAccessException("Owner cannot reject their own bid");
		}

		final var auction = catalogService.getAuctionById(bid.getAuctionId());
		if (!auction.ownerId().equals(ownerId)) {
			throw new IllegalAccessException("Cannot reject this bid as you don't own this bid");
		}
	}
}
