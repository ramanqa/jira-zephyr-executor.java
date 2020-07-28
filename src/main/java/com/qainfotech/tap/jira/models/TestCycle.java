package com.qainfotech.tap.jira.models;

import kong.unirest.json.JSONObject;

import java.util.Map;
import java.util.HashMap;

public class TestCycle {

    JSONObject data;

    public TestCycle(JSONObject testCycleData){
        this.data = testCycleData; 
    }

    public String id(){
        return data.getString("id");
    }

    public String name(){
        return data.getString("name");
    }

    public Map<String, Integer> executionSummary(){
        Map<String, Integer> summary = new HashMap<>();
        summary.put("PASS", 0);
        summary.put("FAIL", 0);
        summary.put("UNEXECUTED", 0);
        summary.put("SKIP", 0);
        for(int i = 0; i < data.getJSONArray("executionSummaries").length(); i++){
            if(data.getJSONArray("executionSummaries").getJSONObject(i).getString("executionStatusName").equals("PASS")){
                Integer count = data.getJSONArray("executionSummaries").getJSONObject(i).getInt("count");
                summary.put("PASS", count);
            }
            if(data.getJSONArray("executionSummaries").getJSONObject(i).getString("executionStatusName").equals("FAIL")){
                Integer count = data.getJSONArray("executionSummaries").getJSONObject(i).getInt("count");
                summary.put("FAIL", count);              
            }
            if(data.getJSONArray("executionSummaries").getJSONObject(i).getString("executionStatusName").equals("UNEXECUTED")){
                Integer count = data.getJSONArray("executionSummaries").getJSONObject(i).getInt("count");
                summary.put("UNEXECUTED", count);              
            }
        }
        return summary;
    }

    public String toString(){
        return this.data.toString();
    }
}
