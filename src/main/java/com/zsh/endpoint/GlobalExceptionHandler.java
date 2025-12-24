package com.zsh.endpoint;

import com.zsh.exception.BusinessException;
import com.zsh.vo.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handlerBusinesException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return Result.error(e.getErrorCodeValue(), e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.warn("Parameter validation failed: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().iterator().next().getMessage();

        log.warn("Constraint violation: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handlerException(Exception e) {
        log.error("System error", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
