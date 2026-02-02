package com.d201.fundingift.funding.domain.port;

import com.d201.fundingift.funding.domain.Funding;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface FundingRepository {

    void save(Funding funding);

    Optional<Funding> findById(long id);

    Slice<FundingEntity> findAllFriendsFunding(Long consumerId, Pageable pageable);
}
