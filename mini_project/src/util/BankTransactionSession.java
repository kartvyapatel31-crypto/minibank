package util;

public class BankTransactionSession implements AutoCloseable {
    private final String sessionName;

    public BankTransactionSession(String sessionName) {
        this.sessionName = sessionName;
        System.out.println("[Session Opened]: " + sessionName);
    }

    public void log(String message) {
        System.out.println("[Session " + sessionName + "]: " + message);
    }

    @Override
    public void close() {
        System.out.println("[Session Closed]: " + sessionName);
    }
}
