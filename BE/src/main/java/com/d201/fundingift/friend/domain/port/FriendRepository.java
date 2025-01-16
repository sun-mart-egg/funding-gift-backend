package com.d201.fundingift.friend.domain.port;


import com.d201.fundingift.friend.domain.Friend;

import java.util.List;
import java.util.Optional;

public interface FriendRepository {

    /**
     * 내 친구 목록
     * 친한 친구 우선으로 정렬하고, 같은 경우 이름 기준으로 정렬
     */
    List<Friend> findAllByConsumerId(Long consumerId);

    // 날 친구로 설정한 친구들
    List<Friend> findAllByToConsumerId(Long toConsumerId);

    Optional<Friend> findByConsumerIdAndToConsumerId(Long consumerId, Long toConsumerId);

    // 날 친한 친구로 설정한 친구들
    List<Friend> findAllByToConsumerIdAndIsFavorite(Long toConsumerId, Boolean isFavorite);
    void save(Friend friend);
    void delete(Friend friend);
}
