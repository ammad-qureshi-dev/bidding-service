/* (C) 2026
bidder.app */
package com.bidder.bidding_service.exceptions;

import java.util.List;
import java.util.NoSuchElementException;

import com.bidder.bidding_service.models.response.ApiResponse;
import com.bidder.bidding_service.models.response.Message;
import com.bidder.bidding_service.models.response.MessageType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<?>> handleIllegalState(IllegalStateException ex, WebRequest request) {
		return ResponseEntity.badRequest().body(generateResponseBody(ex.getMessage()));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ApiResponse<?>> handleNotFound(NoSuchElementException ex, WebRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(generateResponseBody(ex.getMessage()));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiResponse<?>> handleGeneric(RuntimeException ex, WebRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(generateResponseBody(ex.getMessage()));
	}

	private static ApiResponse<?> generateResponseBody(String exceptionMessage) {
		var message = Message.builder().type(MessageType.ERROR).content(exceptionMessage).build();

		return ApiResponse.builder().data(null).messages(List.of(message)).build();
	}
}
