package models.dtos.response.summary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import models.entities.BidStatus;

public record BidSummaryResponse(UUID id, UUID itemId, BigDecimal amount, BidStatus status, String statusDescription,
		LocalDateTime placedAt, LocalDateTime expiresAt) {
}
