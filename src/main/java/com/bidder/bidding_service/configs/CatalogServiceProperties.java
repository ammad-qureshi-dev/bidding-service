/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog-service")
public record CatalogServiceProperties(String getItemById, String getAuctionById, String updateHighestBid) {
}
