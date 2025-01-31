package com.d201.fundingift.product.service;

import com.d201.fundingift._common.exception.CustomException;
import com.d201.fundingift._common.response.SliceList;
import com.d201.fundingift._common.util.SecurityUtil;
import com.d201.fundingift.funding.repository.FundingRepository;
import com.d201.fundingift.product.dto.response.GetProductCategoryResponse;
import com.d201.fundingift.product.dto.response.GetProductDetailResponse;
import com.d201.fundingift.product.dto.response.GetProductOptionResponse;
import com.d201.fundingift.product.dto.response.GetProductResponse;
import com.d201.fundingift.product.entity.Product;
import com.d201.fundingift.product.repository.ProductCategoryRepository;
import com.d201.fundingift.product.repository.ProductOptionRepository;
import com.d201.fundingift.product.repository.ProductRepository;
import com.d201.fundingift.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.d201.fundingift._common.response.ErrorType.*;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ProductService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final WishlistRepository wishlistRepository;
    private final FundingRepository fundingRepository;
    private final SecurityUtil securityUtil;

    /** 상품 카테고리 목록 조회 **/
    public List<GetProductCategoryResponse> getCategories() {
        return productCategoryRepository.findAllByDeletedAtIsNull()
                .stream().map(GetProductCategoryResponse::from)
                .collect(Collectors.toList());
    }

    /** 상품 목록 조회 **/
    public SliceList<GetProductResponse> getProducts(Integer categoryId, String keyword, Integer page, Integer size, Integer sort) {
        if (categoryId != null) {
            validateCategoryId(categoryId);
        }

        Slice<Product> products = productRepository.findAllSliceByCategoryIdAndKeyword(categoryId, keyword, PageRequest.of(page, size, getSort(sort)));
        return getProductResponseSliceList(products);
    }

    private Sort getSort(Integer sort) {
        Sort defaultSort = Sort.by("id").descending();
        if (sort == 0) { // 기본 (최신 순)
            return defaultSort;
        }
        if (sort == 1) { // 리뷰 많은 순
            return Sort.by("reviewCnt").descending().and(defaultSort);
        }
        if (sort == 2) { // 평점 높은 순
            return Sort.by("reviewAvg").descending().and(defaultSort);
        }
        if (sort == 3) { // 가격 높은 순
            return Sort.by("price").descending().and(defaultSort);
        }
        if (sort == 4) { // 가격 낮은 순
            return Sort.by("price").ascending().and(defaultSort);
        }
        throw new CustomException(SORT_NOT_FOUND);
    }

    private SliceList<GetProductResponse> getProductResponseSliceList(Slice<Product> products) {
        return SliceList.from(products.stream().map(GetProductResponse::from).collect(Collectors.toList()),
                products.getPageable(),
                products.hasNext());
    }

    public SliceList<GetProductResponse> getProductsRank(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return getProductResponseSliceList(fundingRepository.findProductSliceOrderByFundingCount(pageable));
    }

    // 상품 상세 조회
    public GetProductDetailResponse getProductDetail(Long productId) {
        // 상품
        Product product = findByProductId(productId);
        // 해당 상품의 옵션
        List<GetProductOptionResponse> options = getOptions(product);
        // 위시리스트 여부
        boolean isWishlist = getIsWishlist(productId);
        // 반환
        return GetProductDetailResponse.from(product, options, isWishlist);
    }

    private List<GetProductOptionResponse> getOptions(Product product) {
        return productOptionRepository.findAllByProduct(product)
                .stream().map(GetProductOptionResponse::from)
                .collect(Collectors.toList());
    }

    private Product findByProductId(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(PRODUCT_NOT_FOUND));
    }

    private boolean getIsWishlist(Long productId) {
        Long consumerId = securityUtil.getConsumerIdOrNull();

        if (consumerId == null) {
            return false;
        }

        return wishlistRepository.findByConsumerIdAndProductId(consumerId, productId).isPresent();
    }

    private void validateCategoryId(Integer categoryId) {
        if (!productCategoryRepository.existsByIdAndDeletedAtIsNull(categoryId)) {
            throw new CustomException(PRODUCT_CATEGORY_NOT_FOUND);
        }
    }

}
