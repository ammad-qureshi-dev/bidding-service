/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.utils;

import com.bidder.bidding_service.http_client.CatalogServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidValidator {

	private final CatalogServiceClient catalogServiceClient;

}
