package com.d201.fundingift.friend.infrastructure.repository;

import com.d201.fundingift.friend.infrastructure.entity.FriendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendJPARepository extends JpaRepository<FriendEntity, Long> {

    List<FriendEntity> findAllByConsumerIdAndDeletedAtIsNullOrderByIsFavoriteDescToConsumerNameAsc(Long consumerId);
    List<FriendEntity> findAllByToConsumerIdAndDeletedAtIsNull(Long toConsumerId);
    Optional<FriendEntity> findByConsumerIdAndToConsumerIdAndDeletedAtIsNull(Long consumerId, Long toConsumerId);
    List<FriendEntity> findAllByToConsumerIdAndIsFavoriteAndDeletedAtIsNull(Long toConsumerId, Boolean isFavorite);

}