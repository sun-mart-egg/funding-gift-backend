package com.d201.fundingift.unit.funding.service;

import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.funding.domain.Funding;
import com.d201.fundingift.funding.domain.port.FundingRepository;
import com.d201.fundingift.funding.dto.request.PostFundingRequest;
import com.d201.fundingift.funding.intrastructure.entity.AnniversaryCategory;
import com.d201.fundingift.funding.intrastructure.repository.AnniversaryCategoryRepository;
import com.d201.fundingift.funding.service.FundingPostService;
import com.d201.fundingift.unit.mock.FakeAnniversaryCategoryRepository;
import com.d201.fundingift.unit.mock.FakeFundingRepository;
import com.d201.fundingift.unit.mock.FakeProductOptionRepository;
import com.d201.fundingift.unit.mock.FakeProductRepository;
import com.d201.fundingift.product.entity.Product;
import com.d201.fundingift.product.entity.ProductOption;
import com.d201.fundingift.product.entity.status.ProductOptionStatus;
import com.d201.fundingift.product.entity.status.ProductStatus;
import com.d201.fundingift.product.repository.ProductOptionRepository;
import com.d201.fundingift.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class FundingPostServiceTest {
    private FundingRepository fundingRepository;
    private FundingPostService fundingPostService;

    @BeforeEach
    void setUp() {

        fundingRepository = new FakeFundingRepository();
        ProductRepository productRepository = new FakeProductRepository();
        ProductOptionRepository productOptionRepository = new FakeProductOptionRepository();
        AnniversaryCategoryRepository anniversaryCategoryRepository = new FakeAnniversaryCategoryRepository();


        fundingPostService = new FundingPostService(fundingRepository, productRepository, productOptionRepository, anniversaryCategoryRepository);

        Product product = Product.builder()
                .id(1L)
                .name("상품1")
                .price(50000)
                .description("테스트용 상품입니다.")
                .image("product1.jpg")
                .reviewAvg(0.0)
                .reviewCnt(0)
                .status(ProductStatus.ACTIVE)
                .build();
        productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .id(1L)
                .name("상품1 옵션")
                .price(0)
                .status(ProductOptionStatus.ACTIVE)
                .product(product)
                .build();
        productOptionRepository.save(productOption);

        AnniversaryCategory anniversaryCategory = new AnniversaryCategory(1, "생일");
        anniversaryCategoryRepository.save(anniversaryCategory);
    }

    @Test
    public void 펀딩을_생성할_수_있다() {
        //given
        Consumer consumer = Consumer.builder()
                .id(1L)
                .socialId("1")
                .email("test@test.com")
                .name("아무개")
                .profileImageUrl("test.jpg")
                .phoneNumber("01033333333")
                .birthyear("1997")
                .birthday("0509")
                .gender("male")
                .build();

        LocalDate now = LocalDate.now();
        PostFundingRequest postFundingRequest = PostFundingRequest.builder()
                .productId(1L)
                .productOptionId(1L)
                .anniversaryCategoryId(1)
                .anniversaryDate(now.plusDays(3))
                .startDate(now)
                .endDate(now.plusDays(6))
                .targetPrice(50000)
                .minPrice(1000)
                .title("내 생일이야")
                .content("다들 축하해줘")
                .accountBank("농협")
                .accountNo("3523333333383")
                .name("아무개")
                .phoneNumber("01033333333")
                .defaultAddr("도로명주소")
                .detailAddr("상세주소")
                .zipCode("33333")
                .isPrivate(false)
                .build();

        //when
        fundingPostService.postFunding(consumer, postFundingRequest);

        //then
        Optional<Funding> result = fundingRepository.findById(1L);
        assertThat(result.get().getTitle()).isEqualTo("내 생일이야");
    }
}
