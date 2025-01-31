package com.d201.fundingift.friend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Schema(description = "카카오 친구 목록 조회 응답")
@Getter
@ToString
public class GetFriendsResponse {

    @Schema(description = "친구 목록")
    private List<FriendDto> list;

    @Schema(description = "친구의 총 수", example = "11")
    private Integer totalCount;

    @Builder
    private GetFriendsResponse(List<FriendDto> list, Integer totalCount) {
        this.list = list;
        this.totalCount = totalCount;
    }

    public static GetFriendsResponse from(List<FriendDto> list, int totalCount) {
        return GetFriendsResponse.builder()
                .list(list)
                .totalCount(totalCount)
                .build();
    }

}