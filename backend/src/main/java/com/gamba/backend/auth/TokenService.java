package com.gamba.backend.auth;

import com.gamba.backend.user.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// Issues the JWT a client sends back as "Authorization: Bearer <token>"
@Service
public class TokenService {

	private final JwtEncoder encoder;
	private final Duration lifetime;

	public TokenService(JwtEncoder encoder, @Value("${gamba.jwt.expiration-ms}") long expirationMs) {
		this.encoder = encoder;
		this.lifetime = Duration.ofMillis(expirationMs);
	}

	// The subject is the user id, so changing the e-mail keeps the token valid
	public String issue(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(String.valueOf(user.getId()))
				.issuedAt(now)
				.expiresAt(now.plus(lifetime))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
