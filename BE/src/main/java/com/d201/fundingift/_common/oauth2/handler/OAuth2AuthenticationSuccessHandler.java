package com.d201.fundingift._common.oauth2.handler;

import com.d201.fundingift._common.jwt.JwtUtil;
import com.d201.fundingift._common.jwt.RedisJwtRepository;
import com.d201.fundingift._common.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.d201.fundingift._common.oauth2.service.OAuth2UserPrincipal;
import com.d201.fundingift._common.oauth2.user.OAuth2UserUnlinkManager;
import com.d201.fundingift._common.oauth2.util.CookieUtils;
import com.d201.fundingift.consumer.service.ConsumerService;
import com.d201.fundingift.friend.service.FriendService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.d201.fundingift._common.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.MODE_PARAM_COOKIE_NAME;
import static com.d201.fundingift._common.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final OAuth2UserUnlinkManager oAuth2UserUnlinkManager;
    private final JwtUtil jwtUtil;
    private final ConsumerService consumerService;
    private final FriendService friendService;
    private final RedisJwtRepository redisJwtRepository;

    /**
     * OAuth2 인증 성공 시 호출되는 메서드
     * 인증 성공 후 리다이렉트할 URL을 결정하고, 리다이렉트 처리
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl;

        targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        logger.debug("redirect targetUrl " + targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);

    }

    /**
     * 리다이렉트할 URL을 결정하는 메서드
     * 프론트엔드에서 전달받은 mode 값에 따라 로그인, 회원가입, 회원탈퇴를 처리
     */
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        // 기본 리다이렉트 URL 설정
        String targetUrl = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(getDefaultTargetUrl());

        // mode 파라미터 가져오기 (login, unlink 등)
        String mode = CookieUtils.getCookie(request, MODE_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("");

        // 인증 정보에서 사용자 정보 추출
        OAuth2UserPrincipal principal = getOAuth2UserPrincipal(authentication);
        if(principal == null) {
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", "Login failed")
                    .build().toUriString();
        }

        // mode에 따라 로그인, 회원가입 또는 회원탈퇴 처리
        switch (mode.toLowerCase()) {
            case "login":
                return consumerService.handleLoginOrRegister(principal, targetUrl);
            case "unlink":
                return consumerService.handleUnlink(principal, targetUrl);
            default:

                return UriComponentsBuilder.fromUriString(targetUrl)
                        .queryParam("error", "Unsupported mode")
                        .build().toUriString();
        }
    }

    /**
     * OAuth2 인증 정보를 추출하는 메서드
     * @param authentication 인증 객체
     * @return OAuth2 사용자 정보 객체
     */
    private OAuth2UserPrincipal getOAuth2UserPrincipal(Authentication authentication) {

        Object principal = authentication.getPrincipal();

        if(principal instanceof OAuth2UserPrincipal) {
            return (OAuth2UserPrincipal) principal;
        }

        return null;
    }

    /**
     * 인증 관련 쿠키 정보를 제거하는 메서드
     */
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {

        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
/**
 * OAuth2 인증 성공시 호출되는 핸들러
 * 프론트앤트에서 백엔드 로그인 요청시 mode 쿼리 파라미터에 담긴 값에 따라 분기하여 처리
 * mode=login -> 사용자 정보 DB 저장, 서비스 액세스 토큰, 리프레시 토큰 생성, 리프레시 토큰 DB 저장
 * mode=unlink -> 각 OAuth2 서비스에 맞는 연결 끊기 API 호출, 사용자 정보/ 리프레시 토큰 DB 삭제
 */
