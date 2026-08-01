package com.bidder.bidding_service.utils;

import com.bidder.bidding_service.http_client.CatalogServiceClient;
import dtos.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import models.entities.Bid;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BidValidator {

    private final CatalogServiceClient catalogServiceClient;
    

}
