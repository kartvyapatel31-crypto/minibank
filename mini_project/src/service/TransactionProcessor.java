package service;

import exception.BankException;
import model.Account;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TransactionProcessor {
    private final ExecutorService executor;
    private final ConcurrentHashMap<String, Account> sharedAccounts;

    public TransactionProcessor() {
        this(new ConcurrentHashMap<>());
    }

    public TransactionProcessor(ConcurrentHashMap<String, Account> sharedAccounts) {
        this.executor = Executors.newFixedThreadPool(4);
        this.sharedAccounts = sharedAccounts != null ? sharedAccounts : new ConcurrentHashMap<>();
    }

    public void submit(Runnable task) {
        executor.execute(task);
    }

    public void addAccount(Account account) {
        if (account != null) {
            sharedAccounts.put(String.valueOf(account.getAccountNumber()), account);
        }
    }

    public Account getAccount(String accountNumber) {
        return sharedAccounts.get(accountNumber);
    }

    public ConcurrentHashMap<String, Account> getSharedAccounts() {
        return sharedAccounts;
    }

    public void processDeposit(String accountNumber, long amount) {
        submit(() -> {
            Account account = sharedAccounts.get(accountNumber);
            if (account != null) {
                try {
                    account.deposit(amount);
                    System.out.println("Deposited " + amount + " to account " + accountNumber + ". Balance: " + account.getBalance());
                } catch (BankException e) {
                    System.out.println("Deposit error on account " + accountNumber + ": " + e.getMessage());
                }
            }
        });
    }

    public void processWithdraw(String accountNumber, long amount) {
        submit(() -> {
            Account account = sharedAccounts.get(accountNumber);
            if (account != null) {
                try {
                    account.withdraw(amount);
                    System.out.println("Withdrew " + amount + " from account " + accountNumber + ". Balance: " + account.getBalance());
                } catch (BankException e) {
                    System.out.println("Withdrawal error on account " + accountNumber + ": " + e.getMessage());
                }
            }
        });
    }

    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
