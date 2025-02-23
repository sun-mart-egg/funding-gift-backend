package com.d201.fundingift._common.config;

import com.d201.fundingift._common.jwt.JwtAuthorizationFilter;
import com.d201.fundingift._common.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.d201.fundingift._common.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.d201.fundingift._common.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import com.d201.fundingift._common.oauth2.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final JwtAuthorizationFilter jwtAuthorizationFilter;

    @Value("${base-url}")
    private String baseUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용: corsConfigurationSource() 메서드에서 정의한 설정 사용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF 보호 기능 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // HTTP 기본 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // For H2 DB
                .headers(headersConfigurer -> headersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // For H2 DB
                // 세션 정책 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 생성 X, (있더라도) 사용 X
                )
                // 요청에 대한 권한 설정
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(antMatcher("/api/consumers/**")).authenticated()
                        .requestMatchers(antMatcher("/api/funding/**")).authenticated()
                        .requestMatchers(antMatcher("/api/attendance/**")).authenticated()
                        .requestMatchers(antMatcher("/api/payment-info/**")).authenticated()
                        .requestMatchers(antMatcher("/h2-console/**")).permitAll()
                        .requestMatchers(antMatcher("/swagger-ui/")).permitAll()
                        .requestMatchers(antMatcher("/login-callback")).permitAll() // login-callback에 대한 허용 추가
                        .anyRequest().permitAll()
                )
                // OAuth2 로그인 설정
                .oauth2Login(configure ->
                        configure.authorizationEndpoint(config -> config.authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
                                .userInfoEndpoint(config -> config.userService(customOAuth2UserService))
                                .failureHandler(oAuth2AuthenticationFailureHandler) // 로그인 실패 핸들러
                                .successHandler(oAuth2AuthenticationSuccessHandler) // 로그인 성공 핸들러
                );

        http.addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 허용할 출처: 로컬 및 배포 환경 URL
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", baseUrl));
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
        // 쿠키 등 자격 증명 포함 여부
        configuration.setAllowCredentials(true);
        // preflight 요청의 캐싱 시간 (초 단위)
        configuration.setMaxAge(3000L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로에 대해 위의 CORS 설정 적용
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

/**
 * 스프링 시큐리티, OAuth2
 * CSRF설정 Disable
 * OAuth2 핸들러 및 서비스 빈으로 등록
 */
