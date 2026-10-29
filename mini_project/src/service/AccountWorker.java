package service;

import exception.InvalidAmountException;
import model.Account;

public class AccountWorker implements Runnable {
    private final Account account;
    private final int times;
    private final long amount;
    private final boolean unsync;

    public AccountWorker(Account account, int times, long amount) {
        this(account, times, amount, false);
    }

    public AccountWorker(Account account, int times, long amount, boolean unsync) {
        this.account = account;
        this.times = times;
        this.amount = amount;
        this.unsync = unsync;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " [Lifecycle: START]");
        for (int i = 0; i < times; i++) {
            try {
                if (unsync) {
                    account.depositUnsync(amount);
                } else {
                    account.deposit(amount);
                }
            } catch (InvalidAmountException e) {
                System.err.println(Thread.currentThread().getName() + " error: " + e.getMessage());
            }
        }
        System.out.println(Thread.currentThread().getName() + " [Lifecycle: FINISH]");
    }
}
