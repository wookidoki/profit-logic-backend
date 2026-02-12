package com.wookidoki.profitlogic.controller;

import com.wookidoki.profitlogic.common.ResponseData;
import com.wookidoki.profitlogic.dto.admin.AdminStatsResponse;
import com.wookidoki.profitlogic.dto.admin.AdminUserResponse;
import com.wookidoki.profitlogic.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ResponseData<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getStats();
        return ResponseEntity.ok(ResponseData.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ResponseData<List<AdminUserResponse>>> getUsers() {
        List<AdminUserResponse> users = adminService.getUsers();
        return ResponseEntity.ok(ResponseData.success(users));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ResponseData<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ResponseData.success(null, "사용자가 삭제되었습니다."));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ResponseData<Void>> deletePost(@PathVariable Long postId) {
        adminService.deletePost(postId);
        return ResponseEntity.ok(ResponseData.success(null, "게시글이 삭제되었습니다."));
    }
}
