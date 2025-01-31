package com.d201.fundingift.funding.intrastructure.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnniversaryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "anniversary_category_id", nullable = false)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String name;

    public AnniversaryCategory(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
