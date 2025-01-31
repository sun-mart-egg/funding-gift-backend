package com.d201.fundingift.funding.domain.port;

import com.d201.fundingift.funding.domain.Funding;

import java.util.Optional;

public interface FundingRepository {

    void save(Funding funding);

    Optional<Funding> findById(long id);
}
