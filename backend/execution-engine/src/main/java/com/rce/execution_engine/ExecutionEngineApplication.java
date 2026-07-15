package com.rce.execution_engine;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ExecutionEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExecutionEngineApplication.class, args);
	}

	@Bean
	public CommandLineRunner runTest(CodeExecutionService executionService) {
		return args -> {
			System.out.println("--- STARTING PHASE 1 TEST ---");

			// The hardcoded Python script we want to test
			String pythonCode = "print('Hello from the Docker Sandbox in Cardiff!')\n" +
					"x = 10\n" +
					"y = 20\n" +
					"print(f'The sum is {x + y}')";

			// Trigger the engine
			String result = executionService.executePython(pythonCode);

			// Print the captured output from the Docker container
			System.out.println("Execution Result:\n" + result);
			System.out.println("--- PHASE 1 TEST COMPLETE ---");
		};
	}
}