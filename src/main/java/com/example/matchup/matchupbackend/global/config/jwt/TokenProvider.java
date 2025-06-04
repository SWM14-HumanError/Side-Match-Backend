package com.example.matchup.matchupbackend.global.config.jwt;

import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.error.exception.AuthorizeException;
import com.example.matchup.matchupbackend.error.exception.ExpiredTokenException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.global.config.oauth.dto.OAuth2LoginUrl;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import io.jsonwebtoken.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Getter
@Service
public class TokenProvider {

    private final JwtProperties jwtProperties;
    private final OAuth2LoginUrl oAuth2LoginUrl;
    private final UserRepository userRepository;
    private final Environment environment;

    public final static String HEADER_AUTHORIZATION = "Authorization";
    public final static String TOKEN_PRIFIX = "Bearer ";

    public String generateToken(User user, Duration expiredAt) {

        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user);
    }

    private String makeToken(Date expiry, User user) {

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .setSubject(user.getEmail())
                .claim("id", user.getId())
                .claim("tokenId", user.getTokenId())
                .claim("role", user.getRole())
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())
                .compact();
    }

    public TokenStatus validToken(String authorizationHeader) {
        String token = getAccessToken(authorizationHeader);

        try {
            Jwts.parser()
                    .setSigningKey(jwtProperties.getSecretKey())
                    .parseClaimsJws(token);
            return TokenStatus.VALID;
        }
        catch (ExpiredJwtException ex) {
            return TokenStatus.EXPIRED;
        }
        catch (Exception e) {
//            log.info("token is not valid: {}", e.getMessage());
//            log.info("token: {}", token);
            return TokenStatus.INVALID_OTHER;
        }
    }

    public boolean validTokenInFilter(String authorizationHeader) throws ExpiredJwtException{
        String token = getAccessToken(authorizationHeader);

        try {
            Jwts.parser()
                    .setSigningKey(jwtProperties.getSecretKey())
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication getAuthentication(String authorizationHeader) {
        String token = getAccessToken(authorizationHeader);

        Claims claims = getClaims(token);
        Set<SimpleGrantedAuthority> authorities =
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(claims.get("id", Integer.class).toString(), "", authorities),
                token,
                authorities);
    }

    /**
     * Bearer 토큰을 받아 user를 반환
     */
    public Long getUserId(String authorizationHeader, String callBack) {
        if (authorizationHeader != null && Arrays.asList(environment.getActiveProfiles()).contains("local") && authorizationHeader.startsWith("test")) {
            return userRepository.findUserByNickname(authorizationHeader).orElseThrow(() -> new UserNotFoundException("가입 된 유저가 아닙니다.")).getId();
        }

        String token = getAccessToken(authorizationHeader);

        if (token == null) return null;
        if (validToken(token) == TokenStatus.INVALID_OTHER) {
            throw new AuthorizeException(callBack);
        } else if (validToken(token) == TokenStatus.EXPIRED) {
            throw new ExpiredTokenException(callBack);
        }

        Claims claims = getClaims(token);
        Long userId = claims.get("id", Long.class);
        String tokenId = claims.get("tokenId", String.class);
        User user = userRepository.findUserById(userId).orElseThrow(() -> new UserNotFoundException("존재하지 않은 아이디를 가진 토큰으로 접근했습니다."));

        if (!user.getTokenId().equals(tokenId)) {
            throw new ExpiredTokenException("관리자에 의해 만료된 토큰입니다.");
        }
        return user.getId();
    }

    public User getUser(String authorizationHeader, String callBack) {
        if (authorizationHeader != null && Arrays.asList(environment.getActiveProfiles()).contains("local") && authorizationHeader.startsWith("test")) {
            return userRepository.findUserByNickname(authorizationHeader).orElseThrow(() -> new UserNotFoundException("가입 된 유저가 아닙니다."));
        }

        String token = getAccessToken(authorizationHeader);

        if (token == null) return null;
        if (validToken(token) == TokenStatus.INVALID_OTHER) {
            throw new AuthorizeException(callBack);
        } else if (validToken(token) == TokenStatus.EXPIRED) {
            throw new ExpiredTokenException(callBack);
        }

        Claims claims = getClaims(token);
        Long userId = claims.get("id", Long.class);
        String tokenId = claims.get("tokenId", String.class);
        User user = userRepository.findUserById(userId).orElseThrow(() -> new UserNotFoundException("존재하지 않은 아이디를 가진 토큰으로 접근했습니다."));
        if (!user.getTokenId().equals(tokenId)) {
            throw new ExpiredTokenException("관리자에 의해 만료된 토큰입니다.");
        }
        return user;
    }

    public Long getUserId(String authorizationHeader) {
        return getUserId(authorizationHeader, "");
    }

    private Claims getClaims(String authorizationHeader) {
        String token = getAccessToken(authorizationHeader);

        return Jwts.parser()
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Authorization in Header에서 Bearer을 제거하여 토큰만 추출
     * @param authorizationHeader: Bearer token
     */
    private String getAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PRIFIX)) {
            return authorizationHeader.substring(TOKEN_PRIFIX.length());
        }
        return authorizationHeader;
    }
}
