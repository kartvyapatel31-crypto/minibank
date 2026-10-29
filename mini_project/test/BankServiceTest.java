package test;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.BankOperations;
import service.BankService;
import service.AuditLogger;
import service.Repository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for MiniBank Capstone (Practical 14 Requirement 4).
 * Tests core banking operations, validation, exceptions, thread safety, and generic repository reflection.
 */
public class BankServiceTest {

    private Repository<Account> repository;
    private BankService bankService;
    private BankOperations auditedBank;

    @BeforeEach
    public void setUp() {
        repository = new Repository<>();
        bankService = new BankService(repository);
        auditedBank = AuditLogger.createAuditedProxy(BankOperations.class, bankService);
    }

    /**
     * Requirement 14.4 (Test 1): A deposit increases the balance.
     */
    @Test
    @DisplayName("Deposit increases account balance correctly")
    public void testDepositIncreasesBalance() throws BankException {
        SavingsAccount account = new SavingsAccount(1001, "TestUser1", 1000.0, 500);
        auditedBank.openAccount(account);

        Account updated = auditedBank.deposit("1001", 750);

        assertEquals(1750.0, updated.getBalance(), 0.001, "Balance should increase by 750");
        assertEquals(1750.0, repository.findById("1001").getBalance(), 0.001, "Repository entity should reflect updated balance");
    }

    /**
     * Requirement 14.4 (Test 2): A withdrawal over the limit throws InsufficientFundsException.
     */
    @Test
    @DisplayName("Withdrawal exceeding available funds throws InsufficientFundsException")
    public void testWithdrawalOverLimitThrowsException() throws BankException {
        // Savings Account: balance 1000, minBalance 500 => can withdraw at most 500
        SavingsAccount savings = new SavingsAccount(1002, "TestSavings", 1000.0, 500);
        auditedBank.openAccount(savings);

        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> auditedBank.withdraw("1002", 700),
                "Withdrawing 700 with minBalance 500 should throw InsufficientFundsException"
        );

        assertTrue(ex.getShortfall() > 0, "Shortfall should be positive");
        assertEquals(1000.0, savings.getBalance(), "Failed withdrawal must not alter balance");

        // Current Account: balance 1000, overdraftLimit 500 => can withdraw at most 1500
        CurrentAccount current = new CurrentAccount(1003, "TestCurrent", 1000.0, 500);
        auditedBank.openAccount(current);

