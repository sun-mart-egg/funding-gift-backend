package com.d201.fundingift.friend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetFriendResponse {

    private String socialId;
    private Boolean isFavorite;
}
