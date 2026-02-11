package com.wookidoki.profitlogic.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "variable_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal variableCost;

    @Column(name = "fixed_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal fixedCost;

    @Column(name = "work_hours", nullable = false)
    private Integer workHours;

    @Column(name = "hourly_wage", nullable = false, precision = 19, scale = 2)
    private BigDecimal hourlyWage;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    public void update(String title, BigDecimal price, BigDecimal variableCost,
                       BigDecimal fixedCost, Integer workHours, BigDecimal hourlyWage,
                       Boolean isPublic) {
        this.title = title;
        this.price = price;
        this.variableCost = variableCost;
        this.fixedCost = fixedCost;
        this.workHours = workHours;
        this.hourlyWage = hourlyWage;
        this.isPublic = isPublic;
    }
}
