package com.d201.fundingift.consumer.service;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.jwt.JwtUtil;
import com.d201.fundingift._common.jwt.RedisJwtRepository;
import com.d201.fundingift._common.oauth2.service.OAuth2UserPrincipal;
import com.d201.fundingift._common.oauth2.user.OAuth2Provider;
import com.d201.fundingift._common.oauth2.user.OAuth2UserUnlinkManager;
import com.d201.fundingift._common.util.SecurityUtil;
import com.d201.fundingift.attendance.entity.Attendance;
import com.d201.fundingift.attendance.repository.AttendanceRepository;
import com.d201.fundingift.consumer.dto.request.PutConsumerInfoRequestDto;
import com.d201.fundingift.consumer.dto.response.GetConsumerInfoByIdResponse;
import com.d201.fundingift.consumer.dto.response.GetConsumerMyInfoResponse;
import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.consumer.repository.ConsumerRepository;
import com.d201.fundingift.friend.service.FriendService;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.domain.status.FundingStatus;
import com.d201.fundingift.funding.intrastructure.repository.FundingJPARepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;

import static com.d201.fundingift._common.oauth2.util.CookieUtils.addCookie;
import static com.d201.fundingift._common.oauth2.util.CookieUtils.deleteCookie;
import static com.d201.fundingift._common.response.ErrorType.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsumerService {

    private final ConsumerRepository consumerRepository;
    private final RedisJwtRepository redisJwtRepository;
    private final FundingJPARepository fundingJpaRepository;
    private final AttendanceRepository attendanceRepository;
    private final SecurityUtil securityUtil;
    private final RestTemplate restTemplate;
    private final FriendService friendService;
    private final JwtUtil jwtUtil;
    private final OAuth2UserUnlinkManager oAuth2UserUnlinkManager;

    // socialId로 회원 찾기.
    public Optional<Consumer> findBySocialId(String socialId) {
        return consumerRepository.findBySocialIdAndDeletedAtIsNull(socialId);
    }

    public Consumer findById(Long id) {
        return consumerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow((() -> new CustomException(USER_NOT_FOUND)));
    }

    // 소비자 ID 유효성 검사
    public boolean isValidConsumerId(Long consumerId) {
        return consumerRepository.existsByIdAndDeletedAtIsNull(consumerId);
    }

    // 내 정보 조회
    public GetConsumerMyInfoResponse getConsumerMyInfo(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(USER_UNAUTHORIZED);
        }

        String id;
        if (authentication.getPrincipal() instanceof OAuth2UserPrincipal) {
            id = ((OAuth2UserPrincipal) authentication.getPrincipal()).getUserInfo().getId();
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            id = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            throw new CustomException(USER_NOT_FOUND);
        }

        return GetConsumerMyInfoResponse.from(findById(Long.parseLong(id)));
    }

    // 소비자 프로필 조회
    public GetConsumerInfoByIdResponse getConsumerInfoById(Long consumerId) {
        return GetConsumerInfoByIdResponse.from(consumerRepository.findByIdAndDeletedAtIsNull(consumerId)
                .orElseThrow((() -> new CustomException(USER_NOT_FOUND))));
    }

    /**
     * 로그인 또는 회원가입 처리
     * - 회원 정보가 존재하지 않으면 회원가입 처리
     * - 존재하면 로그인 처리
     */
    public String handleLoginOrRegister(OAuth2UserPrincipal principal, String targetUrl, HttpServletResponse response) {
        String socialId = principal.getUserInfo().getId();
        Optional<Consumer> consumerOptional = findBySocialId(socialId);

        if (consumerOptional.isEmpty()) {
            // 회원가입 처리
            return registerUser(principal, targetUrl, response);
        } else {
            // 로그인 처리
            return loginUser(principal, consumerOptional.get(), targetUrl, response);
        }
    }

    /**
     * 회원가입 처리 로직
     */
    @Transactional
    public String registerUser(OAuth2UserPrincipal principal, String targetUrl, HttpServletResponse response) {
        Long consumerId = saveOAuth2User(principal);
        log.info("회원가입 완료: consumerId={}", consumerId);

        // 토큰 생성 및 저장
        String accessToken = jwtUtil.createAccessToken(consumerId.toString());
        String refreshToken = jwtUtil.createRefreshToken(consumerId.toString());
        redisJwtRepository.saveRefreshToken(consumerId, refreshToken);
        redisJwtRepository.saveKakaoAccessToken(consumerId, principal.getUserInfo().getAccessToken());

        // 친구 목록 가져오기
        friendService.synchronizeFriends(consumerId);

        // 액세스 토큰은 응답 헤더에 전달
        sendTokenResponse(response, accessToken);
        // 리프레쉬 토큰은 HTTP 쿠키에 저장 (쿠키 유효시간은 초 단위)
        addCookie(response, "Refresh-Token", refreshToken, jwtUtil.getRefreshTokenExpiry());

        // 리다이렉션 URL 생성
        return buildRedirectUrl(targetUrl, consumerId, "sign-up");
    }

    // 회원가입
    private Long saveOAuth2User(OAuth2UserPrincipal principal) {
        Consumer consumer = Consumer.builder()
                .socialId(principal.getUserInfo().getId())
                .email(principal.getUserInfo().getEmail())
                .name(principal.getUserInfo().getName())
                .profileImageUrl(principal.getUserInfo().getProfileImageUrl())
                // 필요한 다른 필드 설정
                .build();

        return consumerRepository.save(consumer).getId();
    }

    /**
     * 로그인 처리 로직
     */
    @Transactional
    public String loginUser(OAuth2UserPrincipal principal, Consumer consumer, String targetUrl, HttpServletResponse response) {
        Long consumerId = consumer.getId();

        updateProfile(consumer, principal);

        // ✅ 새로운 Access Token, Refresh Token 발급
        String newAccessToken = jwtUtil.createAccessToken(consumerId.toString());
        String newRefreshToken = jwtUtil.createRefreshToken(consumerId.toString());

        redisJwtRepository.saveRefreshToken(consumerId, newRefreshToken);
        redisJwtRepository.saveKakaoAccessToken(consumerId, principal.getUserInfo().getAccessToken());
        log.info("새로운 Access 및 Refresh Token 발급: consumerId={}", consumerId);

        // 액세스 토큰은 응답 헤더에 전달
        sendTokenResponse(response, newAccessToken);
        // 리프레쉬 토큰은 HTTP 쿠키에 저장
        addCookie(response, "Refresh-Token", newRefreshToken, jwtUtil.getRefreshTokenExpiry());

        return buildRedirectUrl(targetUrl, consumerId, "main");
    }

    /**
     * 🔹 프로필 변경이 필요한 경우에만 실행되는 트랜잭션
     */
    private void updateProfile(Consumer consumer, OAuth2UserPrincipal principal) {
        String newProfileUrl = principal.getUserInfo().getProfileImageUrl();
        consumer.updateProfileImageUrl(newProfileUrl);
        consumerRepository.save(consumer);
        log.info("프로필 업데이트 완료: consumerId={}, newProfileUrl={}", consumer.getId(), newProfileUrl);
    }

    // 토큰을 응답 헤더에 추가하는 메서드
    private void sendTokenResponse(HttpServletResponse response, String accessToken) {
        response.setHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("Access-Control-Expose-Headers", "Authorization");
    }

    // ✅ URL 생성 메서드 분리
    private String buildRedirectUrl(String targetUrl, Long consumerId, String nextPage) {
        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("consumer-id", consumerId)
                .queryParam("next-page", nextPage)
                .build().toUriString();
    }

    // 로그아웃 처리 로직
    @Transactional
    public void logoutUser(HttpServletRequest request, HttpServletResponse response) {
        Long consumerId = Long.valueOf(securityUtil.getConsumer().getId());
        log.info("logoutUser: "+consumerId);

        redisJwtRepository.deleteRefreshToken(consumerId);
        redisJwtRepository.deleteKakaoAccessToken(consumerId);

        // 리프레쉬 토큰 쿠키 삭제 (액세스 토큰은 헤더로 전달되었으므로 별도 쿠키 삭제 필요 없음)
        deleteCookie(request, response, "Refresh-Token");
    }

    /**
     * 회원탈퇴 처리 로직
     */
    @Transactional
    public String handleUnlink(OAuth2UserPrincipal principal, String targetUrl, HttpServletRequest request, HttpServletResponse response) {
        String socialId = principal.getUserInfo().getId();
        String accessToken = principal.getUserInfo().getAccessToken();
        OAuth2Provider provider = principal.getUserInfo().getProvider();
        Consumer consumer = findBySocialId(socialId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Long consumerId = consumer.getId();
        log.info("회원탈퇴 시도: consumerId={}", consumerId);

        // OAuth2 서비스 연결 해제
        oAuth2UserUnlinkManager.unlink(provider, accessToken);

        // Redis 토큰 삭제
        redisJwtRepository.deleteRefreshToken(consumerId);
        redisJwtRepository.deleteKakaoAccessToken(consumerId);

        // 사용자 논리 삭제
        withdrawConsumer(consumerId);

        // 쿠키 삭제
        deleteCookie(request, response, "Refresh-Token");

        log.info("회원탈퇴 완료: consumerId={}", consumerId);

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("next-page", "logout")
                .build().toUriString();
    }

    @Transactional
    public void updateConsumerInfo(PutConsumerInfoRequestDto putConsumerInfoRequestDto) {
        Long consumerId = Long.valueOf(securityUtil.getConsumer().getId());
        Consumer consumer = consumerRepository.findById(consumerId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        log.info("{} 사용자의 추가 정보 기입",consumerId);
        consumer.updateInfo(putConsumerInfoRequestDto);
    }

    @Transactional(readOnly = true)
    public Boolean isConsumerInProgressOrAttendanceFunding() {
        Long consumerId = Long.valueOf(securityUtil.getConsumer().getId());
        log.info("진행 중이거나 참여 중인 펀딩 확인, 사용자 ID: {}", consumerId);

        // 사용자가 참여한 펀딩 중 IN_PROGRESS 상태인 펀딩이 있는지 확인
        List<Attendance> attendances = attendanceRepository.findByConsumerIdAndDeletedAtIsNull(consumerId);
        for (Attendance attendance : attendances) {
            FundingEntity fundingEntity = attendance.getFundingEntity();
            if (fundingEntity.getFundingStatus().equals(FundingStatus.IN_PROGRESS.toString())) {
                log.error("사용자 ID: {}는 진행 중인 펀딩에 참여하고 있습니다.", consumerId);
                return true;
            }
        }

        // 사용자가 생성한 펀딩 중 IN_PROGRESS 상태인 펀딩이 있는지 확인
        List<FundingEntity> userFundingEntities = fundingJpaRepository.findInProgressFundingsByConsumerId(consumerId);
        if (!userFundingEntities.isEmpty()) {
            log.error("사용자 ID: {}가 생성한 진행 중인 펀딩이 있습니다.", consumerId);
            return true;
        }

        return false;
    }

    // 회원탈퇴
    @Transactional
    public void withdrawConsumer(Long consumerId){
        Consumer consumer = consumerRepository.findByIdAndDeletedAtIsNull(consumerId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 소비자 ID 입니다"));

        // 2. 친구 관계 삭제 (예시: 친구 관계 테이블에서 해당 사용자 ID 삭제)
        // friendRepository.deleteByConsumerId(consumerId); // 친구 관련 로직 추가

        // 3. 연관 엔티티 처리 (cascade 설정에 따라 자동 삭제 또는 논리 삭제)
//        consumer.getAttendances().clear(); // 참석 정보 제거
//        consumer.getAddresses().clear();   // 주소 정보 제거
//        consumer.getAccounts().clear();    // 계좌 정보 제거
//        consumer.getReviews().clear();     // 리뷰 정보 제거

        // 4. 논리 삭제 (JPA에서 삭제 처리하면 @SQLDelete가 작동)
        consumerRepository.delete(consumer); // @SQLDelete에 정의된 쿼리가 실행됨
    }
}
