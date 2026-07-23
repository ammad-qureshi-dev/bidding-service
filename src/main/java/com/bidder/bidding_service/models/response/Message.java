/* (C) 2026
bidder.app */
package com.bidder.bidding_service.models.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
	private MessageType type;
	private String content;
}
