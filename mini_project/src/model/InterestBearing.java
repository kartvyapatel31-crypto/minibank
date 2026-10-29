package model;

public interface InterestBearing {
    double interestRate();
    double getBalance();
    
    default double yearlyInterest() {
        return getBalance() * (interestRate() / 100.0);
    }
}
