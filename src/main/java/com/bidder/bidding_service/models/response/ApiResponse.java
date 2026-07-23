/* (C) 2026
bidder.app */
package com.bidder.bidding_service.models.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
	private T data;

	@Builder.Default
	private LocalDateTime completedAt = LocalDateTime.now();

	@Builder.Default
	private UUID requestId = UUID.randomUUID();

	@Builder.Default
	private List<Message> messages = new ArrayList<>();
}
