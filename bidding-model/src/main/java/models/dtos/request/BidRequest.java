package models.dtos.request;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BidRequest(@NotNull UUID auctionId, @NotNull UUID itemId, @NotNull @DecimalMin("0.0") BigDecimal amount,
		Instant expiresAt) {
}
