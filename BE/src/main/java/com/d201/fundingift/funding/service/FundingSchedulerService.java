package com.d201.fundingift.funding.service;

import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.domain.status.FundingStatus;
import com.d201.fundingift.funding.intrastructure.repository.FundingJPARepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FundingSchedulerService {

    private final FundingJPARepository fundingJpaRepository;

    @Transactional
//    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul") //실제 서비스용
    @Scheduled(cron = "0 8 2 * * ?", zone = "Asia/Seoul") //테스트용
    public void updateFundingStatusInProgress() {
        log.info("start updateFundingStatusInProgress");
        List<FundingEntity> fundingEntities = fundingJpaRepository.findAllByFundingStatusAndStartDateAndDeletedAtIsNull(FundingStatus.PRE_PROGRESS, LocalDate.now());

        for(FundingEntity f : fundingEntities)
            f.changeStatus(String.valueOf(FundingStatus.IN_PROGRESS));
    }

    @Transactional
//    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul") //실제 서비스용
    @Scheduled(cron = "0 48 2 * * ?", zone = "Asia/Seoul") //테스트용
    public void updateFundingStatusSuccessOrFail() {
        log.info("start updateFundingStatusSuccessOrFail");
        List<FundingEntity> fundingEntities = fundingJpaRepository.findAllByFundingStatusAndEndDateAndDateAndDeletedAtIsNull(FundingStatus.IN_PROGRESS, LocalDate.now().minusDays(1));

        for(FundingEntity f : fundingEntities)
            if(f.getSumPrice() < f.getTargetPrice())
                f.changeStatus(String.valueOf(FundingStatus.FAIL));
            else
                f.changeStatus(String.valueOf(FundingStatus.SUCCESS));
    }
}
