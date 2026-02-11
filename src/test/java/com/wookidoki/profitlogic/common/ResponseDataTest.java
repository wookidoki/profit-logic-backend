package com.wookidoki.profitlogic.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseDataTest {

    @Nested
    @DisplayName("성공 응답")
    class SuccessResponse {

        @Test
        @DisplayName("데이터와 함께 성공 응답 생성")
        void shouldCreateSuccessWithData() {
            ResponseData<String> response = ResponseData.success("테스트 데이터");

            assertTrue(response.isSuccess());
            assertEquals("테스트 데이터", response.getData());
            assertNull(response.getMessage());
            assertNotNull(response.getTimestamp());
        }

        @Test
        @DisplayName("데이터 없이 성공 응답 생성")
        void shouldCreateSuccessWithoutData() {
            ResponseData<Void> response = ResponseData.success();

            assertTrue(response.isSuccess());
            assertNull(response.getData());
        }

        @Test
        @DisplayName("메시지와 함께 성공 응답 생성")
        void shouldCreateSuccessWithMessage() {
            ResponseData<String> response = ResponseData.success("데이터", "생성 완료");

            assertTrue(response.isSuccess());
            assertEquals("데이터", response.getData());
            assertEquals("생성 완료", response.getMessage());
        }
    }

    @Nested
    @DisplayName("실패 응답")
    class FailResponse {

        @Test
        @DisplayName("에러 메시지로 실패 응답 생성")
        void shouldCreateFailWithMessage() {
            ResponseData<Void> response = ResponseData.fail("입력값이 잘못되었습니다.");

            assertFalse(response.isSuccess());
            assertNull(response.getData());
            assertEquals("입력값이 잘못되었습니다.", response.getMessage());
            assertNotNull(response.getTimestamp());
        }
    }
}
