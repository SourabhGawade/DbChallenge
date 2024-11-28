package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferMoneyRequest;
import com.dws.challenge.exception.AccountNotFound;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;
  
  @Mock
  private AccountsRepository accountsRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private AccountsService accountsServiceMock;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
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
  void testSuccessfulTransfer() throws InsufficientBalanceException, AccountNotFound {

      Account sender = new Account("123", BigDecimal.valueOf(1000));
      Account receiver = new Account("456", BigDecimal.valueOf(500));
      BigDecimal transferAmount = BigDecimal.valueOf(200);

      Mockito.when(accountsRepository.getAccount("123")).thenReturn(sender);
      Mockito.when(accountsRepository.getAccount("456")).thenReturn(receiver);

      String result = accountsServiceMock.transferMoney(new TransferMoneyRequest("123", "456", transferAmount));

      assertEquals("Money is Transfer Successfully!", result);
      assertEquals(BigDecimal.valueOf(800), sender.getBalance());
      assertEquals(BigDecimal.valueOf(700), receiver.getBalance());
      Mockito.verify(accountsRepository, Mockito.times(1)).transfer(sender);
      Mockito.verify(accountsRepository, Mockito.times(1)).transfer(receiver);
      Mockito.verify(notificationService, Mockito.times(2)).notifyAboutTransfer(Mockito.any(), Mockito.anyString());
  }

  @Test
  void testInsufficientBalance() {

      Account sender = new Account("123", BigDecimal.valueOf(100));
      Account receiver = new Account("456", BigDecimal.valueOf(500));
      BigDecimal transferAmount = BigDecimal.valueOf(200);

      Mockito.when(accountsRepository.getAccount("123")).thenReturn(sender);
      Mockito.when(accountsRepository.getAccount("456")).thenReturn(receiver);

      InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () ->
      accountsServiceMock.transferMoney(new TransferMoneyRequest("123", "456", transferAmount)));
      assertEquals("Insufficient balance to perform the transaction", exception.getMessage());
      Mockito.verify(accountsRepository, Mockito.never()).transfer(Mockito.any());
      Mockito.verify(notificationService, Mockito.never()).notifyAboutTransfer(Mockito.any(), Mockito.anyString());
  }

  @Test
  void testAccountNotFound() {

      Mockito.when(accountsRepository.getAccount("123")).thenReturn(null);

      AccountNotFound exception = assertThrows(AccountNotFound.class, () ->
      accountsServiceMock.transferMoney(new TransferMoneyRequest("123", "456", BigDecimal.valueOf(200))));
      assertEquals("Sender account not found", exception.getMessage());
      Mockito.verify(accountsRepository, Mockito.never()).transfer(Mockito.any());
      Mockito.verify(notificationService, Mockito.never()).notifyAboutTransfer(Mockito.any(), Mockito.anyString());
  }

  @Test
  void testFailedTransfer() throws InsufficientBalanceException, AccountNotFound {
	  
      Account sender = new Account("123", BigDecimal.valueOf(1000));
      Account receiver = new Account("456", BigDecimal.valueOf(500));
      BigDecimal transferAmount = BigDecimal.valueOf(200);

      Mockito.when(accountsRepository.getAccount("123")).thenReturn(sender);
      Mockito.when(accountsRepository.getAccount("456")).thenReturn(receiver);
      Mockito.doThrow(new RuntimeException("DB Error")).when(accountsRepository).transfer(sender);
     
      String result = accountsServiceMock.transferMoney(new TransferMoneyRequest("123", "456", transferAmount));
      
      assertEquals("Money is Transfer Failed!", result);
  }
}
