package com.d201.fundingift.friend.dto.response;

import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.friend.domain.Friend;
import lombok.Builder;
import lombok.Data;

@Data
public class FriendDto {
    private Long id; // friend ID
    private Boolean isFavorite;
    private Long toConsumerId; // 소비자의 친구 소비자 ID
    private String name;
    private String profileImageUrl;

    @Builder
    private FriendDto(Long id,Long toConsumerId, Boolean isFavorite, String name, String profileImageUrl) {
        this.id = id;
        this.toConsumerId = toConsumerId;
        this.isFavorite = isFavorite;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public static FriendDto from(Friend friend, String name, String profileImageUrl) {
        return FriendDto.builder()
                .toConsumerId(friend.getToConsumer().getId())
                .isFavorite(friend.getIsFavorite())
                .name(name)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public static FriendDto from(Friend friend, Consumer toConsumer) {
        return FriendDto.builder()
                .id(friend.getId())
                .isFavorite(friend.getIsFavorite())
                .toConsumerId(toConsumer.getId())
                .name(toConsumer.getName())
                .profileImageUrl(toConsumer.getProfileImageUrl())
                .build();
    }
}
