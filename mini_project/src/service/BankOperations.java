package service;

import exception.BankException;
import model.Account;

import java.util.ArrayList;

/**
 * Interface contract defining standard banking operations for MiniBank.
 * Practical 14 Requirement 1: Service operations layer.
 */
public interface BankOperations {

    /**
     * Opens and persists a new account after validating business and annotation rules.
     */
    Account openAccount(Account account) throws BankException;

    /**
     * Deposits money into the specified account.
     */
    Account deposit(String accountNumber, long amount) throws BankException;

    /**
     * Withdraws money from the specified account according to account constraints.
     */
    Account withdraw(String accountNumber, long amount) throws BankException;

    /**
     * Transfers money between two accounts safely without deadlock.
     */
    void transfer(String fromAccountNumber, String toAccountNumber, long amount) throws BankException;

    /**
     * Generates a formatted statement for the specified account.
     */
    String statement(String accountNumber) throws BankException;

    /**
     * Retrieves all accounts registered in the bank.
     */
    ArrayList<Account> listAccounts();
}

