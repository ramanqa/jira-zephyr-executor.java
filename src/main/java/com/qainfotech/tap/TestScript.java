package com.qainfotech.tap;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.qainfotech.tap.jira.JiraAPI;
import com.qainfotech.tap.jira.models.Issue;
import com.qainfotech.tap.jira.models.TestExecution;
import kong.unirest.json.JSONObject;

public class TestScript {

    String key;
    Map<String, String> report;
    Issue issue;
    TestExecution testExecution;

    public TestScript(TestExecution testExecution, Map<String, String> report){
        this.testExecution = testExecution;
        this.key = this.testExecution.issueKey();;
        this.report = report;
        this.issue = JiraAPI.getIssue(key);
    }

    public String key(){
        return this.key;
    }

    public String summary(){
        return this.issue.summary();
    }

    public String result(){
        String result = this.testExecution.result().toUpperCase(); 
        try{
            result = new JSONObject(this.report.get("status")).getString("name");
        }catch(Exception e){}
        return result;
    }

    public Map<String, String> report(){
        return this.report;
    }

    public List<Map<String, String>> coveredTests(){
        return this.issue.relatesToIssues();
    }
}
