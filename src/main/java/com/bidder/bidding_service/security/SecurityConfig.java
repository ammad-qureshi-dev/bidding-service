/* (C) 2026
bidder.app */
package com.bidder.bidding_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.bidder.bidding_service.utils.Constants.Controller.BASE_URI;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/v3/api-docs.yaml",
								"/swagger-resources/**")
						.permitAll()
						// must be matched before the "{bidId}" wildcard below, which would otherwise also
						// swallow this path
						.requestMatchers(HttpMethod.GET, BASE_URI + "/v1/bid/my-bids").authenticated()
						.requestMatchers(HttpMethod.GET, BASE_URI + "/v1/bid/ping", BASE_URI + "/v1/bid/{bidId}",
								BASE_URI + "/v1/bid/item/{itemId}")
						.permitAll().anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
