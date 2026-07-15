package com.rce.execution_engine;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CodeExecutionService {

    public String executePython(String code) {
        try {
            // Create a temporary directory and script file on the host machine
            Path tempDir = Files.createTempDirectory("rce-");
            Path scriptPath = tempDir.resolve("script.py");
            Files.writeString(scriptPath, code);

            // Get the absolute path to map it into the Docker container
            Process process = getProcess(tempDir);

            // Capture the console output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for the Docker container to finish executing
            process.waitFor();

            return output.toString();

        } catch (Exception e) {
            return "Execution Error: " + e.getMessage();
        }
    }

    private static @NonNull Process getProcess(Path tempDir) throws IOException {
        String hostPath = tempDir.toAbsolutePath().toString();

        // Build the Docker command
        // docker run --rm -v /path/to/temp:/app python:3.9 python /app/script.py
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "run", "--rm",
                "-v", hostPath + ":/app",
                "python:3.9",
                "python", "/app/script.py"
        );

        // Redirect error stream to standard output so we can see Python errors
        processBuilder.redirectErrorStream(true);

        // Execute the command
        return processBuilder.start();
    }
}