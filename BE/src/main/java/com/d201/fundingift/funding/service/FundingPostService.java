package com.d201.fundingift.funding.service;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.response.ErrorType;
import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.funding.domain.FundingDateAndStatus;
import com.d201.fundingift.funding.domain.FundingPrice;
import com.d201.fundingift.funding.domain.port.FundingRepository;
import com.d201.fundingift.funding.dto.request.PostFundingRequest;
import com.d201.fundingift.funding.intrastructure.entity.AnniversaryCategory;
import com.d201.fundingift.funding.intrastructure.repository.AnniversaryCategoryRepository;
import com.d201.fundingift.funding.mapper.FundingMapper;
import com.d201.fundingift.product.entity.Product;
import com.d201.fundingift.product.entity.ProductOption;
import com.d201.fundingift.product.repository.ProductOptionRepository;
import com.d201.fundingift.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FundingPostService {

    private final FundingRepository fundingRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final AnniversaryCategoryRepository anniversaryCategoryRepository;

    @Transactional
    public void postFunding(Consumer consumer, PostFundingRequest postFundingRequest) {

        //상품 없으면 예외
        Product product = getProduct(postFundingRequest.getProductId());

        //상품 옵션 없으면 예외
        ProductOption productOption = getProductOption(postFundingRequest.getProductOptionId());

        //상품과 상품 옵션이 맞는지 확인
        checkingProductAndProductOptionIsSame(product, productOption);

        //기념일 카테고리 없으면 예외
        AnniversaryCategory anniversaryCategory = getAnniversaryCategory(postFundingRequest.getAnniversaryCategoryId());

        // 시작일, 기념일, 종료일 검증 및 상태 결정
        FundingDateAndStatus fundingDateAndStatus = FundingDateAndStatus.of(postFundingRequest.getAnniversaryDate(), postFundingRequest.getStartDate(), postFundingRequest.getEndDate());

        // 목표 금액, 최소 금액 검증
        FundingPrice fundingPrice = FundingPrice.of(postFundingRequest.getTargetPrice(), postFundingRequest.getMinPrice());

        fundingRepository.save(FundingMapper.toFunding(consumer, product, productOption, anniversaryCategory, fundingDateAndStatus, fundingPrice, postFundingRequest));
    }

    private AnniversaryCategory getAnniversaryCategory(Integer anniversaryCategoryId) {
        return anniversaryCategoryRepository.findById(anniversaryCategoryId)
                .orElseThrow(() -> new CustomException(ErrorType.ANNIVERSARY_CATEGORY_NOT_FOUND));
    }

    private ProductOption getProductOption(Long productOptionId) {
        return productOptionRepository.findByIdAndStatusIsActive(productOptionId)
                .orElseThrow(() -> new CustomException(ErrorType.PRODUCT_OPTION_NOT_FOUND));
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));
    }

    //제품과 제품 옵션이 맞는지 확인
    private void checkingProductAndProductOptionIsSame(Product product, ProductOption productOption) {

        if(product.getId().equals(productOption.getProduct().getId()))
            return;

        throw new CustomException(ErrorType.PRODUCT_OPTION_MISMATCH);
    }
}
