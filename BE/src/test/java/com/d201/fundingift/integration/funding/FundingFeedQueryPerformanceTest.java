package com.d201.fundingift.integration.funding;

import com.d201.fundingift.consumer.entity.Consumer;
import com.d201.fundingift.consumer.repository.ConsumerRepository;
import com.d201.fundingift.friend.domain.Friend;
import com.d201.fundingift.friend.infrastructure.entity.FriendEntity;
import com.d201.fundingift.friend.infrastructure.repository.FriendJPARepository;
import com.d201.fundingift.funding.intrastructure.entity.AnniversaryCategory;
import com.d201.fundingift.funding.intrastructure.entity.FundingEntity;
import com.d201.fundingift.funding.intrastructure.repository.AnniversaryCategoryRepository;
import com.d201.fundingift.funding.intrastructure.repository.FundingJPARepository;
import com.d201.fundingift.product.entity.Product;
import com.d201.fundingift.product.entity.ProductOption;
import com.d201.fundingift.product.entity.status.ProductOptionStatus;
import com.d201.fundingift.product.entity.status.ProductStatus;
import com.d201.fundingift.product.repository.ProductOptionRepository;
import com.d201.fundingift.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FundingFeedQueryPerformanceTest {

    @Autowired
    private FundingJPARepository fundingJPARepository;

    @Autowired
    private ConsumerRepository consumerRepository;

    @Autowired
    private FriendJPARepository friendJPARepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private AnniversaryCategoryRepository anniversaryCategoryRepository;

    private Consumer myConsumer;
    private List<Consumer> friends;
    private Product product;
    private ProductOption productOption;
    private AnniversaryCategory anniversaryCategory;

    @BeforeEach
    void setUp() {
        // 내 계정 생성
        myConsumer = Consumer.builder()
                .socialId("my-social-id")
                .email("me@test.com")
                .name("나")
                .profileImageUrl("me.jpg")
                .phoneNumber("01011111111")
                .birthyear("1990")
                .birthday("0101")
                .gender("male")
                .build();
        consumerRepository.save(myConsumer);

        // 상품 생성
        product = Product.builder()
                .name("테스트 상품")
                .price(50000)
                .description("테스트용 상품입니다.")
                .image("product.jpg")
                .reviewAvg(0.0)
                .reviewCnt(0)
                .status(ProductStatus.ACTIVE)
                .build();
        productRepository.save(product);

        // 상품 옵션 생성
        productOption = ProductOption.builder()
                .name("기본 옵션")
                .price(0)
                .status(ProductOptionStatus.ACTIVE)
                .product(product)
                .build();
        productOptionRepository.save(productOption);

        // 기념일 카테고리 생성 (ID는 자동 생성되도록)
        anniversaryCategory = new AnniversaryCategory(null, "생일");
        anniversaryCategoryRepository.save(anniversaryCategory);

        // 친구들 생성 및 친구 관계 설정
        friends = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Consumer friend = Consumer.builder()
                    .socialId("friend-social-id-" + i)
                    .email("friend" + i + "@test.com")
                    .name("친구" + i)
                    .profileImageUrl("friend" + i + ".jpg")
                    .phoneNumber("0102222222" + i)
                    .birthyear("199" + i)
                    .birthday("010" + i)
                    .gender(i % 2 == 0 ? "male" : "female")
                    .build();
            consumerRepository.save(friend);
            friends.add(friend);

            // 나 -> 친구 관계 설정
            Friend myToFriendDomain = Friend.builder()
                    .consumer(myConsumer)
                    .toConsumer(friend)
                    .isFavorite(false)
                    .build();
            friendJPARepository.save(FriendEntity.from(myToFriendDomain));

            // 친구 -> 나 관계 설정 (일부는 친한 친구로 설정)
            Friend friendToMeDomain = Friend.builder()
                    .consumer(friend)
                    .toConsumer(myConsumer)
                    .isFavorite(i < 5) // 5명은 나를 친한 친구로 설정
                    .build();
            friendJPARepository.save(FriendEntity.from(friendToMeDomain));
        }

        // 펀딩 데이터 생성
        LocalDate now = LocalDate.now();
        for (int i = 0; i < friends.size(); i++) {
            Consumer friend = friends.get(i);

            // 공개 펀딩 생성
            FundingEntity publicFunding = FundingEntity.builder()
                    .consumer(friend)
                    .product(product)
                    .productOption(productOption)
                    .anniversaryCategory(anniversaryCategory)
                    .anniversaryDate(now.plusDays(10))
                    .startDate(now)
                    .endDate(now.plusDays(7))
                    .title("공개 펀딩 " + i)
                    .content("공개 펀딩 내용입니다.")
                    .targetPrice(50000)
                    .minPrice(1000)
                    .accountBank("농협")
                    .accountNo("123456789")
                    .name(friend.getName())
                    .phoneNumber(friend.getPhoneNumber())
                    .defaultAddr("서울시")
                    .detailAddr("상세주소")
                    .zipCode("12345")
                    .fundingStatus("IN_PROGRESS")
                    .isPrivate(false)
                    .build();
            fundingJPARepository.save(publicFunding);

            // 비공개 펀딩 생성
            FundingEntity privateFunding = FundingEntity.builder()
                    .consumer(friend)
                    .product(product)
                    .productOption(productOption)
                    .anniversaryCategory(anniversaryCategory)
                    .anniversaryDate(now.plusDays(15))
                    .startDate(now)
                    .endDate(now.plusDays(10))
                    .title("비공개 펀딩 " + i)
                    .content("비공개 펀딩 내용입니다.")
                    .targetPrice(100000)
                    .minPrice(5000)
                    .accountBank("신한")
                    .accountNo("987654321")
                    .name(friend.getName())
                    .phoneNumber(friend.getPhoneNumber())
                    .defaultAddr("서울시")
                    .detailAddr("상세주소")
                    .zipCode("12345")
                    .fundingStatus("IN_PROGRESS")
                    .isPrivate(true)
                    .build();
            fundingJPARepository.save(privateFunding);
        }
    }

    @Test
    @DisplayName("UNION ALL 최적화 쿼리 - 공개 펀딩과 친한 친구 비공개 펀딩 조회")
    void findAllFriendsFunding_ShouldReturnCorrectResults() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);

        // when
        long startTime = System.currentTimeMillis();
        Slice<FundingEntity> result = fundingJPARepository.findAllFriendsFunding(myConsumer.getId(), pageable);
        long endTime = System.currentTimeMillis();

        // then
        System.out.println("=== 펀딩 피드 조회 성능 테스트 결과 ===");
        System.out.println("총 조회된 펀딩 수: " + result.getContent().size());
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");

        // 결과 검증
        // 공개 펀딩: 10개 (모든 친구의 공개 펀딩)
        // 비공개 펀딩: 5개 (나를 친한 친구로 설정한 5명의 비공개 펀딩)
        // 예상 총합: 15개
        assertThat(result.getContent()).hasSize(15);

        // 공개 펀딩 확인
        long publicFundingCount = result.getContent().stream()
                .filter(f -> !f.getIsPrivate())
                .count();
        assertThat(publicFundingCount).isEqualTo(10);

        // 비공개 펀딩 확인 (친한 친구만)
        long privateFundingCount = result.getContent().stream()
                .filter(FundingEntity::getIsPrivate)
                .count();
        assertThat(privateFundingCount).isEqualTo(5);

        System.out.println("공개 펀딩 수: " + publicFundingCount);
        System.out.println("비공개 펀딩 수 (친한 친구): " + privateFundingCount);
        System.out.println("테스트 통과!");
    }

    @Test
    @DisplayName("성능 비교를 위한 반복 실행 테스트")
    void findAllFriendsFunding_PerformanceTest() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        int iterations = 10;
        List<Long> executionTimes = new ArrayList<>();

        // when
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            fundingJPARepository.findAllFriendsFunding(myConsumer.getId(), pageable);
            long endTime = System.nanoTime();
            executionTimes.add((endTime - startTime) / 1_000_000); // ms로 변환
        }

        // then
        double avgTime = executionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long minTime = executionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxTime = executionTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("=== 성능 테스트 결과 (" + iterations + "회 반복) ===");
        System.out.println("평균 실행 시간: " + String.format("%.2f", avgTime) + "ms");
        System.out.println("최소 실행 시간: " + minTime + "ms");
        System.out.println("최대 실행 시간: " + maxTime + "ms");
        System.out.println("실행 시간 목록: " + executionTimes);
    }
}
