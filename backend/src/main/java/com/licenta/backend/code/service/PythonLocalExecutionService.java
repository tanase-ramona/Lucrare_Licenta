package com.licenta.backend.code.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class PythonLocalExecutionService {

    private static final int MAX_CODE_LENGTH = 20_000;
    private static final long RUN_TIMEOUT_SECONDS = 5;

    public JavaLocalExecutionService.ExecutionResult run(String code, String inputData) {
        if (code == null || code.isBlank()) {
            return JavaLocalExecutionService.ExecutionResult.error("Codul este gol.", 0);
        }

        if (code.length() > MAX_CODE_LENGTH) {
            return JavaLocalExecutionService.ExecutionResult.error("Codul depaseste limita maxima de caractere.", 0);
        }

        Path tempDir = null;
        long start = System.currentTimeMillis();

        try {
            tempDir = Files.createTempDirectory("licenta-python-run-");
            Files.writeString(tempDir.resolve("solution.py"), code, StandardCharsets.UTF_8);

            ProcessResult runResult = null;
            IOException lastStartError = null;
            for (String command : pythonCommands()) {
                try {
                    runResult = runProcess(
                            new ProcessBuilder(command, "solution.py").directory(tempDir.toFile()),
                            inputData,
                            RUN_TIMEOUT_SECONDS
                    );
                    break;
                } catch (IOException e) {
                    lastStartError = e;
                }
            }

            if (runResult == null) {
                throw lastStartError != null ? lastStartError : new IOException("Python runner unavailable");
            }

            if (runResult.timedOut) {
                return JavaLocalExecutionService.ExecutionResult.error("Executia a depasit limita de timp.", elapsed(start));
            }

            if (runResult.exitCode != 0) {
                return JavaLocalExecutionService.ExecutionResult.error(cleanError(runResult.stderr, runResult.stdout), elapsed(start));
            }

            return JavaLocalExecutionService.ExecutionResult.success(runResult.stdout, elapsed(start));
        } catch (IOException e) {
            return JavaLocalExecutionService.ExecutionResult.error(
                    "Nu pot porni runner-ul Python local. Verifica daca Python este instalat si disponibil in PATH.",
                    elapsed(start)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JavaLocalExecutionService.ExecutionResult.error("Executia a fost intrerupta.", elapsed(start));
        } finally {
            cleanup(tempDir);
        }
    }

    private List<String> pythonCommands() {
        return isWindows() ? List.of("py", "python", "python3") : List.of("python3", "python");
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
        return error == null || error.isBlank()
                ? "Programul s-a oprit cu eroare, fara mesaj detaliat."
                : error.trim();
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
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
                            // Best-effort cleanup.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }

    private record ProcessResult(String stdout, String stderr, int exitCode, boolean timedOut) {
    }
}
