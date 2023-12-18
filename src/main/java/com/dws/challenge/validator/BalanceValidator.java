package com.dws.challenge.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class BalanceValidator implements ConstraintValidator<BalanceConstraint, AtomicReference<BigDecimal>> {

  @Override
  public boolean isValid(AtomicReference<BigDecimal> value, ConstraintValidatorContext context) {
    BigDecimal balance = value.get();

    return !Objects.isNull(balance) && isBalanceNonNegative(balance);
  }

  private static boolean isBalanceNonNegative(BigDecimal bigDecimal) {
    return bigDecimal.compareTo(BigDecimal.ZERO) >= 0;
  }
}
