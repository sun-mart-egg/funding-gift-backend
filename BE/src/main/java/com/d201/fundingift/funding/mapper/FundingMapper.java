package com.d201.fundingift.funding.mapper;

import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.funding.domain.Funding;
import com.d201.fundingift.funding.domain.FundingDateAndStatus;
import com.d201.fundingift.funding.domain.FundingPrice;
import com.d201.fundingift.funding.dto.request.PostFundingRequest;
import com.d201.fundingift.funding.intrastructure.entity.AnniversaryCategory;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.product.entity.Product;
import com.d201.fundingift.product.entity.ProductOption;


public class FundingMapper {

    public static FundingEntity FundingEntityFromFunding(Funding funding) {

        return FundingEntity.builder()
                .sumPrice(funding.getSumPrice())
                .minPrice(funding.getMinPrice())
                .targetPrice(funding.getTargetPrice())
                .anniversaryDate(funding.getAnniversaryDate())
                .startDate(funding.getStartDate())
                .endDate(funding.getEndDate())
                .title(funding.getTitle())
                .content(funding.getContent())
                .accountBank(funding.getAccountBank())
                .accountNo(funding.getAccountNo())
                .name(funding.getName())
                .phoneNumber(funding.getPhoneNumber())
                .defaultAddr(funding.getDefaultAddr())
                .detailAddr(funding.getDetailAddr())
                .zipCode(funding.getZipCode())
                .fundingStatus(funding.getFundingStatus() != null ? funding.getFundingStatus().name() : null)
                .isPrivate(funding.getIsPrivate())

                .consumer(funding.getConsumer())
                .anniversaryCategory(funding.getAnniversaryCategory())
                .product(funding.getProduct())
                .productOption(funding.getProductOption())

                .build();
    }

    public static Funding toFunding(Consumer consumer,
                                    Product product,
                                    ProductOption productOption,
                                    AnniversaryCategory anniversaryCategory,
                                    FundingDateAndStatus fundingDateAndStatus,
                                    FundingPrice fundingPrice,
                                    PostFundingRequest postFundingRequest) {

        return Funding.builder()
                .consumer(consumer)
                .product(product)
                .productOption(productOption)
                .anniversaryCategory(anniversaryCategory)

                .anniversaryDate(fundingDateAndStatus.getAnniversaryDate())
                .startDate(fundingDateAndStatus.getStartDate())
                .endDate(fundingDateAndStatus.getEndDate())
                .fundingStatus(fundingDateAndStatus.getFundingStatus())

                .minPrice(fundingPrice.getMinPrice())
                .targetPrice(fundingPrice.getTargetPrice())

                .title(postFundingRequest.getTitle())
                .content(postFundingRequest.getContent())
                .accountBank(postFundingRequest.getAccountBank())
                .accountNo(postFundingRequest.getAccountNo())
                .name(postFundingRequest.getName())
                .phoneNumber(postFundingRequest.getPhoneNumber())
                .defaultAddr(postFundingRequest.getDefaultAddr())
                .detailAddr(postFundingRequest.getDetailAddr())
                .zipCode(postFundingRequest.getZipCode())
                .isPrivate(postFundingRequest.getIsPrivate())

                .build();
    }
}
