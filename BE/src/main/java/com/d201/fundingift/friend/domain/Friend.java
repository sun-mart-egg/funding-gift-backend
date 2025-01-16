package com.d201.fundingift.friend.domain;

import com.d201.fundingift.consumer.entity.Consumer;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Friend {

    private Long id;
    private Consumer consumer; // 소비자
    private Consumer toConsumer; // 소비자 친구의 소비자
    private Boolean isFavorite;

    @Builder
    private Friend(Long id, Consumer consumer, Consumer toConsumer, Boolean isFavorite) {
        this.id = id;
        this.consumer = consumer;
        this.toConsumer = toConsumer;
        this.isFavorite = isFavorite;
    }

    public static Friend from(Consumer consumer, Consumer toConsumer, Boolean isFavorite) {
        return Friend.builder()
                .consumer(consumer)
                .toConsumer(toConsumer)
                .isFavorite(isFavorite)
                .build();
    }

    public void toggleFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

}
