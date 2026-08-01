package com.bidder.bidding_service.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "identity-and-auth-service")
public record IdentityAndAuthServiceProperties(
        String getPreferredContact
) {
}
