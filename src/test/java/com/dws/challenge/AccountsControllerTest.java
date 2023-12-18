package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

// we don't need other annotations as SpringBootTest configures everything by itself,
// and having them could mislead developers who doesn't know how it works in-depth
@SpringBootTest
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.retrieveBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
        .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
        .andExpect(status().isOk())
        .andExpect(
            content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  void successfulTransferTest() throws Exception {
    // given
    String sourceCustomerId = "Id-1";
    Account sourceCustomer = new Account(sourceCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(sourceCustomer);

    String destinationCustomerId = "Id-2";
    Account destinationCustomer = new Account(destinationCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(destinationCustomer);

    String urlWithEncodedParameters = String.format("/v1/accounts/transfer?from=%s&to=%s&amount=%s",
        sourceCustomerId,
        destinationCustomerId,
        50);

    // when
    this.mockMvc.perform(put(urlWithEncodedParameters))
        .andExpect(status().isOk())
        .andExpect(
            content().string("Successfully transferred 50 from Id-1 to Id-2"));

    // then
    assertThat(accountsService.getAccount(sourceCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(950));
    assertThat(accountsService.getAccount(destinationCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1050));
  }

  @Test
  void insufficientMoneyOnAccountTest() throws Exception {
    // given
    String sourceCustomerId = "Id-1";
    Account sourceCustomer = new Account(sourceCustomerId, new BigDecimal(10));
    this.accountsService.createAccount(sourceCustomer);

    String destinationCustomerId = "Id-2";
    Account destinationCustomer = new Account(destinationCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(destinationCustomer);

    String urlWithEncodedParameters = String.format("/v1/accounts/transfer?from=%s&to=%s&amount=%s",
        sourceCustomerId,
        destinationCustomerId,
        50);

    // when
    this.mockMvc.perform(put(urlWithEncodedParameters))
        .andExpect(status().isBadRequest())
        .andExpect(
            content().string("Insufficient money on the Id-1 account"));

    // then
    assertThat(accountsService.getAccount(sourceCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(10));
    assertThat(accountsService.getAccount(destinationCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1000));
  }

  @Test
  void negativeAmountForTransferSentTest() throws Exception {
    // given
    String sourceCustomerId = "Id-1";
    Account sourceCustomer = new Account(sourceCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(sourceCustomer);

    String destinationCustomerId = "Id-2";
    Account destinationCustomer = new Account(destinationCustomerId, new BigDecimal(1000));
    this.accountsService.createAccount(destinationCustomer);

    String urlWithEncodedParameters = String.format("/v1/accounts/transfer?from=%s&to=%s&amount=%s",
        sourceCustomerId,
        destinationCustomerId,
        -50);

    // when
    this.mockMvc.perform(put(urlWithEncodedParameters))
        .andExpect(status().isBadRequest());

    // then
    assertThat(accountsService.getAccount(sourceCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1000));
    assertThat(accountsService.getAccount(destinationCustomerId).retrieveBalance()).isEqualTo(new BigDecimal(1000));
  }
}
