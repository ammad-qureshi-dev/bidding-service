/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.exceptions;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import response.ApiMessage;
import response.ApiResponse;
import response.ResponseType;

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
	// ToDo: [SECURITY CONCERN] -- returns ex.getMessage() from ANY uncaught RuntimeException
	// (including ones originating from downstream HTTP/DB clients) straight to the API caller.
	// This can leak internal implementation details (service URLs, DB/driver error text, stack
	// info) to unauthenticated clients. Log the full exception server-side and return a generic
	// message to the client instead.
	public ResponseEntity<ApiResponse<?>> handleGeneric(RuntimeException ex, WebRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(generateResponseBody(ex.getMessage()));
	}

	private static ApiResponse<?> generateResponseBody(String exceptionMessage) {
		var message = ApiMessage.builder().type(ResponseType.ERROR).content(exceptionMessage).build();

		return ApiResponse.builder().data(null).messages(List.of(message)).build();
	}
}
