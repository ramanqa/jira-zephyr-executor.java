package com.qainfotech.tap.jira.models;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
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

    public String summary(){
        return this.data.getJSONObject("fields").getString("summary");
    }

    public String status(){
        return this.data.getJSONObject("fields").getJSONObject("status").getString("name");
    }

    public Date statusCompletedDate(){
        try{
            JSONArray histories = this.data.getJSONObject("changelog").getJSONArray("histories");
            for(int index=0; index<histories.length(); index++){
                JSONObject history = histories.getJSONObject(index);
                if(history.getJSONArray("items").getJSONObject(0).get("toString")==null){
                    continue;
                }
                if(history.getJSONArray("items").getJSONObject(0).getString("toString").toString().equals("Completed")){
                    SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
                    return formatter.parse(history.getString("created").split("T")[0]);
                }
            }
        }catch(Exception e){}
        return null;
    }

    public Date statusChangeDate(){
        try{
            String statusChangeDate = this.data.getJSONObject("fields").getString("statuscategorychangedate");
            statusChangeDate = statusChangeDate.split("T")[0];
            SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
            return formatter.parse(statusChangeDate);
        }catch(Exception e){}
        return null;
    }

    public List<Map<String, String>> relatesToIssues(){
        List<Map<String, String>> relatedIssues = new ArrayList<>();
        if(!this.data.getJSONObject("fields").has("issuelinks")){
            return relatedIssues;
        }
        JSONArray issueLinks = this.data.getJSONObject("fields").getJSONArray("issuelinks");
        for(int index=0; index<issueLinks.length(); index++){
            if(issueLinks.getJSONObject(index).getJSONObject("type").getString("inward").equals("relates to")){
                if(issueLinks.getJSONObject(index).has("inwardIssue")){
                    Map<String, String> relatedIssue = new HashMap<>();
                    relatedIssue.put("key", issueLinks.getJSONObject(index).getJSONObject("inwardIssue").getString("key"));
                    relatedIssue.put("summary", issueLinks.getJSONObject(index).getJSONObject("inwardIssue").getJSONObject("fields").getString("summary"));
                    relatedIssues.add(relatedIssue);
                }
                if(issueLinks.getJSONObject(index).has("outwardIssue")){
                    Map<String, String> relatedIssue = new HashMap<>();
                    relatedIssue.put("key", issueLinks.getJSONObject(index).getJSONObject("outwardIssue").getString("key"));
                    relatedIssue.put("summary", issueLinks.getJSONObject(index).getJSONObject("outwardIssue").getJSONObject("fields").getString("summary"));
                    relatedIssues.add(relatedIssue);
                }
            }
        }
        return relatedIssues;
    }

    public List<String> components(){
        List<String> components = new ArrayList<>();

        JSONArray componentsArray = this.data.getJSONObject("fields").getJSONArray("components");
        for(int i = 0; i < componentsArray.length(); i++){
            components.add(componentsArray.getJSONObject(i).getString("name"));
        }
        return components;
    }

    public String assignee(){
        try{
            return this.data.getJSONObject("fields").getJSONObject("assignee").getString("displayName");
        }catch(Exception e){
            return "";
        }
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
        testInfo.put("testName", null);
        testInfo.put("testClass", null);
        testInfo.put("testMethod", null);
        for(String label:labels()){
            if(label.startsWith("@taid=")){
                try{
                String testClass = label.split("=")[1].split("#")[0];
                String testMethod = label.split("=")[1].split("#")[1];
                String testName = key();
                testInfo.put("testName", testName);
                testInfo.put("testClass", testClass);
                testInfo.put("testMethod", testMethod);
                return testInfo;
                }catch(Exception e){
                    System.out.println("==== FAIL to get test class mapping " + key());
                }
            }
        }
        return testInfo;
    }
}
