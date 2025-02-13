package com.d201.fundingift.medium.funding.service;

import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.consumer.repository.ConsumerRepository;
import com.d201.fundingift.funding.dto.request.PostFundingRequest;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.intrastructure.repository.FundingJPARepository;
import com.d201.fundingift.funding.service.FundingPostService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
@SqlGroup({
        @Sql(value = "/sql/post-funding-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
        @Sql(value = "/sql/delete-all-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
})
public class FundingPostServiceTest {

    @Autowired
    private FundingPostService fundingPostService;

    @Autowired
    private FundingJPARepository fundingRepository;
    @Autowired
    private ConsumerRepository consumerRepository;

    @Test
    public void 펀딩을_생성할_수_있다() {
        //given
        LocalDate now = LocalDate.now();
        Consumer consumer = consumerRepository.findById(1L).orElseThrow(() -> new RuntimeException("1L인 유저가 없습니다."));
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
        Optional<FundingEntity> result = fundingRepository.findById(1L);
        assertThat(result.get().getTitle()).isEqualTo("내 생일이야");

    }
}
