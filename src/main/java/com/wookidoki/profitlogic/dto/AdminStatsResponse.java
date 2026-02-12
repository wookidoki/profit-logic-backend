package com.wookidoki.profitlogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AdminStatsResponse {

    private long userCount;
    private long projectCount;
    private long postCount;
    private long commentCount;
    private long reportCount;
    private long chatCount;
}
