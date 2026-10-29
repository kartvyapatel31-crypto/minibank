package service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {

    public static String generateReport(Path logsFolder, Path reportFile) throws IOException {
        if (!Files.exists(logsFolder)) {
            Files.createDirectories(logsFolder);
        }

        long totalDeposits = 0L;
        long totalWithdrawals = 0L;
        List<String> lines = new ArrayList<>();

        lines.add("--- Transaction Logs Summary Report ---");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsFolder)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                lines.add("File: " + file.getFileName() + " | Size: " + attrs.size() + " bytes | Modified: " + attrs.lastModifiedTime());

                try (BufferedReader reader = Files.newBufferedReader(file)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            String type = parts[0].toUpperCase();
                            try {
                                long amount = Long.parseLong(parts[parts.length - 1]);
                                if (type.contains("DEPOSIT")) {
                                    totalDeposits += amount;
                                } else if (type.contains("WITHDRAW")) {
                                    totalWithdrawals += amount;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }

        lines.add("--------------------------------------");
        lines.add("Total Deposits: " + totalDeposits);
        lines.add("Total Withdrawals: " + totalWithdrawals);
        lines.add("Net Balance Change: " + (totalDeposits - totalWithdrawals));

        if (reportFile.getParent() != null && !Files.exists(reportFile.getParent())) {
            Files.createDirectories(reportFile.getParent());
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {
            for (String l : lines) {
                writer.write(l);
                writer.newLine();
                sb.append(l).append(System.lineSeparator());
            }
        }

        return sb.toString();
    }
}
