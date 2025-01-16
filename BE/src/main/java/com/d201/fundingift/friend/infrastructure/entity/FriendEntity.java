package com.d201.fundingift.friend.infrastructure.entity;

import com.d201.fundingift._common.entity.BaseTime;
import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.friend.domain.Friend;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity(name = "Friend")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumer_id", referencedColumnName = "consumer_id", nullable = false)
    private Consumer consumer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_consumer_id", referencedColumnName = "consumer_id", nullable = false)
    private Consumer toConsumer; // 소비자 친구

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isFavorite;

    @Builder
    private FriendEntity(Long id, Consumer consumer, Consumer toConsumer, Boolean isFavorite) {
        this.id = id;
        this.consumer = consumer;
        this.toConsumer = toConsumer;
        this.isFavorite = isFavorite;
    }

    public static FriendEntity from(Friend friend) {
        return FriendEntity.builder()
                .id(friend.getId())
                .consumer(friend.getConsumer())
                .toConsumer(friend.getToConsumer())
                .isFavorite(friend.getIsFavorite())
                .build();
    }

    public Friend toDomain() {
        return Friend.builder()
                .id(id)
                .consumer(consumer)
                .toConsumer(toConsumer)
                .isFavorite(isFavorite)
                .build();
    }
}
