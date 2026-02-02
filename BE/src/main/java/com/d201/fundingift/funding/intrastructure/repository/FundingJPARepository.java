package com.d201.fundingift.funding.intrastructure.repository;

import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.domain.status.FundingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FundingJPARepository extends JpaRepository<FundingEntity, Long> {

    @Query("select f from FundingEntity f " +
            "where f.consumer.id = :consumerId and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndDeletedAtIsNull(@Param("consumerId") Long consumerId, Pageable pageable);

    @Query("select f from FundingEntity f " +
            "where f.consumer.id = :consumerId and f.isPrivate = false and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndIsPrivateAndDeletedAtIsNull(@Param("consumerId") Long consumerId, Pageable pageable);

    @Query("select f from FundingEntity f " +
            "where f.consumer.id = :consumerId and f.product.name like %:keyword% and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndProductNameAndDeletedAtIsNull(@Param("consumerId") Long consumerId, @Param("keyword") String keyword, Pageable pageable);

    @Query("select f from FundingEntity f " +
            "where f.consumer.id = :consumerId and f.isPrivate = false and f.product.name like %:keyword% and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndIsPrivateAndProductNameAndDeletedAtIsNull(@Param("consumerId") Long consumerId, @Param("keyword") String keyword, Pageable pageable);

    List<FundingEntity> findAllByConsumerIdAndFundingStatusOrderByStartDateAsc(Long consumerId, FundingStatus fundingStatus);

    List<FundingEntity> findAllByConsumerIdAndFundingStatusAndIsPrivateOrderByStartDateAsc(Long consumerId, FundingStatus fundingStatus, Boolean isPrivate);

    @Query("select f from FundingEntity f " +
            "where f.consumer.id = :consumerId and f.fundingStatus = 'IN_PROGRESS' and f.isPrivate = :isPrivate and f.deletedAt is null ORDER BY f.startDate ASC")
    List<FundingEntity> findAllByConsumerIdAndFundingStatusAndIsPrivateAndDeletedAtIsNullOrderByStartDateAsc(@Param("consumerId")Long consumerId, @Param("isPrivate") boolean isPrivate);

    Optional<FundingEntity> findByIdAndDeletedAtIsNull(Long fundingId);

    @Query("SELECT f FROM FundingEntity f " +
            "WHERE " +
            "(YEAR(f.anniversaryDate) = :year AND MONTH(f.anniversaryDate) = :month) " +
            "AND f.consumer.id = :consumerId AND f.deletedAt IS NULL")
    List<FundingEntity> findAllByConsumerIdAndDeletedAtIsNull(@Param("consumerId") Long consuerId, @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT f FROM FundingEntity f " +
            "WHERE " +
            "(YEAR(f.anniversaryDate) = :year AND MONTH(f.anniversaryDate) = :month) " +
            "AND f.consumer.id = :consumerId AND f.isPrivate = false AND f.deletedAt IS NULL")
    List<FundingEntity> findAllByConsumerIdAndIsPrivateAndDeletedAtIsNull(@Param("consumerId") Long consuerId, @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT f FROM FundingEntity f WHERE f.consumer.id IN :consumerIds and f.fundingStatus = 'IN_PROGRESS' AND f.deletedAt IS NULL")
    Slice<FundingEntity> findAllByConsumerIdsAndFundingStatusAndDeletedAtIsNull(@Param("consumerIds") List<Long> consumerIds, Pageable pageable);

    @Query("SELECT f FROM FundingEntity f WHERE f.consumer.id = :consumerId AND f.fundingStatus = 'IN_PROGRESS' AND f.deletedAt IS NULL")
    List<FundingEntity> findInProgressFundingsByConsumerId(@Param("consumerId") Long consumerId);

    @Query("select f from FundingEntity f where f.fundingStatus = :fundingStatus and f.startDate = :date and f.deletedAt IS NULL")
    List<FundingEntity> findAllByFundingStatusAndStartDateAndDeletedAtIsNull(@Param("fundingStatus")FundingStatus fundingStatus, @Param("date") LocalDate date);

    @Query("select f from FundingEntity f where f.fundingStatus = :fundingStatus and f.endDate = :date and f.deletedAt IS NULL")
    List<FundingEntity> findAllByFundingStatusAndEndDateAndDateAndDeletedAtIsNull(@Param("fundingStatus")FundingStatus fundingStatus, @Param("date") LocalDate date);

    /**
     * 펀딩 피드 조회 - UNION ALL 방식으로 최적화
     * 1. 공개 펀딩 (isPrivate = false): 내 친구의 공개 펀딩
     * 2. 친한 친구 공개 펀딩 (isPrivate = true): 친구가 나를 친한 친구로 설정한 경우의 비공개 펀딩
     */
    @Query(value = """
        SELECT * FROM (
            SELECT f.* FROM funding f
            INNER JOIN friend fr ON fr.to_consumer_id = f.consumer_id
            WHERE fr.consumer_id = :consumerId
              AND f.is_private = false
              AND f.deleted_at IS NULL

            UNION ALL

            SELECT f.* FROM funding f
            INNER JOIN friend fr ON fr.to_consumer_id = f.consumer_id
            WHERE fr.consumer_id = :consumerId
              AND f.is_private = true
              AND f.deleted_at IS NULL
              AND EXISTS (
                SELECT 1 FROM friend fr2
                WHERE fr2.consumer_id = f.consumer_id
                  AND fr2.to_consumer_id = :consumerId
                  AND fr2.is_favorite = true
              )
        ) AS combined_funding
        ORDER BY combined_funding.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM (
            SELECT f.funding_id FROM funding f
            INNER JOIN friend fr ON fr.to_consumer_id = f.consumer_id
            WHERE fr.consumer_id = :consumerId
              AND f.is_private = false
              AND f.deleted_at IS NULL

            UNION ALL

            SELECT f.funding_id FROM funding f
            INNER JOIN friend fr ON fr.to_consumer_id = f.consumer_id
            WHERE fr.consumer_id = :consumerId
              AND f.is_private = true
              AND f.deleted_at IS NULL
              AND EXISTS (
                SELECT 1 FROM friend fr2
                WHERE fr2.consumer_id = f.consumer_id
                  AND fr2.to_consumer_id = :consumerId
                  AND fr2.is_favorite = true
              )
        ) AS count_query
        """,
        nativeQuery = true)
    Slice<FundingEntity> findAllFriendsFunding(@Param("consumerId") Long consumerId, Pageable pageable);
}
