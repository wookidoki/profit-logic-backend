package com.wookidoki.profitlogic.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "time_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TimeLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    @Column(name = "hours_spent", nullable = false, precision = 10, scale = 2)
    private BigDecimal hoursSpent;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "memo", length = 500)
    private String memo;
}
