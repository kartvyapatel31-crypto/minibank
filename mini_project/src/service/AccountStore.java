package service;

import model.Account;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class AccountStore {

    private final HashMap<String, Account> accounts = new HashMap<>();
    private final ArrayList<String> transactionHistory = new ArrayList<>();

    public void add(Account account) {
        if (account == null) {
            return;
        }
        accounts.put(String.valueOf(account.getAccountNumber()), account);
        transactionHistory.add("Added account " + account.getAccountNumber());
    }

    public Account get(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public Account get(int accountNumber) {
        return accounts.get(String.valueOf(accountNumber));
    }

    public ArrayList<Account> getAll() {
        return new ArrayList<>(accounts.values());
    }

    public void addTransaction(String record) {
        transactionHistory.add(record);
    }

    public ArrayList<String> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    public ArrayList<Account> sortedByBalance() {
        ArrayList<Account> list = new ArrayList<>(accounts.values());
        list.sort(new Comparator<Account>() {
            @Override
            public int compare(Account a1, Account a2) {
                return Double.compare(a2.getBalance(), a1.getBalance());
            }
        });
        return list;
    }

    public int size() {
        return accounts.size();
    }
}
