package model;

public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    private long minBalance;

    public SavingsAccount(int accountNumber, String ownerName, double balance, long minBalance) {
        super(accountNumber, ownerName, balance);
        this.minBalance = minBalance;
    }

    public long getMinBalance() {
        return minBalance;
    }

    @Override
    public double interestRate() {
        return 4.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return (getBalance() - amount) >= minBalance;
    }

    @Override
    public long getAvailableBalance() {
        return (long) Math.max(0, getBalance() - minBalance);
    }
}
