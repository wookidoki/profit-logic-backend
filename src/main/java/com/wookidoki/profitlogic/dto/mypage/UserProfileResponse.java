package com.wookidoki.profitlogic.dto.mypage;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private String bizType;
    private String role;
    private LocalDateTime createdAt;
    private int projectCount;
    private long totalChatCount;
    private long totalPostCount;
}
