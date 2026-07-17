package com.rce.execution_engine;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
public class CodeExecutionService {

    public String executeCode(String code, String language) {

        // Define configuration variables
        String fileName;
        String dockerImage;
        String shellCommand;

        // Set the configuration based on the requested language
        switch (language.toLowerCase()) {
            case "python":
                fileName = "script.py";
                dockerImage = "python:3.9";
                shellCommand = "python script.py";
                break;
            case "cpp":
                fileName = "main.cpp";
                dockerImage = "gcc:latest";
                // Chains the compilation and execution commands together
                shellCommand = "g++ main.cpp -o main && ./main";
                break;
            default:
                return "Error: Unsupported language selected.";
        }

        Path tempDir = null;

        try {
            // Create a temporary directory on the host machine
            tempDir = Files.createTempDirectory("rce-" + language + "-");

            // Resolve the correct file name dynamically (script.py or main.cpp)
            Path sourcePath = tempDir.resolve(fileName);

            // Safely write the code to the temp file
            Files.writeString(sourcePath, code);

            String containerName = "rce-env" + UUID.randomUUID().toString();

            // Get the absolute path to map it into the Docker container dynamically
            Process process = getProcess(tempDir, dockerImage, shellCommand, containerName);

            // Enforce a 5-second timeout
            boolean finishedInTime = process.waitFor(5, TimeUnit.SECONDS);

            if (!finishedInTime){
                // Kill the process if it exceeds the time limit
                process.destroyForcibly();

                // Safely send a direct kill order
                try {
                    new ProcessBuilder("docker", "rm", "-f", containerName)
                            .start()
                            .waitFor();
                } catch (Exception e) {
                    System.err.println("Failed to manually kill docker container: " + containerName);
                }

                return "Error: Execution timed out. Code exceeded the 5-second limit.";
            }

            // If it finished on time read the file created
            Path outputPath = tempDir.resolve("output.txt");
            if (Files.exists(outputPath)) {
                long fileSize = Files.size(outputPath);
                long MAX_OUTPUT_SIZE = 100 * 1024; // Hard cap at 100 KB

                if (fileSize > MAX_OUTPUT_SIZE) {
                    // Truncate the file reading to prevent Java Heap OOM
                    try (java.io.InputStream in = Files.newInputStream(outputPath)) {
                        byte[] buffer = new byte[(int) MAX_OUTPUT_SIZE];
                        int bytesRead = in.read(buffer);
                        String truncatedOutput = new String(buffer, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8);
                        return truncatedOutput + "\n\n[SYSTEM WARNING: Output truncated. Exceeded 100KB limit.]";
                    }
                } else {
                    // Safe to read entirely
                    return Files.readString(outputPath);
                }
            }

            return "";

        } catch (Exception e) {
            return "Execution Error: " + e.getMessage();
        } finally {
            // Delete the temporary folder after execution to prevent server storage leaks
            if (tempDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(tempDir);
                } catch (IOException e) {
                    System.err.println("Failed to delete temp directory: " + tempDir);
                }
            }
        }
    }

    private static @NonNull Process getProcess(Path tempDir, String dockerImage, String shellCommand, String containerName) throws IOException {
        String hostPath = tempDir.toAbsolutePath().toString();

        // Build the Docker command
        // docker run --rm --memory=256m --cpus=0.5 --network="none" -v /path/to/temp:/app python:3.9 python /app/script.py
        // docker run --rm --memory=256m --cpus=0.5 --network="none" -v /path/to/temp:/app -w /app gcc:latest sh -c "g++ main.cpp -o main && ./main"
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "run",
                "--rm",                  // Removes the dead container instantly
                "--name", containerName, // Assign a name to the container for the kill order
                "--memory=256m",         // No more than 256 MB of RAM
                "--cpus=0.5",            // Limits the CPU to use no more than half a single core
                "--network=none",        // Isolates network
                "--pids-limit=64",       // Prevents PID exhaustion (spawning many processes)
                "--read-only",           // Locks the container
                "--user", "1000:1000",   // Drops root privileges (uses as generic guest)
                "-v", hostPath + ":/app",
                "-w", "/app",
                dockerImage,
                "sh", "-c",
                shellCommand
        );

        // Redirect error stream to standard output so we can see compilation/runtime errors
        processBuilder.redirectErrorStream(true);

        // Write terminal output directly to a file
        File outputFile = tempDir.resolve("output.txt").toFile();
        processBuilder.redirectOutput(outputFile);

        // Execute the command
        return processBuilder.start();
    }
}