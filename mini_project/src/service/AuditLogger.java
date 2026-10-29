package service;

import model.annotation.Audit;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Reflective audit logger for service method invocations.
 * Practical 14 Optional Requirement 5:
 * Uses reflection to inspect the @Audit annotation on methods, logging details
 * to console and a persistent audit log file.
 */
public class AuditLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Path AUDIT_LOG_FILE = resolveAuditLogPath();

    private static Path resolveAuditLogPath() {
        Path base = Files.exists(Paths.get("mini_project")) ? Paths.get("mini_project") : Paths.get(".");
        return base.resolve("logs").resolve("audit.log");
    }

    /**
     * Inspects a method for @Audit and records invocation metadata.
     *
     * @param method the method invoked
     * @param args arguments passed to the method
     * @param result the returned result (if any)
     * @param error throwable if method threw an exception
     */
    public static void record(Method method, Object[] args, Object result, Throwable error) {
        if (method == null) {
            return;
        }

        Audit audit = method.getAnnotation(Audit.class);
        if (audit == null) {
            return;
        }

        String action = audit.action().isEmpty() ? method.getName().toUpperCase() : audit.action();
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String argsString = (args == null || args.length == 0) ? "none" : Arrays.toString(args);
        String status = (error == null) ? "SUCCESS" : "FAILED (" + error.getMessage() + ")";

        String logMessage = String.format("[AUDIT %s] ACTION=%s | METHOD=%s | ARGS=%s | STATUS=%s",
                timestamp, action, method.getName(), argsString, status);

        System.out.println(logMessage);

        try {
            TransactionLog.append(AUDIT_LOG_FILE, logMessage);
        } catch (IOException e) {
            System.err.println("Failed to write to audit log file: " + e.getMessage());
        }
    }

    /**
     * Creates a dynamic proxy that transparently intercepts method calls and logs
     * them via reflection if annotated with @Audit.
     *
     * @param interfaceClass interface type to proxy
     * @param target target implementation instance
     * @param <I> interface type
     * @return audited proxy instance
     */
    @SuppressWarnings("unchecked")
    public static <I> I createAuditedProxy(Class<I> interfaceClass, I target) {
        return (I) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Method targetMethod;
                        try {
                            targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
                        } catch (NoSuchMethodException e) {
                            targetMethod = method;
                        }

                        Object result = null;
                        Throwable error = null;
                        try {
                            result = targetMethod.invoke(target, args);
                            return result;
                        } catch (Throwable t) {
                            error = (t.getCause() != null) ? t.getCause() : t;
                            throw error;
                        } finally {
                            record(targetMethod, args, result, error);
                        }
                    }
                }
        );
    }
}

