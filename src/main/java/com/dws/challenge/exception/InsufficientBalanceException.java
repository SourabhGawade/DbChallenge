package com.dws.challenge.exception;

import lombok.Getter;

@Getter
public class InsufficientBalanceException extends RuntimeException {
	
	public InsufficientBalanceException(String msg) {
		super(msg);
	}
}
