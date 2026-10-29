package model;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.annotation.Id;
import model.annotation.Positive;
import java.io.Serializable;

public abstract class Account implements Transactable, InterestBearing, Serializable, Comparable<Account> {

    private static final long serialVersionUID = 1L;

    @Id
    private int accountNumber;
    private String ownerName;
    @Positive
    private double balance;

    public Account(int accountNumber, String ownerName, double balance) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    // Week 9: Synchronized deposit to guarantee thread-safety
    @Override
    public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero. Received: " + amount);
        }
        this.balance += amount;
    }

    // Week 9: Unsynchronized deposit to demonstrate race conditions
    public void depositUnsync(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero. Received: " + amount);
        }
        double temp = this.balance;
        try {
            // Tiny sleep to force context switch / race condition
            Thread.sleep(0, 1000);
        } catch (InterruptedException ignored) {}
        this.balance = temp + amount;
    }

    // Week 9: Synchronized withdraw to protect shared balance
    @Override
    public synchronized boolean withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero. Received: " + amount);
        }
        if (!canWithdraw(amount)) {
            long available = getAvailableBalance();
            long shortfall = amount > available ? (amount - available) : 1;
            throw new InsufficientFundsException(
                "Insufficient funds in Account " + accountNumber + " (" + ownerName + "). Shortfall: " + shortfall,
                shortfall
            );
        }
        this.balance -= amount;
        return true;
    }

    public long getAvailableBalance() {
        return (long) Math.max(0, balance);
    }

    public void transfer(Account to, long amount) throws BankException {
        if (to == null) {
            throw new AccountNotFoundException("Target account cannot be null.");
        }
        boolean withdrawn = false;
        try {
            this.withdraw(amount);
            withdrawn = true;
            to.deposit(amount);
        } catch (BankException e) {
            if (withdrawn) {
                try {
                    this.deposit(amount);
                } catch (InvalidAmountException ignored) {}
            }
            throw e;
        } finally {
            System.out.println("[Audit] Transfer attempt from Account " + this.accountNumber +
                               " to Account " + to.getAccountNumber() + " completed.");
        }
    }

    // Week 10: Deadlock demonstration - transfers locking in opposite order
    public static void transferUnsafe(Account from, Account to, long amount) throws BankException {
        if (from == null || to == null) {
            throw new AccountNotFoundException("Account cannot be null.");
        }
        synchronized (from) {
            System.out.println(Thread.currentThread().getName() + " locked Account " + from.getAccountNumber());
            try {
                Thread.sleep(100); // Artificial delay to ensure lock interleaving creates deadlock
            } catch (InterruptedException ignored) {}
            System.out.println(Thread.currentThread().getName() + " waiting to lock Account " + to.getAccountNumber());
            synchronized (to) {
                from.withdraw(amount);
                to.deposit(amount);
                System.out.println(Thread.currentThread().getName() + " finished transfer of " + amount);
            }
        }
    }

    // Week 10: Deadlock resolution - always lock account with smaller accountNumber first
    public static void transferSafe(Account from, Account to, long amount) throws BankException {
        if (from == null || to == null) {
            throw new AccountNotFoundException("Account cannot be null.");
        }
        if (from.getAccountNumber() == to.getAccountNumber()) {
            throw new BankException("Cannot transfer to the same account.");
        }

        Account first = from.getAccountNumber() < to.getAccountNumber() ? from : to;
        Account second = from.getAccountNumber() < to.getAccountNumber() ? to : from;

        synchronized (first) {
            System.out.println(Thread.currentThread().getName() + " acquired lock on 1st Account " + first.getAccountNumber());
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {}
            synchronized (second) {
                System.out.println(Thread.currentThread().getName() + " acquired lock on 2nd Account " + second.getAccountNumber());
                from.withdraw(amount);
                to.deposit(amount);
                System.out.println(Thread.currentThread().getName() + " completed safe transfer of " + amount);
            }
        }
    }

    public abstract boolean canWithdraw(long amount);

    @Override
    public abstract double interestRate();

    @Override
    public String toString() {
        return "Account Number: " + accountNumber +
                ", Owner Name: " + ownerName +
                ", Balance: " + balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Account))
            return false;
        Account a = (Account) o;
        return accountNumber == a.accountNumber;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(accountNumber);
    }

    @Override
    public int compareTo(Account other) {
        if (other == null) {
            return 1;
        }
        return Integer.compare(this.accountNumber, other.accountNumber);
    }
}
