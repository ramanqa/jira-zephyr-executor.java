package com.qainfotech.tap.jira.models;

import org.json.JSONObject;

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
}
