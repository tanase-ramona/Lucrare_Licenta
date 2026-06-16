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
public class CLocalExecutionService {

    private static final int MAX_CODE_LENGTH = 20_000;
    private static final long COMPILE_TIMEOUT_SECONDS = 10;
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
            tempDir = Files.createTempDirectory("licenta-c-run-");
            Files.writeString(tempDir.resolve("main.c"), code, StandardCharsets.UTF_8);

            String executableName = isWindows() ? "main.exe" : "main";
            ProcessResult compileResult = runProcess(
                    withCompilerPath(new ProcessBuilder(resolveCompiler(), "main.c", "-O2", "-std=c11", "-o", executableName)
                            .directory(tempDir.toFile())),
                    null,
                    COMPILE_TIMEOUT_SECONDS
            );

            if (compileResult.timedOut) {
                return JavaLocalExecutionService.ExecutionResult.error("Compilarea a depasit limita de timp.", elapsed(start));
            }

            if (compileResult.exitCode != 0) {
                return JavaLocalExecutionService.ExecutionResult.error(cleanError(compileResult.stderr, compileResult.stdout), elapsed(start));
            }

            ProcessResult runResult = runProcess(
                    withCompilerPath(new ProcessBuilder(executablePath(tempDir, executableName)).directory(tempDir.toFile())),
                    inputData,
                    RUN_TIMEOUT_SECONDS
            );

            if (runResult.timedOut) {
                return JavaLocalExecutionService.ExecutionResult.error("Executia a depasit limita de timp.", elapsed(start));
            }

            if (runResult.exitCode != 0) {
                return JavaLocalExecutionService.ExecutionResult.error(cleanError(runResult.stderr, runResult.stdout), elapsed(start));
            }

            return JavaLocalExecutionService.ExecutionResult.success(runResult.stdout, elapsed(start));
        } catch (IOException e) {
            return JavaLocalExecutionService.ExecutionResult.error(
                    "Nu pot porni runner-ul C local. Verifica daca gcc este instalat si disponibil in PATH.",
                    elapsed(start)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JavaLocalExecutionService.ExecutionResult.error("Executia a fost intrerupta.", elapsed(start));
        } finally {
            cleanup(tempDir);
        }
    }

    private String resolveCompiler() {
        Path msysGcc = Paths.get("C:\\msys64\\ucrt64\\bin\\gcc.exe");
        if (Files.isRegularFile(msysGcc)) {
            return msysGcc.toString();
        }
        return "gcc";
    }

    private ProcessBuilder withCompilerPath(ProcessBuilder processBuilder) {
        if (isWindows()) {
            String currentPath = processBuilder.environment().getOrDefault("PATH", "");
            String msysBin = "C:\\msys64\\ucrt64\\bin";
            if (!currentPath.toLowerCase().contains(msysBin.toLowerCase())) {
                processBuilder.environment().put("PATH", msysBin + ";" + currentPath);
            }
        }
        return processBuilder;
    }

    private String executablePath(Path tempDir, String executableName) {
        return isWindows() ? tempDir.resolve(executableName).toString() : "./" + executableName;
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
