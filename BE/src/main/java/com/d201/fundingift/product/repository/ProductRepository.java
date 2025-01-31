package com.d201.fundingift.product.repository;

import com.d201.fundingift.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select p from Product p " +
            "where p.id = :productId and p.status = 'ACTIVE' and p.deletedAt is null")
    Optional<Product> findById(@Param("productId") Long productId);

    @Query("select p from Product p " +
            "where (:categoryId is null or p.productCategory.id = :categoryId) " +
            "and (:keyword is null or p.name like %:keyword% or p.description like %:keyword% or p.productCategory.name like %:keyword%) " +
            "and p.status = 'ACTIVE' and p.deletedAt is null")
    Slice<Product> findAllSliceByCategoryIdAndKeyword(@Param("categoryId") Integer productCategoryId, @Param("keyword") String keyword, Pageable pageable);

}
