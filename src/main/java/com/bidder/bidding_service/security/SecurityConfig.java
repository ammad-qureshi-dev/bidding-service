/* (C) 2026 
bidder.app */
package com.bidder.bidding_service.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) {
		// ToDo: [SECURITY CONCERN] -- anyRequest().permitAll() means every endpoint
		// (including
		// bid creation, accept/reject) is reachable with no authentication at all.
		// There is a
		// JWT_SECRET configured in application-local.properties but no filter here ever
		// validates
		// a token, so identity is entirely determined by the client-supplied
		// X-App-User-Id header.
		// Wire up JWT authentication/authorization before this service is exposed
		// beyond localhost.
		http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(
						exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
							response.setContentType("application/json");
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						}).accessDeniedHandler((request, response, accessDeniedException) -> {
							response.setContentType("application/json");
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						}));

		return http.build();
	}
}
