package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MonthlyTrendResponse {

    private List<String> months;
    private List<MonthlySnapshotDto> snapshots;
}
