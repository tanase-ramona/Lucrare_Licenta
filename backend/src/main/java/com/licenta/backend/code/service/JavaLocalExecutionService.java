package com.licenta.backend.code.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class JavaLocalExecutionService {

    private static final int MAX_CODE_LENGTH = 20_000;
    private static final long COMPILE_TIMEOUT_SECONDS = 10;
    private static final long RUN_TIMEOUT_SECONDS = 5;

    public ExecutionResult run(String code, String inputData) {
        if (code == null || code.isBlank()) {
            return ExecutionResult.error("Codul este gol.", 0);
        }

        if (code.length() > MAX_CODE_LENGTH) {
            return ExecutionResult.error("Codul depaseste limita maxima de caractere.", 0);
        }

        Path tempDir = null;
        long start = System.currentTimeMillis();

        try {
            tempDir = Files.createTempDirectory("licenta-java-run-");
            Files.writeString(tempDir.resolve("Main.java"), code, StandardCharsets.UTF_8);

            ProcessResult compileResult = runProcess(
                    new ProcessBuilder(resolveJavaTool("javac"), "Main.java").directory(tempDir.toFile()),
                    null,
                    COMPILE_TIMEOUT_SECONDS
            );

            if (compileResult.timedOut) {
                return ExecutionResult.error("Compilarea a depasit limita de timp.", elapsed(start));
            }

            if (compileResult.exitCode != 0) {
                return ExecutionResult.error(cleanError(compileResult.stderr, compileResult.stdout), elapsed(start));
            }

            ProcessResult runResult = runProcess(
                    new ProcessBuilder(resolveJavaTool("java"), "Main").directory(tempDir.toFile()),
                    inputData,
                    RUN_TIMEOUT_SECONDS
            );

            if (runResult.timedOut) {
                return ExecutionResult.error("Executia a depasit limita de timp.", elapsed(start));
            }

            if (runResult.exitCode != 0) {
                return ExecutionResult.error(cleanError(runResult.stderr, runResult.stdout), elapsed(start));
            }

            return ExecutionResult.success(runResult.stdout, elapsed(start));
        } catch (IOException e) {
            return ExecutionResult.error(
                    "Nu pot porni runner-ul Java local. Verifica daca JDK este instalat si comenzile java/javac sunt in PATH.",
                    elapsed(start)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.error("Executia a fost intrerupta.", elapsed(start));
        } finally {
            cleanup(tempDir);
        }
    }

    private String resolveJavaTool(String toolName) {
        String executableName = isWindows() ? toolName + ".exe" : toolName;
        String javaHome = System.getenv("JAVA_HOME");

        if (javaHome != null && !javaHome.isBlank()) {
            Path fromJavaHome = Paths.get(javaHome, "bin", executableName);
            if (Files.isRegularFile(fromJavaHome)) {
                return fromJavaHome.toString();
            }
        }

        Path temurinDefault = Paths.get(
                "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.18.8-hotspot\\bin",
                executableName
        );
        if (Files.isRegularFile(temurinDefault)) {
            return temurinDefault.toString();
        }

        return toolName;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private ProcessResult runProcess(ProcessBuilder processBuilder, String inputData, long timeoutSeconds)
            throws IOException, InterruptedException {
        Process process = processBuilder.start();

        if (inputData != null) {
            process.getOutputStream().write(inputData.getBytes(StandardCharsets.UTF_8));
        }
        process.getOutputStream().close();

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(1, TimeUnit.SECONDS);
            return new ProcessResult("", "", -1, true);
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        return new ProcessResult(stdout, stderr, process.exitValue(), false);
    }

    private String cleanError(String stderr, String stdout) {
        String error = stderr != null && !stderr.isBlank() ? stderr : stdout;
        if (error == null || error.isBlank()) {
            return "Programul s-a oprit cu eroare, fara mesaj detaliat.";
        }
        return error.trim();
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private void cleanup(Path tempDir) {
        if (tempDir == null) {
            return;
        }

        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup for temporary runner files.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup for temporary runner files.
        }
    }

    private record ProcessResult(String stdout, String stderr, int exitCode, boolean timedOut) {
    }

    public static class ExecutionResult {
        public final String output;
        public final String error;
        public final boolean success;
        public final long executionTimeMs;

        private ExecutionResult(String output, String error, boolean success, long executionTimeMs) {
            this.output = output;
            this.error = error;
            this.success = success;
            this.executionTimeMs = executionTimeMs;
        }

        public static ExecutionResult success(String output, long executionTimeMs) {
            return new ExecutionResult(output, null, true, executionTimeMs);
        }

        public static ExecutionResult error(String error, long executionTimeMs) {
            return new ExecutionResult("", error, false, executionTimeMs);
        }
    }
}
