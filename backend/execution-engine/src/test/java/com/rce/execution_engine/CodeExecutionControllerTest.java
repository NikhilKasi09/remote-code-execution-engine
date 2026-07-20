package com.rce.execution_engine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CodeExecutionController.class)
class CodeExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodeExecutionService codeExecutionService;

    // RateLimitFilter is picked up as a web-layer bean by @WebMvcTest, so its
    // RateLimiterService dependency needs a stand-in even though this test isn't about rate limiting.
    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    void returnsTheServiceOutputAsJson() throws Exception {
        when(rateLimiterService.tryAcquire(anyString())).thenReturn(true);
        when(codeExecutionService.executeCode(any(), any())).thenReturn("hello\n");

        mockMvc.perform(post("/api/execute")
                        .contentType("application/json")
                        .content("{\"language\":\"python\",\"code\":\"print('hello')\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.output").value("hello\n"));
    }

    @Test
    void passesLanguageAndCodeStraightThroughToTheService() throws Exception {
        when(rateLimiterService.tryAcquire(anyString())).thenReturn(true);
        when(codeExecutionService.executeCode(any(), any())).thenReturn("");

        mockMvc.perform(post("/api/execute")
                        .contentType("application/json")
                        .content("{\"language\":\"cpp\",\"code\":\"int main(){}\"}"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(codeExecutionService).executeCode("int main(){}", "cpp");
    }

    @Test
    void rejectsRequestsOnceTheClientIsRateLimited() throws Exception {
        when(rateLimiterService.tryAcquire(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/execute")
                        .contentType("application/json")
                        .content("{\"language\":\"python\",\"code\":\"print(1)\"}"))
                .andExpect(status().is(429));

        org.mockito.Mockito.verifyNoInteractions(codeExecutionService);
    }
}
