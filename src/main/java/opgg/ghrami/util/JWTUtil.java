//Not being used in the current impl of Ghrami 
package opgg.ghrami.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTUtil {
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000; // 24 hours

    public static String generateToken(Long userId, String username, String email, boolean isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.intValue());
        claims.put("username", username);
        claims.put("email", email);
        claims.put("isAdmin", isAdmin);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static int extractUserId(String token) {
        return extractClaims(token).get("userId", Integer.class);
    }

    public static String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public static String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public static boolean isAdmin(String token) {
        return extractClaims(token).get("isAdmin", Boolean.class);
    }

    public static boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public static boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
