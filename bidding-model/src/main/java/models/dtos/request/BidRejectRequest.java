package models.dtos.request;

import jakarta.validation.constraints.NotNull;

public record BidRejectRequest(

		@NotNull String rejectReason) {
}
