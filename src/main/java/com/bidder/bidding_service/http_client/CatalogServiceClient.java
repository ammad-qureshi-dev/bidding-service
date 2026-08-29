package com.bidder.bidding_service.http_client;

import com.bidder.bidding_service.configs.CatalogServiceProperties;
import dtos.response.AuctionResponse;
import dtos.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import response.ApiResponse;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static utils.Contants.ServiceNames.CATALOG_SERVICE;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogServiceClient {

    private final RestClient restClient;

    private final CatalogServiceProperties props;

    private final Map<String, String> internalServiceBaseUrls;

    public ItemResponse getItemById(UUID itemId) {
        var baseUrl = internalServiceBaseUrls.get(CATALOG_SERVICE);

        var url = UriComponentsBuilder.fromUriString(baseUrl + props.getItemById()).buildAndExpand(itemId).toUriString();

        try {
            log.info("Calling {}", url);
            var item = restClient.get().uri(url).retrieve().body(new ParameterizedTypeReference<ApiResponse<ItemResponse>>() {
            });

            if (item == null) {
                log.error("Response was null for request {}", url);
                throw new RuntimeException("No response received from catalog-service. Item not found. Please check logs");
            }

            return item.getData();
        } catch (RuntimeException e) {
            log.error("Item with ID = {} not found", itemId);
            throw new NoSuchElementException("Item not found");
        }
    }

    public AuctionResponse getAuctionById(UUID auctionId) {
        var baseUrl = internalServiceBaseUrls.get(CATALOG_SERVICE);

        var url = UriComponentsBuilder.fromUriString(baseUrl + props.getAuctionById()).buildAndExpand(auctionId).toUriString();

        try {
            log.info("Calling {}", url);
            var auction = restClient.get().uri(url).retrieve().body(new ParameterizedTypeReference<ApiResponse<AuctionResponse>>() {
            });

            if (auction == null) {
                log.error("Response was null for request {}", url);
                throw new NoSuchElementException("No response received from catalog-service. Auction not found. Please check logs");
            }

            return auction.getData();
        } catch (RuntimeException e) {
            log.error("Internal error. Could not find auction (id = {}): ", auctionId, e);
            throw new RuntimeException("Auction not found");
        }
    }
}
