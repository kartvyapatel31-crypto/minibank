# MiniBank — Object-Oriented Banking System & Generic Repository Capstone

A comprehensive, production-grade Java application demonstrating modern Object-Oriented Programming principles, generic data-access layers with reflection, multithreading, concurrency control, deadlock prevention, Java NIO.2 file persistence, aspect-oriented auditing, and automated JUnit 5 testing.

---

## Architecture Overview

```
                                      +------------------------------------+
                                      |          MiniBank (CLI / UI)       |
                                      +-----------------+------------------+
                                                        |
                                                        v
                                      +------------------------------------+
                                      |   Audit Dynamic Proxy (@Audit)     |
                                      +-----------------+------------------+
                                                        |
                                                        v
                                      +------------------------------------+
                                      |           BankService              |
                                      |    (Implements BankOperations)     |
                                      +--------+--------+--------+---------+
                                               |        |        |
                    +--------------------------+        |        +--------------------------+
                    v                                   v                                   v
+-----------------------+           +-----------------------+           +-----------------------+
|  Repository<Account>  |           | Validation & Security |           | Concurrency & Batch   |
| (Reflection on @Id)   |           |  - AnnotationValidator|           |  - TransactionProcessor|
|  - save() / saveAll() |           |  - Validator (Regex)  |           |  - ConcurrentHashMap  |
|  - findById / findAll |           |  - Custom Exceptions  |           |  - Lock Ordering (Safe|
|  - delete() / average |           +-----------------------+           |    Transfers)         |
+-----------+-----------+                                               +-----------+-----------+
            |                                                                       |
            v                                                                       v
+-----------------------+                                               +-----------------------+
|  Domain Models        |                                               | Persistence & Reports |
|  - Account (Abstract) |                                               |  - StatePersister.ser |
|  - SavingsAccount     |                                               |  - TransactionLog.log |
|  - CurrentAccount     |                                               |  - ReportGenerator    |
|  - FixedDepositAccount|                                               |  - AuditLogger.log    |
+-----------------------+                                               +-----------------------+
```

---

## Progression of Practicals & Key Features

### Practical 1–6: Domain Model & OOP Fundamentals
- **Polymorphism & Abstraction**: Base abstract class [`Account`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/Account.java) with concrete subclasses:
  - [`SavingsAccount`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/SavingsAccount.java) with `minBalance` constraint and 4% interest.
  - [`CurrentAccount`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/CurrentAccount.java) with `overdraftLimit` support.
  - [`FixedDepositAccount`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/FixedDepositAccount.java) with fixed 7% interest and withdrawal locks.
- **Interfaces**: [`Transactable`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/Transactable.java), [`InterestBearing`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/InterestBearing.java), `Comparable<Account>`.
- **Modern Java Idioms**: Pattern matching for `switch` and `instanceof`, Records ([`Command`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/Command.java), [`BankInfo`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/BankInfo.java)), Enums ([`TransactionType`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/TransactionType.java)).

### Practical 7: Reflection & Custom Annotations
- Custom runtime annotations:
  - [`@Id`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/annotation/Id.java): Identifies entity primary key fields for generic storage.
  - [`@Positive`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/annotation/Positive.java): Enforces positive numeric constraints on account balances.
  - [`@MaxLength`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/annotation/MaxLength.java): Enforces string length constraints.
- [`AnnotationValidator`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/util/AnnotationValidator.java): Reflective runtime scanner that validates object fields across inheritance hierarchies.

### Practical 8: Exception Handling & Fault Tolerance
- Structured exception hierarchy extending [`BankException`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/exception/BankException.java):
  - [`InsufficientFundsException`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/exception/InsufficientFundsException.java) (with contextual `shortfall` reporting).
  - [`InvalidAmountException`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/exception/InvalidAmountException.java).
  - [`AccountNotFoundException`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/exception/AccountNotFoundException.java).
- Safe resource management using `try-with-resources` with [`BankTransactionSession`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/util/BankTransactionSession.java) (`AutoCloseable`).

### Practical 9–10: Concurrency, Thread Pools & Deadlock Prevention
- **Race Condition Proof**: Comparative benchmarks between unsynchronized operations (demonstrating lost updates) and synchronized monitors (guaranteeing thread safety).
- **Thread Pools**: [`TransactionProcessor`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/TransactionProcessor.java) leveraging `Executors.newFixedThreadPool(4)` for concurrent task execution.
- **Producer-Consumer Coordination**: [`TransactionBuffer`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/TransactionBuffer.java) utilizing synchronized monitors with `wait()` and `notify()`.
- **Deadlock Creation & Resolution**:
  - `Account.transferUnsafe()` creates circular-wait deadlocks with opposite lock acquisition orders.
  - `Account.transferSafe()` eliminates deadlocks using strict numeric lock-ordering (always locking lower `accountNumber` first).

