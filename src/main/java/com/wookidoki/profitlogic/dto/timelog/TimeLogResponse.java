package com.wookidoki.profitlogic.dto.timelog;

import com.wookidoki.profitlogic.domain.TimeLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLogResponse {

    private Long id;
    private Long projectId;
    private String taskName;
    private BigDecimal hoursSpent;
    private LocalDate logDate;
    private String memo;
    private LocalDateTime createdAt;

    public static TimeLogResponse from(TimeLog timeLog) {
        return TimeLogResponse.builder()
                .id(timeLog.getId())
                .projectId(timeLog.getProject().getId())
                .taskName(timeLog.getTaskName())
                .hoursSpent(timeLog.getHoursSpent())
                .logDate(timeLog.getLogDate())
                .memo(timeLog.getMemo())
                .createdAt(timeLog.getCreatedAt())
                .build();
    }
}
