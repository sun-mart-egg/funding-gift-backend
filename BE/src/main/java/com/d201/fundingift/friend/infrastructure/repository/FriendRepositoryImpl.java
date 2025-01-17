package com.d201.fundingift.friend.infrastructure.repository;

import com.d201.fundingift.friend.domain.Friend;
import com.d201.fundingift.friend.domain.port.FriendRepository;
import com.d201.fundingift.friend.infrastructure.entity.FriendEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FriendRepositoryImpl implements FriendRepository {

    private final FriendJPARepository friendJPARepository;

    @Override
    public Optional<Friend> findByConsumerIdAndToConsumerId(Long consumerId, Long toConsumerId) {
        return friendJPARepository
                .findByConsumerIdAndToConsumerIdAndDeletedAtIsNull(consumerId, toConsumerId)
                .map(FriendEntity::toDomain);
    }

    @Override
    public List<Friend> findAllByToConsumerId(Long toConsumerId) {
        return friendJPARepository
                .findAllByToConsumerIdAndDeletedAtIsNull(toConsumerId)
                .stream()
                .map(FriendEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Friend> findAllByConsumerId(Long consumerId) {
        return friendJPARepository
                .findAllByConsumerIdAndDeletedAtIsNullOrderByIsFavoriteDescToConsumerNameAsc(consumerId)
                .stream()
                .map(FriendEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Friend> findAllByToConsumerIdAndIsFavorite(Long toConsumerId, Boolean isFavorite) {
        return friendJPARepository
                .findAllByToConsumerIdAndIsFavoriteAndDeletedAtIsNull(toConsumerId, isFavorite)
                .stream()
                .map(FriendEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Friend friend) {
        friendJPARepository.save(FriendEntity.from(friend));
    }

    @Override
    public void delete(Friend friend) {
        friendJPARepository.delete(FriendEntity.from(friend));
    }
}
