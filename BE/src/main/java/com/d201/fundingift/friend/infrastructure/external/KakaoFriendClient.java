package com.d201.fundingift.friend.infrastructure.external;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.jwt.RedisJwtRepository;
import com.d201.fundingift.friend.domain.port.FriendExternalPort;
import com.d201.fundingift.friend.dto.GetFriendCommand;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.d201.fundingift._common.response.ErrorType.KAKAO_FRIEND_NOT_FOUND;

@RequiredArgsConstructor
@Component
public class KakaoFriendClient implements FriendExternalPort {

    private final RedisJwtRepository redisJwtRepository;

    private static final String FRIENDS_LIST_SERVICE_URL = "https://kapi.kakao.com/v1/api/talk/friends?limit=50";

    @Override
    public List<GetFriendCommand> getFriends(Long consumerId) {

        // todo : optional로 바꾸기 or 예외 처리 하기
        // 카카오 엑세스 토큰 가져오기
        String kakaoAccessToken = redisJwtRepository.getKakaoAccessToken(consumerId);

        List<GetFriendCommand> list = new ArrayList<>();
        String nextUrl = FRIENDS_LIST_SERVICE_URL;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("Authorization", "Bearer " + kakaoAccessToken);
            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            while (nextUrl != null) {
                HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                        nextUrl,
                        HttpMethod.GET,
                        httpEntity,
                        JsonNode.class
                );

                response.getBody()
                        .get("elements")
                        .forEach(element -> list.add(GetFriendCommand.builder()
                                        .favorite(element.get("favorite").asBoolean())
                                        .socialId(element.get("id").asText())
                                .build()));

                // 다음 페이지 URL 업데이트
                nextUrl = response.getBody().has("after_url") && !response.getBody().get("after_url").isNull()
                        ? response.getBody().get("after_url").asText()
                        : null;
            }

            return list;
        } catch (HttpClientErrorException e) {
            throw new CustomException(KAKAO_FRIEND_NOT_FOUND);
        }
    }
}
