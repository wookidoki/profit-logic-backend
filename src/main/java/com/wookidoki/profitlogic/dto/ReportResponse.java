package com.wookidoki.profitlogic.dto;

import com.wookidoki.profitlogic.domain.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long id;
    private Long projectId;
    private String yearMonth;
    private String content;
    private Integer tokensUsed;
    private LocalDateTime createdAt;

    public static ReportResponse from(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .projectId(report.getProject().getId())
                .yearMonth(report.getYearMonth())
                .content(report.getContent())
                .tokensUsed(report.getTokensUsed())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
