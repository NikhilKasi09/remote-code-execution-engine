package com.rce.execution_engine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * These tests launch real Docker containers, the same way the production code path does -
 * there is no mocking of the execution pipeline. Run with `mvn test -Dgroups=docker` to
 * select just this class, or `-DexcludedGroups=docker` to skip it on a machine without Docker.
 */
@Tag("docker")
class CodeExecutionServiceIntegrationTest {

    private final CodeExecutionService service = new CodeExecutionService();

    @BeforeAll
    static void requireDocker() {
        assumeTrue(isDockerAvailable(), "Docker is not available on this machine - skipping sandbox integration tests");
    }

    private static boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Test
    void runsPythonAndReturnsStdout() {
        String output = service.executeCode("print('hello from python')", "python");
        assertThat(output.trim()).isEqualTo("hello from python");
    }

    @Test
    void compilesAndRunsCpp() {
        String code = """
                #include <iostream>
                int main() {
                    std::cout << "hello from cpp" << std::endl;
                    return 0;
                }
                """;
        String output = service.executeCode(code, "cpp");
        assertThat(output.trim()).isEqualTo("hello from cpp");
    }

    @Test
    void reportsCompileErrorsForBrokenCpp() {
        String output = service.executeCode("int main() { this is not valid c++ }", "cpp");
        assertThat(output).containsIgnoringCase("error");
    }

    @Test
    void rejectsUnsupportedLanguagesWithoutTouchingDocker() {
        String output = service.executeCode("print(1)", "ruby");
        assertThat(output).isEqualTo("Error: Unsupported language selected.");
    }

    @Test
    void killsExecutionThatExceedsTheTimeout() {
        long start = System.currentTimeMillis();
        String output = service.executeCode("while True: pass", "python");
        long elapsed = System.currentTimeMillis() - start;

        assertThat(output).contains("timed out");
        // Should be killed close to the 5s language timeout, not left running indefinitely.
        assertThat(elapsed).isLessThan(15_000);
    }

    @Test
    void blocksNetworkAccessFromInsideTheSandbox() {
        // Validates --network=none actually works, rather than just being present in the code.
        String code = """
                import urllib.request
                try:
                    urllib.request.urlopen('http://example.com', timeout=3)
                    print('NETWORK REACHED')
                except Exception as e:
                    print('NETWORK BLOCKED:', e)
                """;
        String output = service.executeCode(code, "python");
        assertThat(output).doesNotContain("NETWORK REACHED");
    }

    @Test
    void truncatesOutputPastTheHundredKilobyteCap() {
        String code = "print('x' * 1000 * 200)"; // ~200KB in one line
        String output = service.executeCode(code, "python");
        assertThat(output).contains("[SYSTEM WARNING: Output truncated");
    }
}
