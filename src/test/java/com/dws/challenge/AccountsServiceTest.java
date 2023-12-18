package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// we don't need other annotations as SpringBootTest configures everything by itself,
// and having them could mislead developers who doesn't know how it works in-depth
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;
  // this annotation injects Mockito mock instead of a regular bean for a collaborator with the same type
  @MockBean
  private NotificationService notificationService;

  // we need to cleanup in-memory repository after each test
  @AfterEach
  void tearDown() {
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setAtomicBalance(new AtomicReference<>(new BigDecimal(1000)));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void successfulMoneyTransferTest() {
    // given
    String sourceCustomerId = "Id-1";
    Account sourceCustomer = new Account(sourceCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(sourceCustomer);

    String destinationCustomerId = "Id-2";
    Account destinationCustomer = new Account(destinationCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(destinationCustomer);

    // when
    this.accountsService.transfer(sourceCustomer, destinationCustomer, new BigDecimal(500));

    // then
    assertThat(accountsService.getAccount(sourceCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(500));
    assertThat(accountsService.getAccount(destinationCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1500));

    verify(notificationService, times(1)).notifyAboutTransfer(eq(sourceCustomer), anyString());
    verify(notificationService, times(1)).notifyAboutTransfer(eq(destinationCustomer), anyString());
  }

  @Test
  void overdraftMustFailTest() {
    // given
    String sourceCustomerId = "Id-1";
    Account sourceCustomer = new Account(sourceCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(sourceCustomer);

    String destinationCustomerId = "Id-2";
    Account destinationCustomer = new Account(destinationCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(destinationCustomer);

    // when
    boolean transferResult = accountsService.transfer(sourceCustomer, destinationCustomer, new BigDecimal(1500));

    // then
    assertThat(transferResult).isFalse();

    assertThat(accountsService.getAccount(sourceCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1000));
    assertThat(accountsService.getAccount(destinationCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1000));

    verify(notificationService, times(0)).notifyAboutTransfer(eq(sourceCustomer), anyString());
    verify(notificationService, times(0)).notifyAboutTransfer(eq(destinationCustomer), anyString());
  }

  @Test
  void concurrentMoneyTransferTest() {
    // given
    String sourceCustomerId = "Id-1";
    Account sourceCustomer = new Account(sourceCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(sourceCustomer);

    String destinationCustomerId = "Id-2";
    Account destinationCustomer = new Account(destinationCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(destinationCustomer);

    // when
    ExecutorService executor = Executors.newFixedThreadPool(20);

    for (int i = 0; i < 500; i++) {
      executor.submit(() -> this.accountsService.transfer(sourceCustomer, destinationCustomer, BigDecimal.ONE));
    }

    try {
      executor.shutdown();

      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        fail("Execution was not able to fit into the timeout threshold");
      }
    } catch (InterruptedException e) {
      fail("Execution was interrupted");
    }

    // then
    assertThat(accountsService.getAccount(sourceCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(500));
    assertThat(accountsService.getAccount(destinationCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1500));
  }
}
