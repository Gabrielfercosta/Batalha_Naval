package com.batalha.Batalha_Naval.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave = Keys.hmacShaKeyFor(
            "chave-super-secreta-que-nao-vai-dar-erro-de-ser-curta-demais-pqp".getBytes()
    );

    private final long validadeMs = 1000 * 60 * 60 * 24;

    public String gerarToken(String username) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + validadeMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String extrairUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }
}
