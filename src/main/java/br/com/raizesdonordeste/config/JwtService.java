package br.com.raizesdonordeste.config;

import br.com.raizesdonordeste.domain.ApiException;
import br.com.raizesdonordeste.domain.Identity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JwtService {
    private static final Pattern EXPIRY = Pattern.compile("^(\\d+)(s|m|h|d)?$");
    private final String secret;
    private final String expiry;
    private final String issuer;
    private final String audience;
    private final long expiresIn;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expires-in:8h}") String expiry,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.audience}") String audience) {
        if (secret == null || secret.length() < 32 || secret.equals("SUBSTITUA_POR_UM_SEGREDO_ALEATORIO"))
            throw new IllegalStateException("A configuração de JWT_SECRET requer pelo menos 32 caracteres aleatórios.");
        this.secret = secret;
        this.expiry = expiry;
        this.issuer = issuer;
        this.audience = audience;
        this.expiresIn = parseExpiry(expiry);
    }

    private static long parseExpiry(String value) {
        Matcher matcher = EXPIRY.matcher(value);
        if (!matcher.matches()) throw new IllegalStateException("JWT_EXPIRES_IN inválido. Exemplo: 8h.");
        long multiplier = switch (matcher.group(2) == null ? "s" : matcher.group(2)) {
            case "s" -> 1;
            case "m" -> 60;
            case "h" -> 3600;
            case "d" -> 86400;
            default -> throw new IllegalStateException("JWT_EXPIRES_IN inválido.");
        };
        try {
            long seconds = Math.multiplyExact(Long.parseLong(matcher.group(1)), multiplier);
            if (seconds < 1) throw new IllegalStateException("JWT_EXPIRES_IN inválido.");
            return seconds;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalStateException("JWT_EXPIRES_IN inválido.", exception);
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long expiresIn() { return expiresIn; }

    public String issue(Identity identity) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(Long.toString(identity.id()))
                .claim("id", identity.id())
                .claim("nome", identity.nome())
                .claim("role", identity.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresIn)))
                .signWith(key())
                .compact();
    }

    public Identity verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).requireIssuer(issuer)
                    .requireAudience(audience).build().parseSignedClaims(token).getPayload();
            long id = claims.get("id", Number.class).longValue();
            String nome = claims.get("nome", String.class);
            String role = claims.get("role", String.class);
            if (id < 1 || nome == null || !java.util.Set.of("CLIENTE", "ATENDENTE", "COZINHA", "ADMIN").contains(role))
                throw new JwtException("Invalid identity");
            return new Identity(id, nome, role);
        } catch (RuntimeException exception) {
            throw new ApiException("NAO_AUTENTICADO", "Token inválido ou expirado.", 401);
        }
    }
}
