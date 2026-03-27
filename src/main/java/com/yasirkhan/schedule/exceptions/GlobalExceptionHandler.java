package com.yasirkhan.schedule.exceptions;

import com.yasirkhan.schedule.responses.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataBaseException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(DataBaseException ex, HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse
                        .builder()
                        .message(ex.getMessage())
                        .status(ex.getStatus().value())
                        .error(ex.getStatus().getReasonPhrase())
                        .path(request.getRequestURI())
                        .timeStamp(LocalDateTime.now())
                        .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e,  HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse
                        .builder()
                        .message(e.getMessage())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                        .path(request.getRequestURI())
                        .timeStamp(LocalDateTime.now())
                        .build();
        return  new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}
