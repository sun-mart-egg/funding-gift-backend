package com.d201.fundingift.funding.intrastructure.repository;

import com.d201.fundingift.funding.domain.Funding;
import com.d201.fundingift.funding.domain.port.FundingRepository;
import com.d201.fundingift.funding.mapper.FundingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FundingRepositoryImpl implements FundingRepository {

    private final FundingJPARepository fundingJPARepository;

    @Override
    public void save(Funding funding) {
        fundingJPARepository.save(FundingMapper.FundingEntityFromFunding(funding));
    }
}
