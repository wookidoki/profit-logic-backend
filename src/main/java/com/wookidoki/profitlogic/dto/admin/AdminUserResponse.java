package com.wookidoki.profitlogic.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class AdminUserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String bizType;
    private long projectCount;
    private LocalDateTime createdAt;
}
