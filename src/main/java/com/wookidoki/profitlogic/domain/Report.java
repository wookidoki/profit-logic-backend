package com.wookidoki.profitlogic.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reports", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "year_month"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tokens_used", nullable = false)
    @Builder.Default
    private Integer tokensUsed = 0;
}
