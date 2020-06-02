package com.qainfotech.tap.jira.models;

import kong.unirest.json.JSONObject;

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

    public String toString(){
        return data.toString();
    }
}
