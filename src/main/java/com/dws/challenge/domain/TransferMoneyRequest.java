package com.dws.challenge.domain;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//Request class for money transfer
@Data
public class TransferMoneyRequest {

	@NotNull
	@NotEmpty
	private final String senderAccountId;

	@NotNull
	@NotEmpty
	private final String receiverAccountId;

	@NotNull
	@Min(value = 1, message = "Amount Must be higher than Zero.")
	private BigDecimal transferAmount;

	@JsonCreator
	  public TransferMoneyRequest(@JsonProperty("senderAccountId") String senderAccountId,
			  @JsonProperty("receiverAccountId") String receiverAccountId,
	    @JsonProperty("transferAmount") BigDecimal transferAmount) {
	    this.senderAccountId = senderAccountId;
	    this.receiverAccountId = receiverAccountId;
	    this.transferAmount=transferAmount;
	  }
}
