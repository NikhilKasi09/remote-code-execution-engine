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

//	@Bean
//	public CommandLineRunner runTest(CodeExecutionService executionService) {
//		return args -> {
//			System.out.println("--- STARTING PHASE 1 TEST ---");
//
//			// The hardcoded Python script we want to test
//			String pythonCode =
//					"print('Attempting to allocate 1GB of RAM...')\n" +
//							"x = 'A' * (10**9)\n" +
//							"print('If you see this, the constraints failed!')";
//
//			// Trigger the engine
//			String result = executionService.executePython(pythonCode);
//
//			// Print the captured output from the Docker container
//			System.out.println("Execution Result:\n" + result);
//			System.out.println("--- PHASE 1 TEST COMPLETE ---");
//		};
//	}
}