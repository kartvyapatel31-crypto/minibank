package model;

public class CurrentAccount extends Account {
    private static final long serialVersionUID = 1L;

    private long overdraftLimit;

    public CurrentAccount(int accountNumber, String ownerName, double balance, long overdraftLimit) {
        super(accountNumber, ownerName, balance);
        this.overdraftLimit = overdraftLimit;
    }

    public long getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public double interestRate() {
        return 0.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return (getBalance() - amount) >= -overdraftLimit;
    }

    @Override
    public long getAvailableBalance() {
        return (long) Math.max(0, getBalance() + overdraftLimit);
    }
}
