import exception.*;
import model.*;
import model.annotation.*;
import service.*;
import util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

// Requirement 2.5: Use one static import
import static util.Validator.isValidMobile;

public class MiniBank {

    public static void main(String[] args) {

        // Requirement 1.6: place objects of the three types in an Account[] array, loop through it and call interestRate() on each
        Account[] accounts = new Account[3];
        accounts[0] = new SavingsAccount(101, "Alice", 5000, 1000);
        accounts[1] = new CurrentAccount(102, "Bob", 2000, 5000);
        accounts[2] = new FixedDepositAccount(103, "Charlie", 10000);

        System.out.println("----- Account Array Polymorphism -----");
        for (Account acc : accounts) {
            System.out.println(acc.getOwnerName() + " interest rate: " + acc.interestRate());

            // pattern instanceof check to handle one type specially
            if (acc instanceof SavingsAccount sa) {
                System.out.println("  -> This is a Savings Account!");
            } else if (acc instanceof CurrentAccount ca) {
                System.out.println("  -> This is a Current Account!");
            } else if (acc instanceof FixedDepositAccount fda) {
                System.out.println("  -> This is a Fixed Deposit Account!");
            }
        }

        // Requirement 2.3: Use WithdrawRule two ways in main: first with an anonymous class, then with a lambda expression
        System.out.println("\n----- WithdrawRule Testing -----");

        WithdrawRule rule1 = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return account.canWithdraw(amount);
            }
        };
        System.out.println("Anonymous class rule allow 1000 from Alice? " + rule1.allow(accounts[0], 1000));

        WithdrawRule rule2 = (account, amount) -> account.canWithdraw(amount);
        System.out.println("Lambda expression rule allow 15000 from Bob? " + rule2.allow(accounts[1], 15000));

        // Requirement 3.5: In main, create an Account with a negative balance and run AnnotationValidator.validate() to print the error.
        System.out.println("\n----- Annotation Validator Testing -----");
        Account negativeAcc = new SavingsAccount(104, "Dave", -500, 0);
        String[] errors = AnnotationValidator.validate(negativeAcc);
        for (String error : errors) {
            System.out.println("Validation Error: " + error);
        }

        System.out.println("\nStatic Import Test - isValidMobile: " + isValidMobile("9876543210"));

        // ===== Week 8: Exception Handling & Fault-Tolerance =====
        System.out.println("\n----- Week 8: Exception Handling & Fault-Tolerance -----");

        // Try-with-resources with AutoCloseable BankTransactionSession
        try (BankTransactionSession session = new BankTransactionSession("Txn-Session-01")) {
            session.log("Initiating operations for Alice (Acc 101) and Bob (Acc 102)");

            // 1. Successful Deposit & Withdrawal
            try {
                accounts[0].deposit(1000);
                session.log("Deposited 1000 into Alice. New balance: " + accounts[0].getBalance());

                accounts[0].withdraw(2000);
                session.log("Withdrew 2000 from Alice. New balance: " + accounts[0].getBalance());
            } catch (BankException e) {
                System.out.println("Operation failed: " + e.getMessage());
            } finally {
                System.out.println("Operation block 1 finalized.");
            }

            // 2. InvalidAmountException demonstration (non-positive amount)
            try {
                session.log("Attempting negative deposit into Alice...");
                accounts[0].deposit(-200);
            } catch (InvalidAmountException e) {
                System.out.println("Caught Expected InvalidAmountException: " + e.getMessage());
            } finally {
                System.out.println("Deposit validation check finalized.");
            }

            // 3. InsufficientFundsException demonstration (with shortfall)
            try {
                session.log("Attempting excessive withdrawal from Alice...");
                accounts[0].withdraw(50000);
            } catch (InsufficientFundsException e) {
                System.out.println("Caught Expected InsufficientFundsException: " + e.getMessage() +
                        " [Shortfall: " + e.getShortfall() + "]");
            } catch (BankException e) {
                System.out.println("BankException: " + e.getMessage());
            } finally {
                System.out.println("Withdrawal funds check finalized.");
            }

            // 4. AccountNotFoundException demonstration
            try {
                session.log("Attempting transfer to null account...");
                accounts[0].transfer(null, 500);
            } catch (AccountNotFoundException e) {
                System.out.println("Caught Expected AccountNotFoundException: " + e.getMessage());
            } catch (BankException e) {
                System.out.println("BankException: " + e.getMessage());
            } finally {
                System.out.println("Null account transfer check finalized.");
            }

            // 5. Successful Transfer demonstration
            try {
                session.log("Transferring 500 from Alice (101) to Bob (102)...");
                accounts[0].transfer(accounts[1], 500);
                session.log("Transfer successful! Alice Balance: " + accounts[0].getBalance() +
                        ", Bob Balance: " + accounts[1].getBalance());
            } catch (BankException e) {
                System.out.println("Transfer failed: " + e.getMessage());
            } finally {
                System.out.println("Valid transfer check finalized.");
            }
        } // AutoCloseable session closed here

        // ===== Week 9: Concurrency, Race Conditions & Synchronization =====
        System.out.println("\n----- Week 9: Concurrency, Race Conditions & Synchronization -----");

        // Part 1: Unsynchronized deposits across 10 threads (demonstrating Race Condition)
        System.out.println("-> Test 1: Unsynchronized Deposits (Demonstrating Race Condition)");
        Account raceAccount = new SavingsAccount(201, "RaceTest", 0, 0);
        int numThreads = 10;
        int depositsPerThread = 50;
        long depositAmount = 10;
        double expectedTotal = (double) numThreads * depositsPerThread * depositAmount;

        Thread[] unsyncThreads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            unsyncThreads[i] = new Thread(
                    new AccountWorker(raceAccount, depositsPerThread, depositAmount, true),
                    "Worker-Unsync-" + (i + 1)
            );
            unsyncThreads[i].start();
        }

        for (Thread t : unsyncThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Expected Balance: " + expectedTotal);
        System.out.println("Actual Balance (Unsynchronized): " + raceAccount.getBalance());
        if (raceAccount.getBalance() < expectedTotal) {
            System.out.println("Observed Race Condition: Lost updates occurred due to lack of synchronization.");
        } else {
            System.out.println("Note: No lost updates observed in this run.");
        }

        // Part 2: Synchronized deposits across 10 threads (Thread-safe)
        System.out.println("\n-> Test 2: Synchronized Deposits (Thread-Safe with Synchronization)");
        Account syncAccount = new SavingsAccount(202, "SyncTest", 0, 0);
        Thread[] syncThreads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            syncThreads[i] = new Thread(
                    new AccountWorker(syncAccount, depositsPerThread, depositAmount, false),
                    "Worker-Sync-" + (i + 1)
            );
            syncThreads[i].start();
        }

        for (Thread t : syncThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Expected Balance: " + expectedTotal);
        System.out.println("Actual Balance (Synchronized): " + syncAccount.getBalance());
        if (syncAccount.getBalance() == expectedTotal) {
            System.out.println("Synchronization Success: Balance is exactly correct! All 10 threads synchronized safely.");
        }

        // ===== Week 10: Thread Pools, Producer-Consumer & Deadlock Prevention =====
        System.out.println("\n----- Week 10: Thread Pools, Producer-Consumer & Deadlock Prevention -----");

        // 1 & 2. TransactionProcessor with ExecutorService (FixedThreadPool of 4)
        System.out.println("-> Part 1 & 2: Batch Transaction Processing via TransactionProcessor (FixedThreadPool-4)");
        TransactionProcessor processor = new TransactionProcessor();
        Account poolAccount = new SavingsAccount(301, "PoolUser", 1000, 500);

        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            if (taskId % 2 != 0) {
                processor.submit(() -> {
                    try {
                        poolAccount.deposit(200);
                        System.out.println("[PoolTask-" + taskId + "] Deposited 200. Balance: " + poolAccount.getBalance());
                    } catch (BankException e) {
                        System.err.println("[PoolTask-" + taskId + "] Error: " + e.getMessage());
                    }
                });
            } else {
                processor.submit(() -> {
                    try {
                        poolAccount.withdraw(100);
                        System.out.println("[PoolTask-" + taskId + "] Withdrew 100. Balance: " + poolAccount.getBalance());
                    } catch (BankException e) {
                        System.err.println("[PoolTask-" + taskId + "] Error: " + e.getMessage());
                    }
                });
            }
        }
        processor.stop();
        System.out.println("All thread pool transactions completed. Final Balance: " + poolAccount.getBalance());

        // 3. Producer-Consumer using wait() & notify() with TransactionBuffer
        System.out.println("\n-> Part 3: Producer-Consumer Coordination with wait() & notify()");
        TransactionBuffer buffer = new TransactionBuffer(3);
        Thread producer = new Thread(() -> {
            Command[] testCmds = {
                    new Command(TransactionType.DEPOSIT, "301", 500),
                    new Command(TransactionType.WITHDRAW, "301", 200),
                    new Command(TransactionType.TRANSFER, "301", 300),
                    new Command(TransactionType.DEPOSIT, "301", 1000)
            };
            for (Command cmd : testCmds) {
                try {
                    buffer.produce(cmd);
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Txn-Producer");

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 4; i++) {
                try {
                    buffer.consume();
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Txn-Consumer");

        producer.start();
        consumer.start();
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Producer-Consumer coordination completed successfully.");

        // 4. Deadlock Creation and Resolution
        System.out.println("\n-> Part 4: Deadlock Creation and Lock-Ordering Resolution");
        Account accA = new SavingsAccount(10, "UserA", 5000, 100);
        Account accB = new SavingsAccount(20, "UserB", 5000, 100);

        System.out.println("Step 4a: Demonstrating Deadlock (Opposite Lock Ordering)...");
        Thread deadlockT1 = new Thread(() -> {
            try {
                Account.transferUnsafe(accA, accB, 100);
            } catch (BankException ignored) {}
        }, "Deadlock-Thread-AtoB");

        Thread deadlockT2 = new Thread(() -> {
            try {
                Account.transferUnsafe(accB, accA, 100);
            } catch (BankException ignored) {}
        }, "Deadlock-Thread-BtoA");

        deadlockT1.setDaemon(true);
        deadlockT2.setDaemon(true);
        deadlockT1.start();
        deadlockT2.start();

        try {
            deadlockT1.join(300);
            deadlockT2.join(300);
        } catch (InterruptedException ignored) {}

        if (deadlockT1.isAlive() && deadlockT2.isAlive()) {
            System.out.println(">>> DEADLOCK DETECTED! Both threads are stuck in circular wait waiting for each other's locks.");
        }

        System.out.println("\nStep 4b: Resolving Deadlock (Consistent Lock Ordering: smaller ID first)...");
        Account safeAccA = new SavingsAccount(10, "SafeUserA", 5000, 100);
        Account safeAccB = new SavingsAccount(20, "SafeUserB", 5000, 100);

        Thread safeT1 = new Thread(() -> {
            try {
                Account.transferSafe(safeAccA, safeAccB, 100);
            } catch (BankException e) {
                System.err.println(e.getMessage());
            }
        }, "Safe-Thread-AtoB");

        Thread safeT2 = new Thread(() -> {
            try {
                Account.transferSafe(safeAccB, safeAccA, 100);
            } catch (BankException e) {
                System.err.println(e.getMessage());
            }
        }, "Safe-Thread-BtoA");

        safeT1.start();
        safeT2.start();

        try {
            safeT1.join();
            safeT2.join();
        } catch (InterruptedException ignored) {}

        System.out.println(">>> DEADLOCK PREVENTED! Transfers executed successfully without blocking.");
        System.out.println("Final AccA Balance: " + accA.getBalance() + ", AccB Balance: " + accB.getBalance());

        // Week 11: Persistence and Reporting
        System.out.println("\n--- Week 11: Persistence and Reporting ---");

        Path baseDir = Files.exists(Paths.get("mini_project")) ? Paths.get("mini_project") : Paths.get(".");
        Path accountsFile = baseDir.resolve("data").resolve("accounts.ser");

        Account[] originalAccounts = {
                new SavingsAccount(401, "Grace", 7500, 1000),
                new CurrentAccount(402, "Alan", 12000, 5000),
                new FixedDepositAccount(403, "Ada", 25000)
        };

        try {
            StatePersister.save(originalAccounts, accountsFile);
            System.out.println("Saved accounts to " + accountsFile);

            Account[] reloadedAccounts = StatePersister.load(accountsFile);
            System.out.println("Reloaded accounts from file:");
            for (Account acc : reloadedAccounts) {
                System.out.println("  " + acc);
            }
        } catch (Exception e) {
            System.out.println("Persistence error: " + e.getMessage());
        }

        Path logsDir = baseDir.resolve("logs");
        Path dailyLog = logsDir.resolve("daily_transactions.log");

        try {
            TransactionLog.append(dailyLog, "DEPOSIT 401 2500");
            TransactionLog.append(dailyLog, "WITHDRAW 401 1000");
            TransactionLog.append(dailyLog, "DEPOSIT 402 5000");
            TransactionLog.append(dailyLog, "WITHDRAW 402 1500");
            TransactionLog.append(dailyLog, "DEPOSIT 403 10000");
            System.out.println("Appended transactions to log file.");
        } catch (Exception e) {
            System.out.println("Log error: " + e.getMessage());
        }

        Path reportFile = baseDir.resolve("reports").resolve("daily_summary_report.txt");
        try {
            String report = ReportGenerator.generateReport(logsDir, reportFile);
            System.out.println("\n" + report);
        } catch (Exception e) {
            System.out.println("Report error: " + e.getMessage());
        }

        // Week 12: Collections Framework
        System.out.println("--- Week 12: Collections Framework ---");

        AccountStore store = new AccountStore();
        store.add(new SavingsAccount(501, "Tesla", 8500, 1000));
        store.add(new CurrentAccount(502, "Marie", 15200, 3000));
        store.add(new FixedDepositAccount(503, "Albert", 30000));
        store.add(new SavingsAccount(504, "Newton", 4200, 500));
        store.add(new CurrentAccount(505, "Galileo", 19500, 6000));

        System.out.println("Accounts sorted by balance:");
        ArrayList<Account> byBalance = store.sortedByBalance();
        for (Account acc : byBalance) {
            System.out.println("  " + acc.getAccountNumber() + " - " + acc.getOwnerName() + ": " + acc.getBalance());
        }

        System.out.println("\nAccounts sorted by account number (Comparable):");
        ArrayList<Account> byAccNo = store.getAll();
        Collections.sort(byAccNo);
        for (Account acc : byAccNo) {
            System.out.println("  " + acc.getAccountNumber() + " - " + acc.getOwnerName() + ": " + acc.getBalance());
        }

        System.out.println("\nTransaction History:");
        for (String record : store.getTransactionHistory()) {
            System.out.println("  " + record);
        }

        System.out.println("\nConcurrent processing with ConcurrentHashMap:");
        ConcurrentHashMap<String, Account> concurrentMap = new ConcurrentHashMap<>();
        for (Account acc : store.getAll()) {
            concurrentMap.put(String.valueOf(acc.getAccountNumber()), acc);
        }

        TransactionProcessor concurrentProcessor = new TransactionProcessor(concurrentMap);
        concurrentProcessor.processDeposit("501", 1500);
        concurrentProcessor.processWithdraw("502", 2000);
        concurrentProcessor.processDeposit("504", 800);
        concurrentProcessor.processWithdraw("505", 3500);
        concurrentProcessor.stop();

        System.out.println("Updated 501 balance: " + concurrentMap.get("501").getBalance());
        System.out.println("Updated 502 balance: " + concurrentMap.get("502").getBalance());

        // ===== Week 13: Generic Repository & Reflection =====
        System.out.println("\n--- Week 13: Generic Repository & Reflection ---");
        Repository<Account> genericRepo = new Repository<>();
        Account repoAcc1 = new SavingsAccount(601, "Grace Hopper", 14000, 1000);
        Account repoAcc2 = new CurrentAccount(602, "Ada Lovelace", 22000, 5000);
        Account repoAcc3 = new FixedDepositAccount(603, "Charles Babbage", 35000);

        // Requirement 13.3: @SafeVarargs saveAll() saving several objects at once
        genericRepo.saveAll(repoAcc1, repoAcc2, repoAcc3);
        System.out.println("Saved 3 accounts to generic Repository<Account> via @SafeVarargs saveAll().");

        // Requirement 13.2 & 13.5: Reflection lookup of @Id during save, fetch by id, print all
        Account fetchedById = genericRepo.findById("602");
        System.out.println("Fetched by id ('602'): " + fetchedById);

        System.out.println("All accounts in generic repository:");
        for (Account a : genericRepo.findAll()) {
            System.out.println("  " + a);
        }

        // Requirement 13.4: Bounded type parameter generic method static <N extends Number> double average(N[] values)
        Double[] balanceArray = new Double[]{repoAcc1.getBalance(), repoAcc2.getBalance(), repoAcc3.getBalance()};
        double averageBal = Repository.average(balanceArray);
        System.out.println("Average balance across accounts (bounded generic <N extends Number>): " + averageBal);

        // ===== Week 14: Capstone Assembly & BankService =====
        System.out.println("\n--- Week 14: Capstone Assembly & BankService ---");

        // Requirement 14.1: Wire BankService with Repository<Account>, Validator, and Exceptions
        BankService bankService = new BankService();

        // Requirement 14.5: @Audit annotation and reflective logger with dynamic proxy
        BankOperations auditedBank = AuditLogger.createAuditedProxy(BankOperations.class, bankService);

        // Requirement 14.2: Load saved accounts on startup (Practical 11)
        boolean loaded = bankService.loadSavedAccounts(accountsFile);
        if (!loaded || bankService.getRepository().size() == 0) {
            System.out.println("Seeding default accounts into repository...");
            try {
                bankService.openAccount(new SavingsAccount(101, "Alice", 5000, 1000));
                bankService.openAccount(new CurrentAccount(102, "Bob", 2000, 5000));
                bankService.openAccount(new FixedDepositAccount(103, "Charlie", 10000));
            } catch (BankException e) {
                System.out.println("Seed error: " + e.getMessage());
            }
        }

        // Register JVM shutdown hook to guarantee accounts are saved on abnormal exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            bankService.saveAccounts(accountsFile);
        }));

        System.out.println("\n----- MiniBank Interactive Capstone Menu -----");
        Scanner sc = new Scanner(System.in);
        BankInfo bank = new BankInfo("MiniBank", "Main Branch");

        System.out.println("Welcome to " + bank.name() + ", " + bank.branch());

        int choice;
        do {
            System.out.println("\n=================================");
            System.out.println("1. Open Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Account Statement");
            System.out.println("6. List All Accounts");
            System.out.println("7. Safe Batch Processing (Practical 10 & 12)");
            System.out.println("8. Generate Daily Report (Practical 11)");
            System.out.println("9. Exit");
            System.out.print("Enter choice (1-9): ");

            if (!sc.hasNextInt()) {
                System.out.println("\nNon-interactive session or EOF detected. Exiting menu loop.");
                break;
            }

            choice = sc.nextInt();
            sc.nextLine(); // consume leftover newline

            switch (choice) {
                case 1 -> {
                    System.out.println("\n--- Open Account ---");
                    System.out.println("Select Account Type: 1. Savings | 2. Current | 3. Fixed Deposit");
                    System.out.print("Type choice: ");
                    int type = sc.hasNextInt() ? sc.nextInt() : 1;
                    sc.nextLine();

                    System.out.print("Enter Account Number (integer): ");
                    int accNum = sc.hasNextInt() ? sc.nextInt() : 0;
                    sc.nextLine();

                    System.out.print("Enter Owner Name: ");
                    String name = sc.nextLine().trim();

                    System.out.print("Enter Initial Balance: ");
                    double initBal = sc.hasNextDouble() ? sc.nextDouble() : 0.0;
                    sc.nextLine();

                    long extraParam = 0;
                    if (type == 1) {
                        System.out.print("Enter Minimum Balance: ");
                        extraParam = sc.hasNextLong() ? sc.nextLong() : 500;
                        sc.nextLine();
                    } else if (type == 2) {
                        System.out.print("Enter Overdraft Limit: ");
                        extraParam = sc.hasNextLong() ? sc.nextLong() : 2000;
                        sc.nextLine();
                    }

                    System.out.print("Enter Mobile Number (10 digits, optional - press enter to skip): ");
                    String mobile = sc.nextLine().trim();

                    System.out.print("Enter Email Address (optional - press enter to skip): ");
                    String email = sc.nextLine().trim();

                    try {
                        Account opened = bankService.openAccount(type, accNum, name, initBal, extraParam, mobile, email);
                        System.out.println("Account successfully opened:\n  " + opened);
                    } catch (BankException e) {
                        System.err.println("Failed to open account: " + e.getMessage());
                    }
                }
                case 2 -> {
                    System.out.println("\n--- Deposit ---");
                    System.out.print("Enter Account Number: ");
                    String accNum = sc.nextLine().trim();
                    System.out.print("Enter Amount to Deposit: ");
                    long amount = sc.hasNextLong() ? sc.nextLong() : 0;
                    sc.nextLine();

                    try {
                        Account updated = auditedBank.deposit(accNum, amount);
                        System.out.println("Deposit Successful! New Balance: " + updated.getBalance());
                    } catch (BankException e) {
                        System.err.println("Deposit Failed: " + e.getMessage());
                    }
                }
                case 3 -> {
                    System.out.println("\n--- Withdraw ---");
                    System.out.print("Enter Account Number: ");
                    String accNum = sc.nextLine().trim();
                    System.out.print("Enter Amount to Withdraw: ");
                    long amount = sc.hasNextLong() ? sc.nextLong() : 0;
                    sc.nextLine();

                    try {
                        Account updated = auditedBank.withdraw(accNum, amount);
                        System.out.println("Withdrawal Successful! Remaining Balance: " + updated.getBalance());
                    } catch (InsufficientFundsException e) {
                        System.err.println("Insufficient Funds! Shortfall: " + e.getShortfall());
                    } catch (BankException e) {
                        System.err.println("Withdrawal Failed: " + e.getMessage());
                    }
                }
                case 4 -> {
                    System.out.println("\n--- Transfer ---");
                    System.out.print("Enter Source Account Number: ");
                    String fromAcc = sc.nextLine().trim();
                    System.out.print("Enter Destination Account Number: ");
                    String toAcc = sc.nextLine().trim();
                    System.out.print("Enter Amount to Transfer: ");
                    long amount = sc.hasNextLong() ? sc.nextLong() : 0;
                    sc.nextLine();

                    try {
                        auditedBank.transfer(fromAcc, toAcc, amount);
                        System.out.println("Transfer of " + amount + " from " + fromAcc + " to " + toAcc + " completed successfully!");
                    } catch (BankException e) {
                        System.err.println("Transfer Failed: " + e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.println("\n--- Account Statement ---");
                    System.out.print("Enter Account Number: ");
                    String accNum = sc.nextLine().trim();

                    try {
                        String stmt = auditedBank.statement(accNum);
                        System.out.println(stmt);
                    } catch (BankException e) {
                        System.err.println("Statement Error: " + e.getMessage());
                    }
                }
                case 6 -> {
                    System.out.println("\n--- Registered Accounts ---");
                    ArrayList<Account> all = auditedBank.listAccounts();
                    if (all.isEmpty()) {
                        System.out.println("No accounts currently registered.");
                    } else {
                        for (Account a : all) {
                            System.out.println("  [" + a.getClass().getSimpleName() + "] Acc#" + a.getAccountNumber() +
                                    " | Owner: " + a.getOwnerName() + " | Balance: " + a.getBalance());
                        }
                    }
                }
                case 7 -> {
                    System.out.println("\n--- Safe Batch Processing (TransactionProcessor + ConcurrentHashMap) ---");
                    List<Command> batch = Arrays.asList(
                            new Command(TransactionType.DEPOSIT, "101", 1000),
                            new Command(TransactionType.WITHDRAW, "102", 500),
                            new Command(TransactionType.DEPOSIT, "103", 2000),
                            new Command(TransactionType.TRANSFER, "101", 300)
                    );
                    bankService.processBatch(batch);
                }
                case 8 -> {
                    System.out.println("\n--- Daily Summary Report ---");
                    try {
                        String summary = bankService.generateReport(logsDir, reportFile);
                        System.out.println(summary);
                    } catch (Exception e) {
                        System.err.println("Report generation error: " + e.getMessage());
                    }
                }
                case 9 -> {
                    System.out.println("Saving accounts and shutting down...");
                    bankService.saveAccounts(accountsFile);
                    System.out.println("Thank you for using MiniBank. Goodbye!");
                }
                default -> System.out.println("Invalid selection. Please choose an option from 1 to 9.");
            }

        } while (choice != 9);

        // Ensure accounts saved on normal menu loop termination
        bankService.saveAccounts(accountsFile);
        sc.close();
    }
}
