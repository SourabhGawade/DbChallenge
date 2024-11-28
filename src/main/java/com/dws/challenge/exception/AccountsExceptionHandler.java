package com.dws.challenge.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class AccountsExceptionHandler {

	 @ExceptionHandler(InsufficientBalanceException.class)
	    public ResponseEntity<Object> handleInsufficientBalanceException(InsufficientBalanceException ex) {
	        log.error("Insufficient balance: {}", ex.getMessage());
	        return buildErrorResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
	    }


	    @ExceptionHandler(AccountNotFound.class)
	    public ResponseEntity<Object> handleAccountNotFoundException(AccountNotFound ex) {
	        log.error("Account not found: {}", ex.getMessage());
	        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
	    }

	    @ExceptionHandler(Exception.class)
	    public ResponseEntity<Object> handleGenericException(Exception ex) {
	        log.error("Unknown Error Occured: {}", ex.getMessage());
	        return buildErrorResponse("Somethign went wrong please try after sometime! ", HttpStatus.INTERNAL_SERVER_ERROR);
	    }

	    private ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status) {
	        Map<String, Object> errorDetails = new HashMap<>();
	        errorDetails.put("timestamp", LocalDateTime.now());
	        errorDetails.put("message", message);
	        errorDetails.put("status", status.value());
	        errorDetails.put("error", status.getReasonPhrase());

	        return new ResponseEntity<>(errorDetails, status);
	    }
}	
