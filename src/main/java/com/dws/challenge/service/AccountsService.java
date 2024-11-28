package com.dws.challenge.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferMoneyRequest;
import com.dws.challenge.exception.AccountNotFound;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Getter
	private final NotificationService notificationService;
	
	// Map to acquire lock on accoount for thread safe 
	// Incase of actual DB we can use optimastic locking with versioning
	private final Map<String, Lock> accountLocks = new ConcurrentHashMap<>();

	@Autowired
	public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	public String transferMoney(TransferMoneyRequest transferMoneyRequest)
			throws InsufficientBalanceException, AccountNotFound {

		// Get account details
		// In case of actual DB call we can pull both records in one call to avoid DB round trip.
		Account senderAccountDetails = Optional.ofNullable(getAccount(transferMoneyRequest.getSenderAccountId()))
				.orElseThrow(() -> new AccountNotFound("Sender account not found"));

		Account receiverAccountDetails = Optional.ofNullable(getAccount(transferMoneyRequest.getReceiverAccountId()))
				.orElseThrow(() -> new AccountNotFound("Receiver account not found"));

		// Pre - Validate balance and perform transfer (To-Avoid unnecessary code execution)
		if (senderAccountDetails.getBalance().compareTo(transferMoneyRequest.getTransferAmount()) <= 0) {
			throw new InsufficientBalanceException("Insufficient balance to perform the transaction");
		}
		
		log.info("Sufficient Balance to transfer Money: {}", senderAccountDetails.getBalance());
		Boolean isTransferSuccess= doTransfer(transferMoneyRequest.getSenderAccountId(), transferMoneyRequest.getReceiverAccountId(), transferMoneyRequest.getTransferAmount());
		if(isTransferSuccess) {
			// Sending Notifications
			sendNotifications(senderAccountDetails, receiverAccountDetails, transferMoneyRequest.getTransferAmount());
			return "Money is Transfer Successfully!";
		}else {
			return "Money is Transfer Failed!";
		}	
	}

	@Async
	private void sendNotifications(Account senderAccountDetails, Account recieverAccountDetails,
			BigDecimal transferMoneyRequest) {
		// Async call to send notifications as no impact on actual transaction even if notification failed
		String amount = transferMoneyRequest + "$";
		notificationService.notifyAboutTransfer(senderAccountDetails, amount + " Amount has been Deposited from your account");
		notificationService.notifyAboutTransfer(recieverAccountDetails, amount + " Amount has been Credited to your account");

	}
	
	// Get or create a lock for the account
    private Lock getLock(String accountId) {
        return accountLocks.computeIfAbsent(accountId, id -> new ReentrantLock());
    }
	
	private Boolean doTransfer(String senderAccountId, String receiverAccountId, BigDecimal transferMoneyAmount) {
		// Acquiring locks for both accounts to avoid dead lock
		String firstLockId = senderAccountId.compareTo(receiverAccountId) < 0 ? senderAccountId : receiverAccountId;
		String secondLockId = senderAccountId.compareTo(receiverAccountId) < 0 ? receiverAccountId : senderAccountId;

		Lock firstLock = getLock(firstLockId);
		Lock secondLock = getLock(secondLockId);

		firstLock.lock();
		secondLock.lock();

		try {
			// To Re-Validate balance for thread-safe ( We can keep only one check here)
			Account senderAccountDetails = this.getAccount(senderAccountId);
			Account recieverAccountDetails = this.getAccount(receiverAccountId);
			if (senderAccountDetails.getBalance().compareTo(transferMoneyAmount) <= 0) {
				throw new InsufficientBalanceException("Insufficient balance to perform the transaction");
			}
			senderAccountDetails.setBalance(senderAccountDetails.getBalance().subtract(transferMoneyAmount));
			recieverAccountDetails.setBalance(recieverAccountDetails.getBalance().add(transferMoneyAmount));
			this.accountsRepository.transfer(senderAccountDetails);
			this.accountsRepository.transfer(recieverAccountDetails);

			return true;

		}catch (InsufficientBalanceException e) {
			log.info("InsufficientBalanceException ", e.getMessage());
			throw e;
		}catch (Exception e) {
			log.info("Exception while doing transaction: {}", e.getMessage());
			return false;
		} finally {
			firstLock.unlock();
			secondLock.unlock();
		}
	}
}