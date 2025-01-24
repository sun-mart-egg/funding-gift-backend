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
    private final RedisJwtRepository redisJwtRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // /login-callback 경로에 대한 필터링 제외 처리
        if ("/login-callback".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            if (jwtUtil.validateAccessToken(token)) {
                logger.info("JwtAuthorizationFilter: Valid access token");
                Authentication authentication = jwtUtil.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (jwtUtil.isTokenExpired(token)) {
                logger.info("JwtAuthorizationFilter: Token is expired");
                String userId = jwtUtil.extractUserIdFromExpiredToken(token);
                logger.info("JwtAuthorizationFilter: Extracted userId: " + userId);

                // Redis에서 Refresh Token 조회
                String refreshToken = redisJwtRepository.getRefreshToken(userId);

                if (refreshToken != null && jwtUtil.validateRefreshToken(refreshToken)) {
                    logger.info("JwtAuthorizationFilter: Valid refresh token");
                    // 새 Access Token 발급
                    String newAccessToken = jwtUtil.createAccessToken(userId);
                    SecurityContextHolder.getContext().setAuthentication(jwtUtil.getAuthentication(newAccessToken));
                    response.setHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + newAccessToken);
                } else {
                    logger.info("JwtAuthorizationFilter: Refresh token is invalid or missing");
                    SecurityContextHolder.clearContext();
                }
            } else {
                logger.info("JwtAuthorizationFilter: Access token is invalid");
                SecurityContextHolder.clearContext();
            }

        filterChain.doFilter(request, response);
        logger.info("JwtAuthorizationFilter: Request processed");

        // Todo : 리프레쉬 로직 개발 중
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
