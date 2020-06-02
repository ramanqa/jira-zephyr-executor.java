package com.qainfotech.tap.jira.models;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import kong.unirest.json.JSONObject;
import kong.unirest.json.JSONArray;

public class Issue {

    JSONObject data;

    public Issue(JSONObject data){
        this.data = data;
    }

    public String toString(){
        return this.data.toString();
    }

    public String key(){
        return this.data.getString("key");
    }

    public String id(){
        return this.data.getString("id");
    }

    public List<String> labels(){
        JSONArray jLabels = this.data.getJSONObject("fields").getJSONArray("labels");
        List<String> labels = new ArrayList<>();
        for(int index=0; index<jLabels.length(); index++){
            labels.add(jLabels.getString(index)); 
        }
        return labels;
    }

    public Map<String, String> taid(){
        Map<String, String> testInfo = new HashMap<>();
        for(String label:labels()){
            if(label.startsWith("@taid=")){
                String testClass = label.split("=")[1].split("#")[0];
                String testMethod = label.split("=")[1].split("#")[1];
                String testName = key();
                testInfo.put("testName", testName);
                testInfo.put("testClass", testClass);
                testInfo.put("testMethod", testMethod);
                return testInfo;
            }
        }
        return testInfo;
    }
}
