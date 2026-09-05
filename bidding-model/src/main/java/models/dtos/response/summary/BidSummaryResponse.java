package models.dtos.response.summary;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.UUID;

import models.entities.BidStatus;

public record BidSummaryResponse(UUID id, UUID itemId, UUID bidderId, BigDecimal amount, BidStatus status, String statusDescription,
								 Instant placedAt, Instant expiresAt) {
}
