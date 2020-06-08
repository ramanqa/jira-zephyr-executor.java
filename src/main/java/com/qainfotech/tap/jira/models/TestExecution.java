package com.qainfotech.tap.jira.models;

import kong.unirest.json.JSONObject;

import java.util.Map;
import java.util.HashMap;

public class TestExecution {

    JSONObject data;

    public TestExecution(JSONObject testExecutionData){
        this.data = testExecutionData;
    }

    public String id(){
        return data.getJSONObject("execution").getString("id");
    }

    public String issueKey(){
        return data.getString("issueKey");
    }

    public String issueId(){
        return "" + data.getJSONObject("execution").getInt("issueId");
    }

    public String cycleId(){
        return data.getJSONObject("execution").getString("cycleId");
    }

    public String issueSummary(){
        return data.getString("issueSummary");
    }

    public String result(){
        return data.getJSONObject("execution").getJSONObject("status").getString("name");
    }
  
    public Map<String, String> testDetails(){
        Map<String, String> testDetails = new HashMap<>();
        return testDetails;
    }

    public String toString(){
        return data.toString();
    }
}
