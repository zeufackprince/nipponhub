package com.nipponhub.nipponhubv0.Services;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;

/**
 * The type Jwt utils.
 */
@Component
public class JWTUtils {

    // private SecretKey Key;
    // private  static  final long EXPIRATION_TIME = 86400000;  //24 hours

    // /**
    //  * Instantiates a new Jwt utils.
    //  */
    // public JWTUtils(){
    //     String secreteString = "843567893696976453275974432697R634976R738467TR678T34865R6834R8763T478378637664538745673865783678548735687R3";
    //     byte[] keyBytes = Base64.getDecoder().decode(secreteString.getBytes(StandardCharsets.UTF_8));
    //     this.Key = new SecretKeySpec(keyBytes, "HmacSHA256");
    // }

    @Value("${app.jwt.secret}")
    private String secretString;
    
    @Value("${app.jwt.expiration}")
    private long expiration;
    
    private SecretKey Key;
    
    @PostConstruct
    public void initKey() {
        try {
            if (secretString == null || secretString.isEmpty()) {
                throw new IllegalArgumentException("JWT_SECRET is not configured. Set app.jwt.secret in application.properties or JWT_SECRET environment variable");
            }
            byte[] keyBytes = Base64.getDecoder().decode(secretString.getBytes(StandardCharsets.UTF_8));
            this.Key = new SecretKeySpec(keyBytes, "HmacSHA256");
            
            System.out.println("✓ JWT Key initialized successfully");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to initialize JWT secret. Ensure JWT_SECRET is valid Base64 encoded. Error: " + e.getMessage(), e);
        }
    }

    /**
     * Generate token string.
     *
     * @param userDetails the user details
     * @return the string
     */
    public String generateToken(UserDetails userDetails){
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Key)
                .compact();
    }

    /**
     * Generate refresh token string.
     *
     * @param claims      the claims
     * @param userDetails the user details
     * @return the string
     */
    public  String generateRefreshToken(HashMap<String, Object> claims, UserDetails userDetails){
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Key)
                .compact();
    }

    /**
     * Extract username string.
     *
     * @param token the token
     * @return the string
     */
    public  String extractUsername(String token){
        return  extractClaims(token, Claims::getSubject);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction){
        return claimsTFunction.apply(Jwts.parser().verifyWith(Key).build().parseSignedClaims(token).getPayload());
    }

    /**
     * Is token valid boolean.
     *
     * @param token       the token
     * @param userDetails the user details
     * @return the boolean
     */
    public  boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Is token expired boolean.
     *
     * @param token the token
     * @return the boolean
     */
    public  boolean isTokenExpired(String token){
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }


}

