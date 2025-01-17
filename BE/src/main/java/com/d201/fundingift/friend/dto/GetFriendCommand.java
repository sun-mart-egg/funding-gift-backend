package com.d201.fundingift.friend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetFriendCommand {

    private String socialId;
    private Boolean isFavorite;
}
