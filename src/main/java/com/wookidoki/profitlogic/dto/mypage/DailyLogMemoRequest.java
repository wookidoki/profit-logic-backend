package com.wookidoki.profitlogic.dto.mypage;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyLogMemoRequest {

    @Size(max = 2000, message = "메모는 2000자 이내로 작성해주세요.")
    private String personalMemo;
}
