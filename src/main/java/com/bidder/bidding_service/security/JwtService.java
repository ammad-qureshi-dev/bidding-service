/* (C) 2026
bidder.app */
package com.bidder.bidding_service.security;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

	@Value("${jwt.secret}")
	private String secretKey;

	public UUID extractAppUserId(String token) {
		String id = extractClaim(token, claims -> claims.get("id", String.class));
		return id != null ? UUID.fromString(id) : null;
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public boolean isTokenValid(String token) {
		return !isTokenExpired(token);
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		return claimsResolver.apply(extractAllClaims(token));
	}

	private boolean isTokenExpired(String token) {
		return extractClaim(token, Claims::getExpiration).before(new Date());
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).getBody();
	}

	private Key getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
