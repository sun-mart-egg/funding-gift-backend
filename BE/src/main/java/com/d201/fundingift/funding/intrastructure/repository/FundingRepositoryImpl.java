package com.d201.fundingift.funding.intrastructure.repository;

import com.d201.fundingift.funding.domain.Funding;
import com.d201.fundingift.funding.domain.port.FundingRepository;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.mapper.FundingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FundingRepositoryImpl implements FundingRepository {

    private final FundingJPARepository fundingJPARepository;

    @Override
    public void save(Funding funding) {
        fundingJPARepository.save(FundingMapper.FundingEntityFromFunding(funding));
    }

    @Override
    public Optional<Funding> findById(long l) {
        return Optional.empty();
    }

    @Override
    public Slice<FundingEntity> findAllFriendsFunding(Long consumerId, Pageable pageable) {
        return fundingJPARepository.findAllFriendsFunding(consumerId, pageable);
    }
}
