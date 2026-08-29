/* (C) 2026
bidder.app */
package com.bidder.bidding_service.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.AppUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public static final String TOKEN_COOKIE = "token";

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		String jwt = null;
		if (request.getCookies() != null) {
			jwt = Arrays.stream(request.getCookies()).filter(cookie -> TOKEN_COOKIE.equals(cookie.getName()))
					.map(Cookie::getValue).findFirst().orElse(null);
		}

		if (jwt == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String username = jwtService.extractUsername(jwt);

		if (!Objects.isNull(username) && Objects.isNull(SecurityContextHolder.getContext().getAuthentication())) {
			if (jwtService.isTokenValid(jwt)) {
				UUID userId = jwtService.extractAppUserId(jwt);

				AppUser principal = new AppUser(userId, username, null, null);

				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
						null, principal.getAuthorities());

				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}

		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		String path = request.getServletPath();

		return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
				|| path.startsWith("/swagger-resources") || path.equals("/swagger-ui.html");
	}
}
