/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.mappers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import models.dtos.request.BidRequest;
import models.dtos.response.summary.BidSummaryResponse;
import models.entities.Bid;

public class BidMapper {

	// ToDo: default to the auction's end time (via catalog-service lookup) instead
	// of a fixed window once inter-service communication is wired up
	private static final int DEFAULT_EXPIRY_DAYS = 7;

	public static Bid requestToEntity(BidRequest request) {
		var timeNow = Instant.now();
		return Bid.builder().auctionId(request.auctionId()).itemId(request.itemId()).amount(request.amount())
				.placedAt(timeNow)
				.expiresAt(request.expiresAt() == null
						? timeNow.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS)
						: request.expiresAt())
				.build();
	}

	public static BidSummaryResponse entityToSummary(Bid b) {
		if (b == null) {
			return null;
		}

		return new BidSummaryResponse(b.getId(), b.getItemId(), b.getBidderId(), b.getAmount(), b.getStatus(),
				b.getStatusDescription(), b.getPlacedAt(), b.getExpiresAt());
	}
}
