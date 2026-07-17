package com.rce.execution_engine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows frontend communication
public class CodeExecutionController {

    private final CodeExecutionService executionService;

    public CodeExecutionController(CodeExecutionService executionService){
        this.executionService = executionService;
    }

    @PostMapping("/execute") // Listens for incoming POST requests (client -> server) on localhost/api/execute
    public ResponseEntity<Map<String, String>> executeCode(@RequestBody ExecutionRequest request){

        // Extract the code from the incoming JSON
        String userCode = request.getCode();
        String language = request.getLanguage();

        // Execute based on language

        String output = executionService.executeCode(userCode, language);

        // Format back to JSON
        Map<String, String> response = new HashMap<>();
        response.put("output", output);

        return ResponseEntity.ok(response);


    }
}
