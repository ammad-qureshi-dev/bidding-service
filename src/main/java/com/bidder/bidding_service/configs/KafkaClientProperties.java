/* (C) 2026
bidder.app */
package com.bidder.bidding_service.configs;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Extra Kafka client properties (e.g. security.protocol, sasl.*, ssl.*) passed
 * straight through to the producer factory.
 */
@ConfigurationProperties(prefix = "spring.kafka")
public record KafkaClientProperties(Map<String, String> properties) {

	public KafkaClientProperties {
		properties = properties == null ? Map.of() : properties;
	}
}
