package com.d201.fundingift._common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@RequiredArgsConstructor
@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 추가

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 특정 경로(login-callback) 필터링 제외
        if ("/login-callback".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        boolean isTokenRefreshed = false;

        logger.info("JwtAuthorizationFilter: Filtering request");

        if (StringUtils.hasText(token)) {
            logger.info("JwtAuthorizationFilter: Token found: " + token);
            if (jwtUtil.validateAccessToken(token)) {
                // ✅ Access Token이 유효하면 인증 정보 설정
                logger.info("JwtAuthorizationFilter: Valid access token");
                Authentication authentication = jwtUtil.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (jwtUtil.isTokenExpired(token)) {
                // ✅ Access Token 만료된 경우 Refresh Token 확인 후 재발급
                logger.info("JwtAuthorizationFilter: Access token expired, checking refresh token...");

                String userId = jwtUtil.extractUserIdFromExpiredToken(token);
                Long consumerId = Long.parseLong(userId);

                // 🪙 Redis에서 Refresh Token 가져오기
                String refreshToken = redisTemplate.opsForValue().get("refreshToken:" + consumerId);

                if (refreshToken != null && jwtUtil.validateRefreshToken(refreshToken)) {
                    logger.info("JwtAuthorizationFilter: Valid refresh token found, issuing new access token.");

                    // 🪙 새로운 Access Token 발급
                    String newAccessToken = jwtUtil.createAccessToken(userId);

                    // 🪙 SecurityContext 업데이트
                    SecurityContextHolder.getContext().setAuthentication(jwtUtil.getAuthentication(newAccessToken));

                    // 🪙 새로운 Access Token을 응답 헤더에 추가
                    response.setHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + newAccessToken);
                    isTokenRefreshed = true;
                } else {
                    logger.info("JwtAuthorizationFilter: No valid refresh token found.");
                }
            } else {
                logger.info("JwtAuthorizationFilter: Access token is invalid");
            }
        } else {
            logger.info("JwtAuthorizationFilter: No token found in the request");
        }

        filterChain.doFilter(request, response);
        logger.info("JwtAuthorizationFilter: Request processed");

        // Todo : 리프레쉬 로직 작성, 정상 작동 하는 지 확인 필요

        // ✅ 새로운 Access Token이 발급된 경우, 클라이언트가 헤더에서 받을 수 있도록 설정
        if (isTokenRefreshed) {
            response.addHeader("Access-Control-Expose-Headers", AUTHORIZATION_HEADER);
            logger.info("JwtAuthorizationFilter: New token added to response header");
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