        assertThrows(
                InsufficientFundsException.class,
                () -> auditedBank.withdraw("1003", 2000),
                "Withdrawing 2000 with overdraft 500 should throw InsufficientFundsException"
        );
    }

    /**
     * Requirement 14.4 (Test 3): Many concurrent deposits keep the balance correct (Thread-safety).
     */
    @Test
    @DisplayName("Many concurrent deposits keep balance exactly correct without lost updates")
    public void testConcurrentDepositsKeepBalanceCorrect() throws Exception {
        double initialBalance = 1000.0;
        SavingsAccount shared = new SavingsAccount(1004, "ConcurrentHolder", initialBalance, 0);
        bankService.openAccount(shared);

        int numThreads = 10;
        int depositsPerThread = 100;
        long amountPerDeposit = 25;
        double expectedFinalBalance = initialBalance + ((double) numThreads * depositsPerThread * amountPerDeposit);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < depositsPerThread; j++) {
                        bankService.deposit("1004", amountPerDeposit);
                    }
                } catch (Exception e) {
                    fail("Concurrent deposit encountered unexpected error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate cleanly");
        assertTrue(completed, "All concurrent tasks should complete within timeout");

        assertEquals(expectedFinalBalance, shared.getBalance(), 0.001,
                "Final balance after concurrent deposits must match exact expected total without lost updates");
    }

    /**
     * Test 4: Deadlock-safe atomic transfer between two accounts.
     */
    @Test
    @DisplayName("Safe transfer updates both source and destination balances atomically")
    public void testTransferBetweenAccounts() throws BankException {
        SavingsAccount from = new SavingsAccount(2001, "Sender", 3000.0, 500);
        CurrentAccount to = new CurrentAccount(2002, "Receiver", 1000.0, 2000);
        bankService.openAccount(from);
        bankService.openAccount(to);

        auditedBank.transfer("2001", "2002", 1200);

        assertEquals(1800.0, from.getBalance(), 0.001, "Source account should decrease by 1200");
        assertEquals(2200.0, to.getBalance(), 0.001, "Destination account should increase by 1200");
    }

    /**
     * Test 5: Generic Repository reflection extracts @Id key from entity hierarchy.
     */
    @Test
    @DisplayName("Generic Repository extracts @Id key via reflection and provides full CRUD")
    public void testGenericRepositoryReflection() {
        Repository<Account> customRepo = new Repository<>();
        SavingsAccount acc = new SavingsAccount(3001, "ReflectUser", 5500.0, 1000);

        // Save automatically extracts accountNumber (3001) via @Id reflection
        customRepo.save(acc);

        Account retrieved = customRepo.findById("3001");
        assertNotNull(retrieved, "Retrieved account should not be null");
        assertEquals("ReflectUser", retrieved.getOwnerName());

        ArrayList<Account> all = customRepo.findAll();
        assertEquals(1, all.size());

        boolean deleted = customRepo.delete("3001");
        assertTrue(deleted, "Delete should return true for existing account");
        assertNull(customRepo.findById("3001"), "Account should no longer exist after delete");
    }

    /**
     * Test 6: Bounded generic method average(N[] values).
     */
    @Test
    @DisplayName("Generic bounded method average() calculates correct mean across Number subtypes")
    public void testGenericBoundedAverage() {
        Double[] doubleValues = new Double[]{100.0, 200.0, 300.0, 400.0};
        assertEquals(250.0, Repository.average(doubleValues), 0.001);

        Integer[] intValues = new Integer[]{10, 20, 30, 40, 50};
        assertEquals(30.0, Repository.average(intValues), 0.001);

        Double[] empty = new Double[0];
        assertEquals(0.0, Repository.average(empty), 0.001);
    }

    /**
     * Test 7: Non-positive amounts throw InvalidAmountException.
     */
    @Test
    @DisplayName("Non-positive deposit or withdrawal throws InvalidAmountException")
    public void testInvalidAmountThrowsException() throws BankException {
        SavingsAccount acc = new SavingsAccount(4001, "ValidUser", 1000.0, 500);
        bankService.openAccount(acc);

        assertThrows(InvalidAmountException.class, () -> bankService.deposit("4001", -50));
        assertThrows(InvalidAmountException.class, () -> bankService.deposit("4001", 0));
        assertThrows(InvalidAmountException.class, () -> bankService.withdraw("4001", -100));
        assertThrows(InvalidAmountException.class, () -> bankService.withdraw("4001", 0));
    }

    /**
     * Test 8: Operating on non-existent account throws AccountNotFoundException.
     */
    @Test
    @DisplayName("Missing account ID throws AccountNotFoundException")
    public void testAccountNotFoundThrowsException() {
        assertThrows(AccountNotFoundException.class, () -> bankService.deposit("9999", 500));
        assertThrows(AccountNotFoundException.class, () -> bankService.withdraw("9999", 500));
        assertThrows(AccountNotFoundException.class, () -> bankService.statement("9999"));
    }

    /**
     * Standalone test runner that executes tests reflectively so the suite can be run
     * with or without the JUnit standalone runner.
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       MINIBANK JUNIT 5 TEST RUNNER (CAPSTONE)    ");
        System.out.println("=================================================");

        BankServiceTest suite = new BankServiceTest();
        Method[] methods = BankServiceTest.class.getDeclaredMethods();
        int passed = 0;
        int failed = 0;

        for (Method m : methods) {
            if (m.isAnnotationPresent(Test.class)) {
                String testName = m.getName();
                DisplayName dn = m.getAnnotation(DisplayName.class);
                if (dn != null) {
                    testName += " (" + dn.value() + ")";
                }

                try {
                    suite.setUp();
                    m.invoke(suite);
                    System.out.println("[PASS] " + testName);
                    passed++;
                } catch (Throwable t) {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    System.err.println("[FAIL] " + testName + " -> " + cause.getMessage());
                    failed++;
                }
            }
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Total Tests: " + (passed + failed) + " | Passed: " + passed + " | Failed: " + failed);
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
