package com.d201.fundingift.funding.domain;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.response.ErrorType;
import com.d201.fundingift.funding.domain.status.FundingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class FundingDateAndStatus {

    private LocalDate anniversaryDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private FundingStatus fundingStatus;

    @Builder
    public FundingDateAndStatus(LocalDate anniversaryDate, LocalDate startDate, LocalDate endDate, FundingStatus fundingStatus) {
        this.anniversaryDate = anniversaryDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fundingStatus = fundingStatus;
    }

    public static FundingDateAndStatus of(LocalDate anniversaryDate, LocalDate startDate, LocalDate endDate) {

        isStartDatePast(startDate);
        isAnniversaryDatePast(anniversaryDate, startDate);
        isEndDatePast(endDate, anniversaryDate);
        isOver7Days(startDate, endDate);

        return FundingDateAndStatus.builder()
                .anniversaryDate(anniversaryDate)
                .startDate(startDate)
                .endDate(endDate)
                .fundingStatus(IsStartDateToday(startDate))
                .build();
    }

    //시작일이 현재 날짜보다 과거면 예외
    private static void isStartDatePast(LocalDate startDate) {
        if(startDate.isBefore(LocalDate.now()))
            throw new CustomException(ErrorType.FUNDING_START_DATE_IS_PAST);
    }

    // 기념일이 시작일보다 과거면 예외
    private static void isAnniversaryDatePast(LocalDate anniversaryDate, LocalDate startDate) {
        if(anniversaryDate.isBefore(startDate))
            throw new CustomException(ErrorType.FUNDING_ANNIVERSARY_DATE_IS_PAST);
    }

    // 종료일이 기념일 보다 과거이면 예외
    private static void isEndDatePast(LocalDate endDate, LocalDate anniversaryDate) {
        if(endDate.isBefore(anniversaryDate))
            throw new CustomException(ErrorType.FUNDING_END_DATE_IS_PAST);
    }

    //시작일 종료일 7일 넘으면 예외
    private static void isOver7Days(LocalDate start, LocalDate end) {
        if (Math.abs(start.until(end).getDays()) > 7)
            throw new CustomException(ErrorType.FUNDING_DURATION_NOT_VALID);
    }

    //시작일이 오늘이면 IN_PROGRESS로 상태 변경, 미래면 PRE_PROGRESS
    private static FundingStatus IsStartDateToday(LocalDate startDate) {
        if(startDate.equals(LocalDate.now()))
            return FundingStatus.IN_PROGRESS;
        return FundingStatus.PRE_PROGRESS;
    }
}
