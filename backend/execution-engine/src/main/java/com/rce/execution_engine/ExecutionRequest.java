package com.rce.execution_engine;

public class ExecutionRequest {
    private String language;
    private String code;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language){
        this.language = language;
    }

    public String getCode(){
        return code;
    }

    public void setCode(String code){
        this.code = code;
    }
}
