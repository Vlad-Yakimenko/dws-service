package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
// Constructor injection is preferred cause not use of reflection
@RequiredArgsConstructor
public class AccountsService {

  // Exposing service's collaborators is a bad practice, especially if it is used only in tests.
  // To avoid it, we can DI mechanism or reflection, or if we want to inject mock in a bean, we should use the @MockBean annotation
  @Getter
  private final AccountsRepository accountsRepository;
  private final NotificationService notificationService;

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public boolean transfer(Account from, Account to, BigDecimal amount) {
    while (true) {
      BigDecimal currentBalance = from.getAtomicBalance().get();

      if (currentBalance.compareTo(amount) < 0) {
        return false; // Not enough balance to transfer
      }

      if (from.getAtomicBalance().compareAndSet(currentBalance, currentBalance.subtract(amount))) {
        to.getAtomicBalance().updateAndGet(current -> current.add(amount));

        notificationService.notifyAboutTransfer(from, String.format("%s successfully transferred to %s", amount, to));
        notificationService.notifyAboutTransfer(to, String.format("%s successfully received from %s", amount, from));

        return true;
      }
    }
  }
}
