package model;

public class FixedDepositAccount extends Account {
    private static final long serialVersionUID = 1L;
    
    public FixedDepositAccount(int accountNumber, String ownerName, double balance) {
        super(accountNumber, ownerName, balance);
    }

    @Override
    public double interestRate() {
        return 7.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        return false;
    }

    @Override
    public long getAvailableBalance() {
        return 0L;
    }
}
