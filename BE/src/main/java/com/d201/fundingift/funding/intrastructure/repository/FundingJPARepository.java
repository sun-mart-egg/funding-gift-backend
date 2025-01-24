package com.d201.fundingift.funding.intrastructure.repository;

import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.domain.status.FundingStatus;
import com.d201.fundingift.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FundingJPARepository extends JpaRepository<FundingEntity, Long> {

    @Query("select f from funding f " +
            "where f.consumer.id = :consumerId and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndDeletedAtIsNull(@Param("consumerId") Long consumerId, Pageable pageable);

    @Query("select f from funding f " +
            "where f.consumer.id = :consumerId and f.isPrivate = false and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndIsPrivateAndDeletedAtIsNull(@Param("consumerId") Long consumerId, Pageable pageable);

    @Query("select f from funding f " +
            "where f.consumer.id = :consumerId and f.product.name like %:keyword% and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndProductNameAndDeletedAtIsNull(@Param("consumerId") Long consumerId, @Param("keyword") String keyword, Pageable pageable);

    @Query("select f from funding f " +
            "where f.consumer.id = :consumerId and f.isPrivate = false and f.product.name like %:keyword% and f.deletedAt is null")
    Slice<FundingEntity> findAllByConsumerIdAndIsPrivateAndProductNameAndDeletedAtIsNull(@Param("consumerId") Long consumerId, @Param("keyword") String keyword, Pageable pageable);

    List<FundingEntity> findAllByConsumerIdAndFundingStatusOrderByStartDateAsc(Long consumerId, FundingStatus fundingStatus);

    List<FundingEntity> findAllByConsumerIdAndFundingStatusAndIsPrivateOrderByStartDateAsc(Long consumerId, FundingStatus fundingStatus, Boolean isPrivate);

    @Query("select f from funding f " +
            "where f.consumer.id = :consumerId and f.fundingStatus = 'IN_PROGRESS' and f.isPrivate = :isPrivate and f.deletedAt is null ORDER BY f.startDate ASC")
    List<FundingEntity> findAllByConsumerIdAndFundingStatusAndIsPrivateAndDeletedAtIsNullOrderByStartDateAsc(@Param("consumerId")Long consumerId, @Param("isPrivate") boolean isPrivate);

    Optional<FundingEntity> findByIdAndDeletedAtIsNull(Long fundingId);

    @Query("SELECT f FROM funding f " +
            "WHERE " +
            "(YEAR(f.anniversaryDate) = :year AND MONTH(f.anniversaryDate) = :month) " +
            "AND f.consumer.id = :consumerId AND f.deletedAt IS NULL")
    List<FundingEntity> findAllByConsumerIdAndDeletedAtIsNull(@Param("consumerId") Long consuerId, @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT f FROM funding f " +
            "WHERE " +
            "(YEAR(f.anniversaryDate) = :year AND MONTH(f.anniversaryDate) = :month) " +
            "AND f.consumer.id = :consumerId AND f.isPrivate = false AND f.deletedAt IS NULL")
    List<FundingEntity> findAllByConsumerIdAndIsPrivateAndDeletedAtIsNull(@Param("consumerId") Long consuerId, @Param("year") Integer year, @Param("month") Integer month);

    @Query("select p FROM funding f right join f.product p " +
            "where p.status = 'ACTIVE' and p.deletedAt is null " +
            "group by p order by count(f) desc")
    Slice<Product> findProductSliceOrderByFundingCount(Pageable pageable);


    @Query("SELECT f FROM funding f WHERE f.consumer.id IN :consumerIds and f.fundingStatus = 'IN_PROGRESS' AND f.deletedAt IS NULL")
    Slice<FundingEntity> findAllByConsumerIdsAndFundingStatusAndDeletedAtIsNull(@Param("consumerIds") List<Long> consumerIds, Pageable pageable);

    @Query("SELECT f FROM funding f WHERE f.consumer.id = :consumerId AND f.fundingStatus = 'IN_PROGRESS' AND f.deletedAt IS NULL")
    List<FundingEntity> findInProgressFundingsByConsumerId(@Param("consumerId") Long consumerId);

    @Query("select f from funding f where f.fundingStatus = :fundingStatus and f.startDate = :date and f.deletedAt IS NULL")
    List<FundingEntity> findAllByFundingStatusAndStartDateAndDeletedAtIsNull(@Param("fundingStatus")FundingStatus fundingStatus, @Param("date") LocalDate date);

    @Query("select f from funding f where f.fundingStatus = :fundingStatus and f.endDate = :date and f.deletedAt IS NULL")
    List<FundingEntity> findAllByFundingStatusAndEndDateAndDateAndDeletedAtIsNull(@Param("fundingStatus")FundingStatus fundingStatus, @Param("date") LocalDate date);

}
