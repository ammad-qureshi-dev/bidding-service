/* (C) 2026
bidder.app */
package com.bidder.bidding_service.utils;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Constants {

	@AllArgsConstructor
	public static class Controller {
		public static final String BASE_URI = "/api";
		public static final String V1 = "/v1";
	}

	public static class Messages {
		public static final String REJECT_REASON_BID_EXPIRED = "Bid reached expiry date, bid is no longer acceptable for this item";
	}
}
