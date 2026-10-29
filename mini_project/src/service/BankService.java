package service;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.Command;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import model.TransactionType;
import model.annotation.Audit;
import util.AnnotationValidator;
import util.Validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BankService coordinates the domain models, validation layer, generic repository,
 * concurrency controls, file persistence, and audit logging.
 * Practical 14 Requirement 1: Core service wiring together Repository<Account>,
 * Validator, and Exception classes.
 */
public class BankService implements BankOperations {

    private final Repository<Account> repository;
    private final Path logsFolder;
    private final Path dailyLogFile;

    public BankService() {
        this(new Repository<>());
    }

    public BankService(Repository<Account> repository) {
        this.repository = repository != null ? repository : new Repository<>();
        Path base = Files.exists(Paths.get("mini_project")) ? Paths.get("mini_project") : Paths.get(".");
        this.logsFolder = base.resolve("logs");
        this.dailyLogFile = logsFolder.resolve("daily_transactions.log");
    }

    public Repository<Account> getRepository() {
        return repository;
    }

    public Path getLogsFolder() {
        return logsFolder;
    }

    public Path getDailyLogFile() {
        return dailyLogFile;
    }

    @Override
    @Audit(action = "OPEN_ACCOUNT")
    public Account openAccount(Account account) throws BankException {
        if (account == null) {
            throw new BankException("Account details cannot be null.");
        }

        String id = String.valueOf(account.getAccountNumber());
        if (repository.existsById(id)) {
            throw new BankException("Account with number " + id + " already exists.");
        }

        // Validate using AnnotationValidator (@Positive, @MaxLength)
        String[] errors = AnnotationValidator.validate(account);
        if (errors != null && errors.length > 0) {
            throw new BankException("Account validation failed: " + String.join("; ", errors));
        }

        repository.save(account);
        logEntry("OPEN " + id + " " + (long) account.getBalance());
        return account;
    }

    /**
     * Convenience factory helper to open an account with customer contact validation.
     */
    public Account openAccount(int typeChoice, int accountNumber, String ownerName, double initialBalance,
                               long extraParam, String mobile, String email) throws BankException {
        // Validate contact details using Validator
        if (mobile != null && !mobile.isBlank() && !Validator.isValidMobile(mobile)) {
            throw new BankException("Invalid Indian mobile number format (must be 10 digits starting with 6-9): " + mobile);
        }
        if (email != null && !email.isBlank() && !Validator.isValidEmail(email)) {
            throw new BankException("Invalid email address format: " + email);
        }

        Account acc = switch (typeChoice) {
            case 1 -> new SavingsAccount(accountNumber, ownerName, initialBalance, extraParam);
            case 2 -> new CurrentAccount(accountNumber, ownerName, initialBalance, extraParam);
            case 3 -> new FixedDepositAccount(accountNumber, ownerName, initialBalance);
            default -> throw new BankException("Invalid account type selection: " + typeChoice);
        };

        return openAccount(acc);
    }

