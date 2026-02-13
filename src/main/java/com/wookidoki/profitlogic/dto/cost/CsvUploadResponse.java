package com.wookidoki.profitlogic.dto.cost;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvUploadResponse {

    private int totalRows;
    private int importedCount;
    private int skippedCount;
    private List<CostDetailResponse> importedItems;
    private List<String> errors;
}