### Practical 11–12: Persistence, Reporting & Collections
- **Object Serialization**: [`StatePersister`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/StatePersister.java) stores and reloads binary snapshots in `data/accounts.ser`.
- **NIO.2 Logging & Reporting**: [`TransactionLog`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/TransactionLog.java) appends transaction history; [`ReportGenerator`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/ReportGenerator.java) parses transaction logs to compute total deposits, withdrawals, and net change.
- **Collections Framework**: Sorting via `Comparable` and `Comparator`, thread-safe store with `ConcurrentHashMap`.

### Practical 13: Generic Data Access Layer (`Repository<T>`)
- **Generic Store**: [`Repository<T>`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/Repository.java) encapsulates a `HashMap<String, T>` with synchronized CRUD methods:
  - `T save(T entity)`
  - `T findById(String id)`
  - `ArrayList<T> findAll()`
  - `boolean delete(String id)`
- **Reflective Key Extraction**: In `save()`, the entity's class hierarchy is inspected to discover the `@Id` annotated field, reading its value as the primary map key.
- **Varargs Safe Batching**: `@SafeVarargs public final synchronized ArrayList<T> saveAll(T... items)`.
- **Bounded Generic Method**: `public static <N extends Number> double average(N[] values)` computes arithmetic averages over any number subtype (`Double`, `Integer`, `Long`, etc.).

### Practical 14: Capstone Assembly & BankService
- **Service Layer**: [`BankService`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/BankService.java) orchestrates `Repository<Account>`, `Validator`, and domain exceptions:
  - `openAccount`, `deposit`, `withdraw`, `transfer`, `statement`, `listAccounts`.
