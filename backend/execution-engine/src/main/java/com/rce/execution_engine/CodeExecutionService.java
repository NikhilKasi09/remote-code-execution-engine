package com.rce.execution_engine;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

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

            // Enforce a 5-second timeout
            boolean finishedInTime = process.waitFor(5, TimeUnit.SECONDS);

            if (!finishedInTime){
                // Kill the process if it exceeds the time limit
                process.destroyForcibly();
                return "Error: Execution timed out. Code exceeded the 5-second limit.";
            }

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
                "docker", "run",
                "--rm", // Removes the dead container instantly
                "--memory=256m", // No more than 256 MB of RAM
                "--cpus=0.5", // Limits the CPU to use no more than half a single core
                "--network=none", // Isolates network
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