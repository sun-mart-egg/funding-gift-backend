package com.d201.fundingift._common.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisJwtRepository implements JwtRepository {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    @Override
    public void saveAccessToken(Long consumerId, String accessToken) {
        redisTemplate.opsForValue().set("accessToken:" + consumerId, accessToken);
    }

    @Override
    public void saveRefreshToken(Long consumerId, String refreshToken) {
        Claims claims = jwtUtil.parseToken(refreshToken);
        Date expiration = claims.getExpiration(); // JWT의 만료 시간 가져오기
        long duration = expiration.getTime() - System.currentTimeMillis(); // 남은 시간 계산

        redisTemplate.opsForValue().set("refreshToken:" + consumerId, refreshToken, duration,  TimeUnit.MILLISECONDS);
    }

    @Override
    public void saveKakaoAccessToken(Long consumerId, String kakaoAccessToken) {
        redisTemplate.opsForValue().set("kakaoAccessToken:" + consumerId, kakaoAccessToken);
    }

    @Override
    public String getAccessToken(Long consumerId) {
        return redisTemplate.opsForValue().get("accessToken:" + consumerId);
    }

    @Override
    public String getRefreshToken(String consumerId) {
        return redisTemplate.opsForValue().get("refreshToken:" + consumerId);
    }

    @Override
    public String getKakaoAccessToken(Long consumerId) {
        return redisTemplate.opsForValue().get("kakaoAccessToken:" + consumerId);
    }

    @Override
    public void deleteAccessToken(Long consumerId) {
        redisTemplate.delete("accessToken:" + consumerId);
    }

    @Override
    public void deleteRefreshToken(Long consumerId) {
        redisTemplate.delete("refreshToken:" + consumerId);
    }

    @Override
    public void deleteKakaoAccessToken(Long consumerId) {
        redisTemplate.delete("kakaoAccessToken:" + consumerId);
    }
}