- **Reflective Auditing**: Custom [`@Audit`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/model/annotation/Audit.java) annotation and [`AuditLogger`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/src/service/AuditLogger.java) dynamic proxy that intercepts service calls, logging timestamp, action, arguments, and status to console and `logs/audit.log`.
- **Connected Practical 1 Menu**: Fully integrated console UI with 9 options, loading accounts from `data/accounts.ser` at startup and saving them on exit.
- **JUnit 5 Suite**: [`BankServiceTest`](file:///d:/JAVA_OOP/JAVA_practicals/mini_project/test/BankServiceTest.java) verifying deposits, withdrawal limit exceptions, concurrent deposit thread-safety, safe transfers, and generic repository reflection.

---

## Project Structure

```
mini_project/
├── data/
│   └── accounts.ser                       # Serialized accounts binary store
├── lib/
│   └── junit-platform-console-standalone-1.10.2.jar  # JUnit 5 standalone runtime
├── logs/
│   ├── audit.log                          # Reflective audit logs
│   └── daily_transactions.log             # Transaction execution log
├── reports/
│   └── daily_summary_report.txt           # Generated summary report
├── out/                                   # Compiled bytecode (.class)
├── src/
│   ├── exception/
│   │   ├── AccountNotFoundException.java
│   │   ├── BankException.java
│   │   ├── InsufficientFundsException.java
│   │   └── InvalidAmountException.java
│   ├── model/
│   │   ├── annotation/
│   │   │   ├── Audit.java                 # @Audit annotation (Practical 14)
│   │   │   ├── Id.java                    # @Id annotation (Practical 7 & 13)
│   │   │   ├── MaxLength.java
│   │   │   └── Positive.java
│   │   ├── Account.java                   # Abstract base account
│   │   ├── BankInfo.java                  # Record
│   │   ├── Command.java                   # Record
│   │   ├── CurrentAccount.java
│   │   ├── Customer.java
│   │   ├── FixedDepositAccount.java
│   │   ├── InterestBearing.java
│   │   ├── MenuOption.java
│   │   ├── Premium.java
│   │   ├── SavingsAccount.java
│   │   ├── Transactable.java
│   │   └── TransactionType.java
│   ├── service/
│   │   ├── AccountStore.java              # Collection store (Practical 12)
│   │   ├── AccountWorker.java             # Thread worker (Practical 9)
│   │   ├── AuditLogger.java               # Reflective audit proxy (Practical 14)
│   │   ├── BankOperations.java            # Service contract interface
│   │   ├── BankService.java               # Capstone business service (Practical 14)
│   │   ├── CommandParser.java
│   │   ├── ReportGenerator.java           # Reporting engine (Practical 11)
│   │   ├── Repository.java                # Generic repository with reflection (Practical 13)
│   │   ├── StatePersister.java            # Object serialization (Practical 11)
│   │   ├── StatementFormatter.java
│   │   ├── TransactionBuffer.java         # Producer-consumer buffer (Practical 10)
│   │   ├── TransactionLog.java            # Log writer (Practical 11)
│   │   └── TransactionProcessor.java      # Thread pool engine (Practical 10)
│   ├── util/
│   │   ├── AnnotationValidator.java       # Reflective validator (Practical 7)
│   │   ├── BankTransactionSession.java    # AutoCloseable session (Practical 8)
│   │   ├── Validator.java                 # Contact & regex validator
│   │   └── WithdrawRule.java              # Functional interface
│   └── MiniBank.java                      # Main runnable entry point & interactive menu
├── test/
│   └── BankServiceTest.java               # JUnit 5 test suite (Practical 14)
├── manifest.txt                           # JAR manifest (Main-Class: MiniBank)
├── MiniBank.jar                           # Final executable runnable JAR
└── README.md                              # Complete capstone documentation
```

---

## Build, Test & Run Instructions

### 1. Compile the Application
From the workspace root (`d:\JAVA_OOP\JAVA_practicals`):
```powershell
javac -d mini_project/out (Get-ChildItem -Path mini_project/src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

### 2. Compile and Run the Test Suite (JUnit 5)
Compile tests:
```powershell
javac -cp "mini_project/out;mini_project/lib/junit-platform-console-standalone-1.10.2.jar" -d mini_project/out mini_project/test/BankServiceTest.java
```

Execute tests via JUnit 5 Platform Console Launcher:
```powershell
java -jar mini_project/lib/junit-platform-console-standalone-1.10.2.jar execute -cp mini_project/out -c test.BankServiceTest
```

Or execute via the standalone test runner:
```powershell
java -cp "mini_project/out;mini_project/lib/junit-platform-console-standalone-1.10.2.jar" test.BankServiceTest
```

#### Test Suite Coverage:
| Test Method | Category | Verification Goal |
|---|---|---|
| `testDepositIncreasesBalance` | Core Operations | Verifies balance increases by deposited amount and reflects in repository |
| `testWithdrawalOverLimitThrowsException` | Fault Tolerance | Asserts `InsufficientFundsException` thrown with positive shortfall |
| `testConcurrentDepositsKeepBalanceCorrect` | Concurrency | 10 threads × 100 deposits = 1,000 operations without race conditions |
| `testTransferBetweenAccounts` | Deadlock Safety | Verifies safe transfer updates both balances atomically |
| `testGenericRepositoryReflection` | Generic DAO | Verifies reflection discovers `@Id`, saves, finds, and deletes entities |
| `testGenericBoundedAverage` | Generics | Verifies `<N extends Number>` average calculation across number arrays |
| `testInvalidAmountThrowsException` | Validation | Non-positive deposit/withdrawal throws `InvalidAmountException` |
| `testAccountNotFoundThrowsException` | Validation | Missing account lookup throws `AccountNotFoundException` |

### 3. Build the Runnable Executable JAR
```powershell
cd mini_project
jar cvfm MiniBank.jar manifest.txt -C out .
cd ..
```

### 4. Run the MiniBank Application
Run the packaged JAR:
```powershell
java -jar mini_project/MiniBank.jar
```

Or run via class path:
```powershell
java -cp mini_project/out MiniBank
```

---

## Interactive Menu (Practical 14)

Upon running `MiniBank`, the full progression of Milestone 1, 2, and 3 labs runs as self-verifying test demonstrations, followed by the Interactive Capstone Menu:

```text
=================================
1. Open Account
2. Deposit
3. Withdraw
4. Transfer
5. Account Statement
6. List All Accounts
7. Safe Batch Processing (Practical 10 & 12)
8. Generate Daily Report (Practical 11)
9. Exit
Enter choice (1-9):
```

- **Open Account (1)**: Choose account type (Savings, Current, Fixed Deposit), enter details, and optional mobile/email validated via regex and annotation rules.
- **Deposit (2)**: Safely deposit funds into any account.
- **Withdraw (3)**: Withdraw funds subject to account-specific constraints (`minBalance` or `overdraftLimit`).
- **Transfer (4)**: Deadlock-safe atomic transfer between any two accounts.
- **Account Statement (5)**: Formatted statement showing account number, owner, and balance.
- **List All Accounts (6)**: Displays all accounts in the generic repository.
- **Safe Batch Processing (7)**: Processes concurrent transactions with `TransactionProcessor` and `ConcurrentHashMap`.
- **Generate Daily Report (8)**: Parses logs and prints aggregate deposits, withdrawals, and net change.
- **Exit (9)**: Persists all registered accounts to `data/accounts.ser` and gracefully shuts down.

