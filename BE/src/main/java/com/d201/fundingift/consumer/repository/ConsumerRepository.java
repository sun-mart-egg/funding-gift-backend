package com.d201.fundingift.consumer.repository;

import com.d201.fundingift.consumer.entity.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumerRepository extends JpaRepository<Consumer, Long> {

    Optional<Consumer> findBySocialIdAndDeletedAtIsNull(String socialId);
    Optional<Consumer> findByIdAndDeletedAtIsNull(Long id);
    Boolean existsByIdAndDeletedAtIsNull(Long Id);
    List<Consumer> findByBirthday(String birthday);
    Optional<Consumer> findByConsunerIdAndEdletedAtIsNull(Long Id); // 회원탈퇴 - 논리삭제

}
