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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface FundingMapper {

    FundingMapper INSTANCE = Mappers.getMapper(FundingMapper.class);

    FundingEntity fundingToFundingEntity(Funding funding);

    @Mapping(target = "minPrice", source = "fundingPrice.minPrice")
    @Mapping(target = "targetPrice", source = "fundingPrice.targetPrice")
    @Mapping(target = "anniversaryDate", source = "fundingDateAndStatus.anniversaryDate")
    @Mapping(target = "startDate", source = "fundingDateAndStatus.startDate")
    @Mapping(target = "endDate", source = "fundingDateAndStatus.endDate")
    @Mapping(target = "title", source = "postFundingRequest.title")
    @Mapping(target = "content", source = "postFundingRequest.content")
    @Mapping(target = "accountBank", source = "postFundingRequest.accountBank")
    @Mapping(target = "accountNo", source = "postFundingRequest.accountNo")
    @Mapping(target = "name", source = "postFundingRequest.name")
    @Mapping(target = "phoneNumber", source = "postFundingRequest.phoneNumber")
    @Mapping(target = "defaultAddr", source = "postFundingRequest.defaultAddr")
    @Mapping(target = "detailAddr", source = "postFundingRequest.detailAddr")
    @Mapping(target = "zipCode", source = "postFundingRequest.zipCode")
    @Mapping(target = "fundingStatus", source = "fundingDateAndStatus.fundingStatus")
    @Mapping(target = "isPrivate", source = "postFundingRequest.isPrivate")
    @Mapping(target = "consumer", source = "consumer")
    @Mapping(target = "anniversaryCategory", source = "anniversaryCategory")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "productOption", source = "productOption")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Funding toFunding(
            Consumer consumer,
            Product product,
            ProductOption productOption,
            AnniversaryCategory anniversaryCategory,
            FundingDateAndStatus fundingDateAndStatus,
            FundingPrice fundingPrice,
            PostFundingRequest postFundingRequest
    );
}
