package com.dws.challenge.domain;

import com.dws.challenge.validator.BalanceConstraint;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
// javax packages were replaced with jakarta in Java 17
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.executable.ValidateOnExecution;
import lombok.Data;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Data
public class Account {

  @NotNull
  @NotEmpty
  private final String accountId;

  @NotNull
  @BalanceConstraint(message = "Initial balance must be positive.")
  @JsonProperty("balance")
  private AtomicReference<BigDecimal> atomicBalance;

  public Account(String accountId) {
    this.accountId = accountId;
    this.atomicBalance = new AtomicReference<>(BigDecimal.ZERO);
  }

  @JsonCreator
  @ValidateOnExecution
  public Account(@JsonProperty("accountId") String accountId,
                 @JsonProperty("balance") BigDecimal balance) {
    this.accountId = accountId;
    this.atomicBalance = new AtomicReference<>(balance);
  }

  public BigDecimal retrieveBalance() {
    return atomicBalance.get();
  }
}
