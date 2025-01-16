package com.d201.fundingift.friend.service;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.util.SecurityUtil;
import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.consumer.repository.ConsumerRepository;
import com.d201.fundingift.friend.domain.Friend;
import com.d201.fundingift.friend.domain.port.FriendExternalPort;
import com.d201.fundingift.friend.domain.port.FriendRepository;
import com.d201.fundingift.friend.dto.FriendDto;
import com.d201.fundingift.friend.dto.GetFriendCommand;
import com.d201.fundingift.friend.dto.response.GetFriendStoryResponse;
import com.d201.fundingift.friend.dto.response.GetFriendsResponse;

import com.d201.fundingift.funding.entity.Funding;
import com.d201.fundingift.funding.repository.FundingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.d201.fundingift._common.response.ErrorType.*;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FriendService {

    private final ConsumerRepository consumerRepository;
    private final FriendRepository friendRepository;
    private final FundingRepository fundingRepository;
    private final FriendExternalPort friendExternalPort;
    private final SecurityUtil securityUtil;

    /**
     * 친구 목록 동기화 -> 친구 목록 조회
     * 카카오톡 친구와 DB 친구를 비교하여 카카오톡에만 친구가 있는 경우 DB에도 저장합니다.
     * 카카오톡 즐겨찾기 친구는 친한 친구로 설정하여 DB에 저장/업데이트 합니다.
     */
    @Transactional
    public GetFriendsResponse synchronizeFriends(Long consumerId) {
        List<GetFriendCommand> friends = friendExternalPort.getFriends(consumerId);
        Consumer consumer = findByConsumerId(consumerId)
                .orElseThrow(() -> new CustomException(CONSUMER_NOT_FOUND));

        for(GetFriendCommand f : friends) {
            // 소비자 친구 정보 받기
            Consumer toConsumer = consumerRepository
                    .findBySocialIdAndDeletedAtIsNull(f.getSocialId())
                            .orElse(null);

            if(toConsumer == null) {
                log.info("소셜 아이디 {}에 해당 친구가 DB에 없습니다.", f.getSocialId());
                continue;
            }

            // 이미 친구인지 확인
            friendRepository
                    .findByConsumerIdAndToConsumerId(consumerId, toConsumer.getId())
                    .ifPresentOrElse(friend -> {
                        friend.toggleFavorite(f.getFavorite());
                        friendRepository.save(friend);
                            }, () -> {
                                friendRepository
                                        .save(Friend.from(consumer, toConsumer, f.getFavorite()));
                            });

        }

        return getFriends(consumerId);
    }

    // 친구 목록 조회
    public GetFriendsResponse getFriends(Long consumerId) {
        // 친한 친구 우선으로 정렬하고, 같은 경우 이름 기준으로 정렬
        List<FriendDto> list = friendRepository.findAllByConsumerId(consumerId).stream()
                .map(f -> FriendDto.from(f, f.getToConsumer()))
                .toList();

        return GetFriendsResponse.from(list, list.size());
    }

    // 친한 친구 설정 변경
    @Transactional
    public void toggleFavorite(Long consumerId, Long toConsumerId) {

        log.info("consumerId {}",consumerId);
        log.info("toConsumerId {}",toConsumerId);
        Friend friend = friendRepository.findByConsumerIdAndToConsumerId(consumerId, toConsumerId).orElseThrow(() -> new CustomException(FRIEND_NOT_FOUND));
        friend.toggleFavorite(!friend.getIsFavorite());

        friendRepository.save(friend);
    }

    public List<GetFriendStoryResponse> getFriendsStory() {
        Long consumerId = securityUtil.getConsumerId();

        List<Friend> friends = friendRepository.findAllByConsumerId(consumerId);
        List<GetFriendStoryResponse> getFriendStoryResponses = new ArrayList<>();


        for(Friend f : friends) {
            log.info(String.valueOf(f.getToConsumer().getId()));
            //친구의 펀딩 목록 중 진행중이고 시작일이 제일 빠른 하나 반환
            List<Funding> privateFundings = getAllByConsumerIdAndFundingStatusAndIsPrivateAndDeletedAtIsNullOrderByStartDateAsc(f, true);
            List<Funding> notPrivateFundings = getAllByConsumerIdAndFundingStatusAndIsPrivateAndDeletedAtIsNullOrderByStartDateAsc(f, false);

            Optional<Consumer> consumer = findByConsumerId(f.getToConsumer().getId());

            //내 친구가 소비자가 아닌 경우
            if(consumer.isEmpty())
                continue;

            //친한 친구 아닌데 친한친구 공개 펀딩만 있는 경우
            if(!privateFundings.isEmpty() && notPrivateFundings.isEmpty() && !checkingIsFavoriteFriend(consumer.get().getId(), consumerId))
                continue;

            if(!notPrivateFundings.isEmpty()) {
                getFriendStoryResponses.add(GetFriendStoryResponse.from(notPrivateFundings.get(0), consumer.get()));
            } else if(!privateFundings.isEmpty()) {
                getFriendStoryResponses.add(GetFriendStoryResponse.from(privateFundings.get(0), consumer.get()));
            }
        }

        Collections.sort(getFriendStoryResponses);

        return getFriendStoryResponses;
    }

    /**
     * private
     */
    private Optional<Consumer> findByConsumerId(Long consumerId){
        return consumerRepository.findByIdAndDeletedAtIsNull(consumerId);
    }

    private List<Funding> getAllByConsumerIdAndFundingStatusAndIsPrivateAndDeletedAtIsNullOrderByStartDateAsc(Friend f, boolean isPrivate) {
        return fundingRepository.findAllByConsumerIdAndFundingStatusAndIsPrivateAndDeletedAtIsNullOrderByStartDateAsc(f.getToConsumer().getId(),isPrivate);
    }

    private boolean checkingIsFavoriteFriend(Long toConsumerId, Long consumerId) {
        Optional<Friend> friend = friendRepository.findByConsumerIdAndToConsumerId(toConsumerId, consumerId);

        //보려는 펀딩 목록의 대상에 본인이 친구가 아니거나 친한 친구가 아닌 경우 -> false
        return friend.isPresent() && friend.get().getIsFavorite();
    }
}
