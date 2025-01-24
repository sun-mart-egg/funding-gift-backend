package com.d201.fundingift.funding.domain;

import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.funding.domain.status.FundingStatus;
import com.d201.fundingift.funding.intrastructure.entity.AnniversaryCategory;
import com.d201.fundingift.product.entity.Product;
import com.d201.fundingift.product.entity.ProductOption;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class Funding {

    private Long id;

    private Integer sumPrice;

    private Integer minPrice;

    private Integer targetPrice;

    private LocalDate anniversaryDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private String title;

    private String content;

    private String accountBank;

    private String accountNo;

    private String name;

    private String phoneNumber;

    private String defaultAddr;

    private String detailAddr;

    private String zipCode;

    private FundingStatus fundingStatus;

    private Boolean isPrivate;

    private Consumer consumer;

    private AnniversaryCategory anniversaryCategory;

    private Product product;

    private ProductOption productOption;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    private Funding(Long id, Integer sumPrice, Integer minPrice, Integer targetPrice, LocalDate anniversaryDate, LocalDate startDate, LocalDate endDate, String title, String content, String accountBank, String accountNo, String name, String phoneNumber, String defaultAddr, String detailAddr, String zipCode, FundingStatus fundingStatus, Boolean isPrivate, Consumer consumer, AnniversaryCategory anniversaryCategory, Product product, ProductOption productOption, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.sumPrice = sumPrice;
        this.minPrice = minPrice;
        this.targetPrice = targetPrice;
        this.anniversaryDate = anniversaryDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.title = title;
        this.content = content;
        this.accountBank = accountBank;
        this.accountNo = accountNo;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.defaultAddr = defaultAddr;
        this.detailAddr = detailAddr;
        this.zipCode = zipCode;
        this.fundingStatus = fundingStatus;
        this.isPrivate = isPrivate;
        this.consumer = consumer;
        this.anniversaryCategory = anniversaryCategory;
        this.product = product;
        this.productOption = productOption;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}
