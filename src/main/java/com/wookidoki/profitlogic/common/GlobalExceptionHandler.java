package com.wookidoki.profitlogic.common;

import com.wookidoki.profitlogic.common.exception.BusinessLogicException;
import com.wookidoki.profitlogic.common.exception.DuplicateEmailException;
import com.wookidoki.profitlogic.common.exception.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ResponseData<Void>> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseData.fail(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ResponseData<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseData.fail(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseData<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(ResponseData.fail(message));
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ResponseData<Void>> handleBusinessLogic(BusinessLogicException ex) {
        return ResponseEntity.badRequest()
                .body(ResponseData.fail(ex.getMessage()));
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ResponseData<Void>> handleArithmetic(ArithmeticException ex) {
        return ResponseEntity.badRequest()
                .body(ResponseData.fail(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseData<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ResponseData.fail(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseData.fail("서버 내부 오류가 발생했습니다."));
    }
}