    @Override
    @Audit(action = "DEPOSIT")
    public Account deposit(String accountNumber, long amount) throws BankException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero. Received: " + amount);
        }

        Account account = repository.findById(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account number " + accountNumber + " not found.");
        }

        account.deposit(amount);
        logEntry("DEPOSIT " + accountNumber + " " + amount);
        return account;
    }

    @Override
    @Audit(action = "WITHDRAW")
    public Account withdraw(String accountNumber, long amount) throws BankException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero. Received: " + amount);
        }

        Account account = repository.findById(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account number " + accountNumber + " not found.");
        }

        account.withdraw(amount);
        logEntry("WITHDRAW " + accountNumber + " " + amount);
        return account;
    }

    @Override
    @Audit(action = "TRANSFER")
    public void transfer(String fromAccountNumber, String toAccountNumber, long amount) throws BankException {
        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be greater than zero. Received: " + amount);
        }

        Account from = repository.findById(fromAccountNumber);
        if (from == null) {
            throw new AccountNotFoundException("Source account " + fromAccountNumber + " not found.");
        }

        Account to = repository.findById(toAccountNumber);
        if (to == null) {
            throw new AccountNotFoundException("Destination account " + toAccountNumber + " not found.");
        }

        // Deadlock-safe atomic transfer from Practical 10
        Account.transferSafe(from, to, amount);
        logEntry("WITHDRAW " + fromAccountNumber + " " + amount);
        logEntry("DEPOSIT " + toAccountNumber + " " + amount);
    }

    @Override
    @Audit(action = "STATEMENT")
    public String statement(String accountNumber) throws BankException {
        Account account = repository.findById(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("Account number " + accountNumber + " not found.");
        }
        return StatementFormatter.buildStatement(account);
    }

    @Override
    @Audit(action = "LIST_ACCOUNTS")
    public ArrayList<Account> listAccounts() {
        return repository.findAll();
    }

    /**
     * Practical 14 Requirement 3: Process a batch of transactions safely using
     * TransactionProcessor (Practical 10) over a thread-safe ConcurrentHashMap store (Practical 12).
     */
    @Audit(action = "PROCESS_BATCH")
    public void processBatch(List<Command> commands) {
        if (commands == null || commands.isEmpty()) {
            System.out.println("No batch transactions to process.");
            return;
        }

        ConcurrentHashMap<String, Account> threadSafeMap = new ConcurrentHashMap<>();
        for (Account acc : repository.findAll()) {
            threadSafeMap.put(String.valueOf(acc.getAccountNumber()), acc);
        }

        TransactionProcessor processor = new TransactionProcessor(threadSafeMap);

        System.out.println("Starting batch transaction execution for " + commands.size() + " commands...");
        for (Command cmd : commands) {
            if (cmd.type() == TransactionType.DEPOSIT) {
                processor.processDeposit(cmd.accountNumber(), cmd.amount());
                logEntry("DEPOSIT " + cmd.accountNumber() + " " + cmd.amount());
            } else if (cmd.type() == TransactionType.WITHDRAW) {
                processor.processWithdraw(cmd.accountNumber(), cmd.amount());
                logEntry("WITHDRAW " + cmd.accountNumber() + " " + cmd.amount());
            } else if (cmd.type() == TransactionType.TRANSFER) {
                processor.submit(() -> {
                    Account src = threadSafeMap.get(cmd.accountNumber());
                    if (src != null) {
                        try {
                            src.withdraw(cmd.amount());
                            logEntry("WITHDRAW " + cmd.accountNumber() + " " + cmd.amount());
                        } catch (BankException e) {
                            System.err.println("Batch transfer failed on " + cmd.accountNumber() + ": " + e.getMessage());
                        }
                    }
                });
            }
        }

        processor.stop();
        System.out.println("Batch transaction processing completed successfully.");
    }

    /**
     * Practical 14 Requirement 2 & Practical 11:
     * Load saved accounts on startup using ObjectInputStream.
     */
    public boolean loadSavedAccounts(Path accountsFile) {
        try {
            if (Files.exists(accountsFile)) {
                Account[] loaded = StatePersister.load(accountsFile);
                if (loaded != null && loaded.length > 0) {
                    repository.saveAll(loaded);
                    System.out.println("Successfully loaded " + loaded.length + " accounts from " + accountsFile);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load accounts from " + accountsFile + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Practical 14 Requirement 2 & Practical 11:
     * Save all accounts on exit using ObjectOutputStream.
     */
    public boolean saveAccounts(Path accountsFile) {
        try {
            ArrayList<Account> accounts = repository.findAll();
            StatePersister.save(accounts.toArray(new Account[0]), accountsFile);
            System.out.println("Successfully saved " + accounts.size() + " accounts to " + accountsFile);
            return true;
        } catch (Exception e) {
            System.err.println("Error saving accounts to " + accountsFile + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Practical 14 Requirement 3 & Practical 11:
     * Generates a daily summary report from transaction logs.
     */
    public String generateReport(Path logsFolder, Path reportFile) throws IOException {
        return ReportGenerator.generateReport(logsFolder, reportFile);
    }

    private void logEntry(String entry) {
        try {
            TransactionLog.append(dailyLogFile, entry);
        } catch (IOException e) {
            System.err.println("Warning: Failed to log transaction: " + e.getMessage());
        }
    }
}

