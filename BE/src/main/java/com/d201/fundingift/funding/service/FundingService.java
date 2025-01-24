package com.d201.fundingift.funding.service;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.response.ErrorType;
import com.d201.fundingift._common.response.SliceList;
import com.d201.fundingift._common.util.FcmNotificationProvider;
import com.d201.fundingift._common.util.SecurityUtil;
import com.d201.fundingift.attendance.repository.AttendanceRepository;
import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.consumer.repository.ConsumerRepository;
import com.d201.fundingift.friend.domain.Friend;
import com.d201.fundingift.friend.domain.port.FriendRepository;
import com.d201.fundingift.funding.domain.status.FundingStatus;
import com.d201.fundingift.funding.dto.request.DeleteFundingRequest;
import com.d201.fundingift.funding.dto.response.GetFundingCalendarResponse;
import com.d201.fundingift.funding.dto.response.GetFundingDetailResponse;
import com.d201.fundingift.funding.dto.response.GetFundingResponse;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.intrastructure.repository.FundingJPARepository;
import com.d201.fundingift._common.dto.FcmNotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FundingService {

    private final FundingJPARepository fundingJPARepository;
    private final AttendanceRepository attendanceRepository;
    private final ConsumerRepository consumerRepository;
    private final FriendRepository friendRepository;
    private final SecurityUtil securityUtil;
    private final FcmNotificationProvider fcmNotificationProvider;

//    @Transactional
//    public void postFunding(PostFundingRequest postFundingRequest) {
//        Consumer consumer = getConsumer();
//
//        //상품 없으면 예외
//        Product product = getProduct(postFundingRequest);
//
//        //상품 옵션 없으면 예외
//        ProductOption productOption = getProductOption(postFundingRequest);
//
//        //제품과 제품 옵션이 맞는지 확인
//        checkingProductAndProductOptionIsSame(product, productOption);
//
//        //기념일 카테고리 없으면 예외
//        AnniversaryCategory anniversaryCategory = getAnniversaryCategory(postFundingRequest);
//
//        //시작일이 현재 날짜보다 과거면 예외
//        isStartDatePast(postFundingRequest.getStartDate());
//
//        // 기념일이 시작일보다 과거면 예외
//        isAnniversaryDatePast(postFundingRequest.getAnniversaryDate(), postFundingRequest.getStartDate());
//
//        // 종료일이 기념일 보다 과거이면 예외
//        isEndDatePast(postFundingRequest.getEndDate(), postFundingRequest.getAnniversaryDate());
//
//        //시작일 종료일 7일 넘으면 예외
//        isOver7Days(postFundingRequest.getStartDate(), postFundingRequest.getEndDate());
//
//        //시작일이 오늘이면 IN_PROGRESS로 상태 변경, 미래면 PRE_PROGRESS
//        fundingJPARepository.save(Funding.from(postFundingRequest, IsStartDateToday(postFundingRequest.getStartDate()), consumer, anniversaryCategory, product, productOption));
//
//        // 알림
//        fcmNotificationProvider.sendToMany(
//                getConsumersByToConsumerIdAndFavorite(consumer.getId()),
//                FcmNotificationDto.of("펀딩 등록 알림", consumer.getName() + "님이 펀딩을 등록했어요!")
//        );
//    }

    @Transactional
    public void deleteFunding(DeleteFundingRequest deleteFundingRequest) {
        Long myConsumerId = securityUtil.getConsumerId();

        //펀딩 존재 확인
        FundingEntity funding = getFunding(deleteFundingRequest.getFundingId());

        //내 펀딩이 맞는지 확인
        if(!Objects.equals(myConsumerId, funding.getConsumer().getId()))
            throw new CustomException(ErrorType.USER_UNAUTHORIZED);

        //삭제 가능한 상태인지 확인 - 펀딩 시작전일 경우만 삭제 가능(PRE_PROGRESS인 경우)
        if(!"PRE_PROGRESS".equals(String.valueOf(funding.getFundingStatus())))
            throw new CustomException(ErrorType.FUNDING_STATUS_NOT_DELETED);

        fundingJPARepository.delete(funding);
    }

    //내 펀딩 목록 보기
    public SliceList<GetFundingResponse> getMyFundings(String keyword, Pageable pageable) {
        Long myConsumerId = securityUtil.getConsumerId();

        //제품명으로 검색 안하는 경우
        if (keyword == null)
            return getFundingsSliceList(findAllByConsumerId(myConsumerId, pageable));

        //제품명으로 검색하는 경우
        return getFundingsSliceList(findAllByConsumerIdAndProductName(myConsumerId, keyword, pageable));
    }

    //내가 참여한 펀딩 목록 조회
    public SliceList<GetFundingResponse> getMyAttendanceFundings(Pageable pageable) {
        Long myConsumerId = securityUtil.getConsumerId();

        return getFundingsSliceList(findAllByConsumerRightJoinAttendance(myConsumerId, pageable));
    }

    //친구 펀딩 목록 보기
    public SliceList<GetFundingResponse> getFriendFundings(Long friendConsumerId, String keyword, Pageable pageable) {
        Long myConsumerId = securityUtil.getConsumerId();

        //친구 아이디 존재 여부 확인
        findByConsumerId(friendConsumerId);

        //보려는 펀딩 목록의 대상이 자신의 친구인지 확인
        checkingFriend(myConsumerId, friendConsumerId);

        //보려는 펀딩 목록의 대상에 자신이 친한 친구인지 확인
        if(checkingIsFavoriteFriend(friendConsumerId, myConsumerId)) {
            //제품명으로 검색 안하는 경우
            if(keyword == null)
                return getFundingsSliceList(findAllByConsumerId(friendConsumerId, pageable));

            return getFundingsSliceList(findAllByConsumerIdAndProductName(friendConsumerId, keyword, pageable));
        } else {
            //제품명으로 검색 안하는 경우
            if(keyword == null)
                return getFundingsSliceList(findAllByConsumerIdAndIsPrivate(friendConsumerId, pageable));

            return getFundingsSliceList(findAllByConsumerIdAndIsPrivateAndProductName(friendConsumerId, keyword, pageable));
        }
    }

    public SliceList<GetFundingResponse> getFundingFeeds(Pageable pageable) {
        Long myConsumerId = securityUtil.getConsumerId();

        //친구 리스트 조회
        List<Friend> friends = friendRepository.findAllByConsumerId(myConsumerId);

        return getFundingsFeedSliceList(findAllByConsumerIdsAndFundingStatus(friends, pageable), friends);
    }

    public List<GetFundingResponse> getFundingsStory(Long consumerId) {
        Long myConsumerId = securityUtil.getConsumerId();

        if(!Objects.equals(myConsumerId, consumerId)) {
            //친구 아이디 존재 여부 확인
            findByConsumerId(consumerId);

            //보려는 펀딩 목록의 대상이 자신의 친구인지 확인
            checkingFriend(myConsumerId, consumerId);
        }

        //보려는 펀딩 목록의 대상에 자신이 친한 친구인지 확인
        if(Objects.equals(myConsumerId, consumerId) || checkingIsFavoriteFriend(consumerId, myConsumerId)) {
            return getFundingsList(findByConsumerIdAndFundingStatusOrderedByStartDate(consumerId));
        } else {
            return getFundingsList(findByConsumerIdAndFundingStatusAndIsPrivateOrderByStartDateAsc(consumerId));
        }
    }

    //펀딩 상세 조회
    public GetFundingDetailResponse getFundingDetailResponse(Long fundingId) {
        Long myConsumerId = securityUtil.getConsumerId();

        FundingEntity funding = getFunding(fundingId);
        Long fundingConsumerId = funding.getConsumer().getId();

        //내 펀딩인지 확인
        if(!Objects.equals(myConsumerId, fundingConsumerId)) {

            //보려는 펀딩 목록의 대상이 자신의 친구인지 확인
            checkingFriend(myConsumerId, fundingConsumerId);

            //글 허용범위가 펀딩 생성자의 친한 친구 인지 확인
            if(funding.getIsPrivate()) {
                checkingIsFavoriteFriendOrElseThrow(fundingConsumerId, myConsumerId);
            }
        }

        return GetFundingDetailResponse.from(funding);
    }

    public List<GetFundingCalendarResponse> getFundingCalendarsResponse(Integer year, Integer month) {
        List<GetFundingCalendarResponse> fundingList = new ArrayList<>();
        Long myConsumerId = securityUtil.getConsumerId();

        //친구 리스트 조회
        List<Friend> friends = friendRepository.findAllByConsumerId(myConsumerId);

        for(Friend f : friends) {

            //친구가 날 친한 친구로 설정 했는지 확인
            if(checkingIsFavoriteFriend(f.getToConsumer().getId(), myConsumerId)) {
                //친한 친구로 설정한 경우 isPrivate 상관 없이 모두 조회
                fundingList.addAll(
                        fundingJPARepository
                                .findAllByConsumerIdAndDeletedAtIsNull(f.getToConsumer().getId(), year, month)
                                .stream()
                                .map(GetFundingCalendarResponse::from)
                                .toList()
                );
                continue;
            }

            //친한 친구가 아닌경우 IsPrivate == false만 조회
            fundingList.addAll(
                    fundingJPARepository
                            .findAllByConsumerIdAndIsPrivateAndDeletedAtIsNull(f.getToConsumer().getId(), year, month)
                            .stream()
                            .map(GetFundingCalendarResponse::from)
                            .toList()
            );
        }

        return fundingList;
    }

    /**
     * 내부 메서드
     */
    private FundingEntity getFunding(Long fundingId) {
        return fundingJPARepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new CustomException(ErrorType.FUNDING_NOT_FOUND));
    }

    private List<GetFundingResponse> getFundingsList(List<FundingEntity> fundings) {
        return fundings.stream().map(GetFundingResponse::from).collect(Collectors.toList());
    }

    private List<FundingEntity> findByConsumerIdAndFundingStatusOrderedByStartDate(Long consumerId) {
        return fundingJPARepository.findAllByConsumerIdAndFundingStatusOrderByStartDateAsc(consumerId, FundingStatus.IN_PROGRESS);
    }

    private List<FundingEntity> findByConsumerIdAndFundingStatusAndIsPrivateOrderByStartDateAsc(Long consumerId) {
        return fundingJPARepository.findAllByConsumerIdAndFundingStatusAndIsPrivateOrderByStartDateAsc(consumerId, FundingStatus.IN_PROGRESS, false);
    }

    //slice<Funding> -> SliceList<GetFundingResponse> 변경 매서드
    private SliceList<GetFundingResponse> getFundingsSliceList(Slice<FundingEntity> fundings) {
        return SliceList.from(fundings.stream().map(GetFundingResponse::from).collect(Collectors.toList()), fundings.getPageable(), fundings.hasNext());
    }

    //slice<Funding> -> SliceList<GetFundingResponse> 변경 매서드
    private SliceList<GetFundingResponse> getFundingsFeedSliceList(Slice<FundingEntity> fundings, List<Friend> friends) {
        Map<Long, Friend> toFriends = new HashMap<>();

        for(Friend f : friends) {
            Optional<Friend> toFriend = friendRepository.findByConsumerIdAndToConsumerId(f.getToConsumer().getId(), f.getConsumer().getId());
            toFriend.ifPresent(friend -> toFriends.put(f.getToConsumer().getId(), friend));
        }

        List<FundingEntity> changed = new ArrayList<>();
        for(FundingEntity f : fundings) {
            if(!f.getIsPrivate()) {
                changed.add(f);
                continue;
            }

            if(toFriends.containsKey(f.getConsumer().getId()) && toFriends.get(f.getConsumer().getId()).getIsFavorite())
                changed.add(f);
        }

        return SliceList.from(changed.stream().map(GetFundingResponse::from).collect(Collectors.toList()), fundings.getPageable(), fundings.hasNext());
    }

    //consumerId로 펀딩 목록 찾기
    private Slice<FundingEntity> findAllByConsumerId(Long consumerId, Pageable pageable) {
        return fundingJPARepository.findAllByConsumerIdAndDeletedAtIsNull(consumerId, pageable);
    }

    private Slice<FundingEntity> findAllByConsumerRightJoinAttendance(Long consumerId, Pageable pageable) {
        return attendanceRepository.findAllByConsumerIdAndAndDeletedAtIsNull(consumerId, pageable);
    }

    //consumerId, isPrivate == false로 펀딩 목록 찾기
    private Slice<FundingEntity> findAllByConsumerIdAndIsPrivate(Long consumerId, Pageable pageable) {
        return fundingJPARepository.findAllByConsumerIdAndIsPrivateAndDeletedAtIsNull(consumerId, pageable);
    }

    //consumerId, 검색어로 펀딩 목록 찾기
    private Slice<FundingEntity> findAllByConsumerIdAndProductName(Long consumerId, String keyword, Pageable pageable) {
        return fundingJPARepository.findAllByConsumerIdAndProductNameAndDeletedAtIsNull(consumerId, keyword, pageable);
    }

    //consumerId, isPrivate == false, 검색어로 펀딩 목록 찾기
    private Slice<FundingEntity> findAllByConsumerIdAndIsPrivateAndProductName(Long consumerId, String keyword, Pageable pageable) {
        return fundingJPARepository.findAllByConsumerIdAndIsPrivateAndProductNameAndDeletedAtIsNull(consumerId, keyword, pageable);
    }

    private Slice<FundingEntity> findAllByConsumerIdsAndFundingStatus(List<Friend> friends, Pageable pageable) {
        List<Long> friendIds = friends.stream()
                .map(Friend::getToConsumer)
                .map(Consumer::getId)
                .toList();

        return fundingJPARepository.findAllByConsumerIdsAndFundingStatusAndDeletedAtIsNull(friendIds, pageable);
    }

    private void findByConsumerId(Long consumerId){
        consumerRepository.findByIdAndDeletedAtIsNull(consumerId)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));
    }

    private void checkingFriend(Long consumerId, Long toConsumerId) {
        friendRepository.findByConsumerIdAndToConsumerId(consumerId, toConsumerId)
                .orElseThrow(() -> new CustomException(ErrorType.FRIEND_NOT_FOUND));
    }

    private boolean checkingIsFavoriteFriend(Long toConsumerId, Long consumerId) {
        Optional<Friend> friend = friendRepository.findByConsumerIdAndToConsumerId(toConsumerId, consumerId);

        //보려는 펀딩 목록의 대상에 본인이 친구가 아니거나 친한 친구가 아닌 경우 -> false
        return friend.isPresent() && friend.get().getIsFavorite();
    }

    private void checkingIsFavoriteFriendOrElseThrow(Long toConsumerId, Long consumerId) {
        friendRepository.findByConsumerIdAndToConsumerId(toConsumerId, consumerId)
                .orElseThrow(() -> new CustomException(ErrorType.FRIEND_NOT_IS_FAVORITE));
    }
}
