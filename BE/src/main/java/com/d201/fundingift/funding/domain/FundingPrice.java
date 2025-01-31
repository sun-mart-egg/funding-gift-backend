package com.d201.fundingift.funding.domain;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.response.ErrorType;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FundingPrice {

    private Integer minPrice;

    private Integer targetPrice;

    @Builder
    private FundingPrice(Integer minPrice, Integer targetPrice) {
        this.minPrice = minPrice;
        this.targetPrice = targetPrice;
    }

    public static FundingPrice of(Integer minPrice, Integer targetPrice) {

        checkingTargetPrice(targetPrice);
        checkingMinPrice(targetPrice, minPrice);

        return FundingPrice.builder()
                .minPrice(minPrice)
                .targetPrice(targetPrice)
                .build();
    }

    // 0 < 목표 금액
    private static void checkingTargetPrice(Integer targetPrice) {
        if(targetPrice <= 0)
            throw new CustomException(ErrorType.FUNDING_TARGETPRICE_IS_UNDER_ZERO);
    }

    // 0 < 최소 금액 <= 목표 금액
    private static void checkingMinPrice(Integer targetPrice, Integer minPrice) {
        if(minPrice <= 0)
            throw new CustomException(ErrorType.FUNDING_MINPRICE_IS_UNDER_ZERO);

        if(targetPrice < minPrice)
            throw new CustomException(ErrorType.FUNDING_MINPRICE_IS_OVER_TARGETPRICE);
    }
}
