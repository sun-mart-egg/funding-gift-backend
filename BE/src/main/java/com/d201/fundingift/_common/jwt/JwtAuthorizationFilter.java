package com.d201.fundingift._common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

import static com.d201.fundingift._common.oauth2.util.CookieUtils.addCookie;

@RequiredArgsConstructor
@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String LOGIN_CALLBACK_PATH = "/login-callback";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refreshToken:";
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtUtil jwtUtil;
    private final RedisJwtRepository redisJwtRepository;
    //private final RedisTemplate<String, String> redisTemplate; // RedisTemplate 추가

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return LOGIN_CALLBACK_PATH.equals(path);
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 요청 헤더에서 토큰을 추출합니다.
            String token = resolveToken(request);

            // 2. 토큰이 존재하지 않는 경우
            if (!StringUtils.hasText(token)) {
                logger.info("JwtAuthorizationFilter: 요청에서 토큰을 찾을 수 없습니다.");
            }
            // 3. 액세스 토큰이 유효한 경우
            else if (jwtUtil.validateAccessToken(token)) {
                processValidAccessToken(token);
            }
            // 4. 액세스 토큰이 만료된 경우 (리프레쉬 토큰 확인)
            else if (jwtUtil.isTokenExpired(token)) {
                processExpiredAccessToken(token, request, response);
            }
            // 5. 그 외 (액세스 토큰이 유효하지 않은 경우)
            else {
                logger.info("JwtAuthorizationFilter: 액세스 토큰이 유효하지 않습니다.");
            }
        } catch (Exception e){
            logger.error("JwtAuthorizationFilter: SecurityContext에 사용자 인증 정보를 설정하는 중 오류 발생", e);
        }

        filterChain.doFilter(request, response);
    }

    private void processValidAccessToken(String token) {
        logger.info("JwtAuthorizationFilter: 유효한 액세스 토큰이 확인되었습니다.");
        // 토큰 기반으로 인증 정보를 생성하여 SecurityContext에 저장합니다.
        setAuthenticationFromToken(token);
    }

    private void processExpiredAccessToken(String expiredToken, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.info("JwtAuthorizationFilter: 액세스 토큰이 만료되었습니다. 쿠키에 저장된 리프레쉬 토큰을 확인합니다.");

        // 만료된 액세스 토큰에서 사용자 식별자(userId)를 추출합니다.
        String userId = jwtUtil.extractUserIdFromExpiredToken(expiredToken);
        Long consumerId = Long.parseLong(userId);

        // Todo : 클라이언트에 쿠키에 있는 리프레쉬 토큰을 가져오기.

        // 1. 쿠키에서 리프레쉬 토큰을 추출합니다.
        String providedRefreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Refresh-Token".equals(cookie.getName())) {
                    providedRefreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if (!StringUtils.hasText(providedRefreshToken)) {
            logger.info("JwtAuthorizationFilter: 쿠키에 리프레쉬 토큰이 없음 ❌ → 401 반환");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token is required");
            return;
        }

        // 2. Redis에서 해당 사용자의 리프레쉬 토큰을 조회합니다.
        //String refreshTokenKey = REFRESH_TOKEN_KEY_PREFIX + consumerId;
        String storedRefreshToken = redisJwtRepository.getRefreshToken(consumerId);

        // 3. 쿠키에서 제공한 토큰과 Redis에 저장된 토큰이 일치하며, 토큰이 유효한지 확인합니다.
        if (StringUtils.hasText(storedRefreshToken) &&providedRefreshToken.equals(storedRefreshToken) &&
                jwtUtil.validateRefreshToken(providedRefreshToken)) {
            logger.info("JwtAuthorizationFilter: 유효한 리프레쉬 토큰이 확인되었습니다. 새로운 액세스 토큰을 발급합니다.");

            // 새로운 액세스 토큰과 리프레쉬 토큰을 발급합니다.
            String newAccessToken = jwtUtil.createAccessToken(userId);
            String newRefreshToken = jwtUtil.createRefreshToken(userId);

            // Redis에 새로운 리프레쉬 토큰 저장 (기존 토큰 덮어씌우기)
            redisJwtRepository.saveRefreshToken(consumerId,newRefreshToken);

            // SecurityContext를 새로운 액세스 토큰 기반으로 업데이트합니다.
            setAuthenticationFromToken(newAccessToken);

            // 응답 헤더에 새로운 액세스 토큰을 추가합니다.
            response.setHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + newAccessToken);
            response.addHeader("Access-Control-Expose-Headers", AUTHORIZATION_HEADER);

            // 5. 응답 쿠키에 새로운 리프레쉬 토큰을 설정합니다.
            addCookie(response, "Refresh-Token", newRefreshToken, jwtUtil.getRefreshTokenExpiry());
        } else {
            logger.info("JwtAuthorizationFilter: ❌ 리프레쉬 토큰이 유효하지 않음 → 401 반환 (재로그인 필요)");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Refresh token is Invalid"); // 401 반환
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    public void setAuthenticationFromToken(String token) {
        Authentication authentication = jwtUtil.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
