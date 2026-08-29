/* (C) 2026
bidder.app */
package com.bidder.bidding_service.controllers;

import java.util.List;
import java.util.UUID;

import com.bidder.bidding_service.services.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.dtos.request.BidRejectRequest;
import models.dtos.request.BidRequest;
import models.dtos.response.summary.BidSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import response.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/bid")
@RequiredArgsConstructor
public class BidController {

	private final BidService bidService;

	@PostMapping
	public ResponseEntity<ApiResponse<UUID>> createBid(@RequestHeader("X-App-User-Id") UUID bidderId,
																@RequestBody BidRequest request) {
		var response = bidService.createBid(request, bidderId);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<UUID>builder().data(response).build());
	}

	@PutMapping("/{bidId}")
	public ResponseEntity<ApiResponse<UUID>> updateBid(@PathVariable UUID bidId, @RequestBody BidRequest request,
			@RequestHeader("X-App-User-Id") UUID bidderId) {
		if (!bidService.isBidOwner(bidId, bidderId)) {
			log.error("Cannot update bid - userId={} does not own bid={}", bidderId, bidId);
			throw new IllegalStateException("Cannot update bid - user does not own bid");
		}

		var response = bidService.updateBid(bidId, request, bidderId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<UUID>builder().data(response).build());
	}

	@PostMapping("/accept/{bidId}")
	public ResponseEntity<ApiResponse<Boolean>> acceptBid(@PathVariable UUID bidId,
			@RequestHeader("X-App-User-Id") UUID bidderId) {

		if (bidService.isBidOwner(bidId, bidderId)) {
			log.error("Cannot accept bid - userId={} does not own bid={}", bidderId, bidId);
			return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(false).build());
		}

		bidService.acceptBid(bidId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(true).build());
	}

	@PostMapping("/reject/{bidId}")
	public ResponseEntity<ApiResponse<Boolean>> rejectBid(@PathVariable UUID bidId,
			@RequestBody BidRejectRequest request, @RequestHeader("X-App-User-Id") UUID bidderId) {

		if (bidService.isBidOwner(bidId, bidderId)) {
			log.error("Cannot reject you're own bid");
			return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(false).build());
		}

		bidService.rejectBid(bidId, request.rejectReason());
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Boolean>builder().data(true).build());
	}

	@GetMapping("/{bidId}")
	public ResponseEntity<ApiResponse<BidSummaryResponse>> getBidById(@PathVariable UUID bidId) {
		var bid = bidService.getBid(bidId);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<BidSummaryResponse>builder().data(bid).build());
	}

	@GetMapping("/my-bids")
	public ResponseEntity<ApiResponse<List<BidSummaryResponse>>> getMyBids(
			@RequestHeader("X-App-User-Id") UUID appUserId) {
		var myBids = bidService.getMyBids(appUserId);
		return ResponseEntity.status(HttpStatus.OK)
				.body(ApiResponse.<List<BidSummaryResponse>>builder().data(myBids).build());
	}

	@GetMapping("/item/{itemId}")
	public ResponseEntity<ApiResponse<List<BidSummaryResponse>>> getBidsForItem(@PathVariable UUID itemId) {
		var bids = bidService.getBidsForItem(itemId);
		return ResponseEntity.status(HttpStatus.OK)
				.body(ApiResponse.<List<BidSummaryResponse>>builder().data(bids).build());
	}
}
