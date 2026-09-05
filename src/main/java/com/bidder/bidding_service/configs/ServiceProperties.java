/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.configs;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceProperties {

	@Bean
	@ConfigurationProperties(prefix = "bidder-internal-services")
	public Map<String, String> internalServiceBaseUrls() {
		return new HashMap<>();
	}
}
