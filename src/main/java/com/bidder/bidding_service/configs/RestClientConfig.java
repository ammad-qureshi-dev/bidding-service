/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	// ToDo: [SECURITY CONCERN] -- no connect/read timeouts are configured on this RestClient. A
	// slow or hanging catalog-service (or an attacker controlling/MITM-ing that endpoint) can
	// tie up request threads indefinitely, creating a denial-of-service vector for this service.
	public RestClient restClient(RestClient.Builder builder) {
		return builder.build();
	}
}
