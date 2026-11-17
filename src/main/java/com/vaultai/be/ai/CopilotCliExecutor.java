package com.vaultai.be.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.vaultai.be.exception.AgentUnavailableException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vaultai.be.config.AppProperties;

import lombok.extern.slf4j.Slf4j;

/** Runs GitHub Copilot CLI in programmatic mode: <cli-command> -p "<prompt>" -s */
@Slf4j
@Component
@Profile("!mock")
public class CopilotCliExecutor implements PromptExecutor {

    private static final boolean WINDOWS =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");

    private final AppProperties.Agent properties;
    private final Semaphore slots;
    /** Empty folder, so the CLI never sees the project's files. */
    private final Path workDir;

    public CopilotCliExecutor(AppProperties appProperties) throws IOException {
        this.properties = appProperties.agent();
        this.slots = new Semaphore(properties.maxConcurrent());
        this.workDir = Files.createTempDirectory("jarvis-copilot");

        String executable = properties.cliCommand().getFirst().toLowerCase(Locale.ROOT);
        if (WINDOWS && (executable.endsWith(".cmd") || executable.endsWith(".bat"))) {
            log.warn("app.agent.cli-command points to a .cmd/.bat file. Multi-line prompts will break; "
                    + "point it to node + the CLI's index.js or to copilot.exe instead.");
        }
        log.info("Copilot CLI executor ready: command={}, workDir={}", properties.cliCommand(), workDir);
    }

    @Override
    public CliResult execute(String prompt) {
        if (!acquireSlot()) {
            throw new AgentUnavailableException("Too many requests to the assistant right now");
        }

        Path stderrFile = null;
        long start = System.nanoTime();
        try {
            stderrFile = Files.createTempFile(workDir, "stderr-", ".log");

            List<String> command = new ArrayList<>(properties.cliCommand());
            command.add("-p");
            command.add(WINDOWS ? sanitizeForWindows(prompt) : prompt);
            command.add("-s");

            Process process = new ProcessBuilder(command)
                    .directory(workDir.toFile())
                    .redirectError(stderrFile.toFile())
                    .start();
            process.getOutputStream().close(); // the CLI gets nothing on stdin

            // read stdout on another thread, otherwise a full pipe buffer blocks the process
            CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));

            if (!process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new AgentUnavailableException(
                        "Copilot CLI did not answer within " + properties.timeout().toSeconds() + " seconds");
            }

            String output = stdout.get(5, TimeUnit.SECONDS);
            String errors = new String(Files.readAllBytes(stderrFile), StandardCharsets.UTF_8);
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            log.info("Copilot CLI finished in {} ms with exit code {}", durationMs, process.exitValue());
            return new CliResult(process.exitValue(), output, errors, durationMs);
        } catch (AgentUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AgentUnavailableException("The assistant call was interrupted", ex);
        } catch (IOException | ExecutionException | TimeoutException | UncheckedIOException ex) {
            throw new AgentUnavailableException("Could not run Copilot CLI: " + ex.getMessage(), ex);
        } finally {
            slots.release();
            deleteQuietly(stderrFile);
        }
    }

    private boolean acquireSlot() {
        try {
            return slots.tryAcquire(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * On Windows, Java does not escape double quotes inside arguments, so they would cut the prompt.
     * A trailing backslash would also escape the closing quote.
     */
    private static String sanitizeForWindows(String prompt) {
        String result = prompt.replace('"', '\'');
        while (result.endsWith("\\")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String readAll(InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            log.debug("Could not delete {}", file);
        }
    }
}