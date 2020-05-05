package com.qainfotech.tap.jira;

import com.qainfotech.tap.ConfigReader;
import com.qainfotech.tap.jira.models.*;
import java.util.Map;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.HttpResponse;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.net.URISyntaxException;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Base64;
import java.net.URLEncoder;

public class JiraAPI {


    public static JSONObject getGeneralInformation() throws URISyntaxException, UnirestException, IOException{
        Map<String, String> jwtResponse  = GenerateJwt.jwt("GET", "/public/rest/api/1.0/config/generalinformation");
        JSONObject response = Unirest.get(jwtResponse.get("url"))
            .header("Authorization", jwtResponse.get("jwt"))
            .header("zapiAccessKey", ConfigReader.get("zephyr.accessKey"))
            .asJson().getBody().getObject();
        return response;
    }

    public static String getProjectNameById(String projectId) throws UnirestException, IOException{
        Base64.Encoder encoder = Base64.getEncoder();
        String signature = encoder.encodeToString(((String)ConfigReader.get("jira.userId") + ":" + ConfigReader.get("jira.apiKey")).getBytes());
        String url = ConfigReader.get("jira.baseUrl") + "/rest/api/3/project/" + projectId;
        JSONObject response = Unirest.get(url)
            .header("Authorization", "Basic " + signature)
            .asJson().getBody().getObject();
        return response.getString("name");
    }

    public static List<String> getIssuesByProjectIdAndLabel(String projectId, String label) throws UnirestException, IOException{
        String projectName = getProjectNameById(projectId);
        String jql = URLEncoder.encode("project=\""+projectName+"\" and labels=\""+label+"\"", "UTF-8");
      
        JSONObject response = getIssuesByJqlStartingAt(jql, 0);
        Integer maxResults = response.getInt("maxResults");
        Integer total = response.getInt("total");
        List<String> issues = new ArrayList<>();

        for(int index=0; index<response.getJSONArray("issues").length(); index++){
            String issueKey = response.getJSONArray("issues").getJSONObject(index).getString("key");
            issues.add(issueKey);
        }

        for(int iteration=1; iteration<=Math.floor(total/maxResults); iteration++){
            response = getIssuesByJqlStartingAt(jql, iteration*maxResults);
            for(int index=0;index<response.getJSONArray("issues").length(); index++){
                String issueKey = response.getJSONArray("issues").getJSONObject(index).getString("key");
                issues.add(issueKey);
            }
        }

        return issues;
    }

    public static String createTestCycle(String projectId, String versionId, String testCycleName) throws UnirestException, URISyntaxException, IOException{
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("name", testCycleName);
        bodyJson.put("projectId", projectId );
        bodyJson.put("versionId", versionId);
        Map<String, String> jwtResponse  = GenerateJwt.jwt("POST", "/public/rest/api/1.0/cycle");
        JSONObject response = Unirest.post(jwtResponse.get("url"))
            .header("Authorization", jwtResponse.get("jwt"))
            .header("zapiAccessKey", ConfigReader.get("zephyr.accessKey"))
            .header("Content-Type", "application/json")
            .body(bodyJson.toString())
            .asJson().getBody().getObject();

        return response.getString("id");
    }
    
    public static void addTestsToTestCycle(String projectId, String versionId, String cycleId, List<String> testIssues) throws URISyntaxException, UnirestException, IOException {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("method", "1" );
        bodyJson.put("issues", testIssues.toArray());
        bodyJson.put("projectId", projectId );
        bodyJson.put("versionId", versionId);

        Map<String, String> jwtResponse  = GenerateJwt.jwt("POST", "/public/rest/api/1.0/executions/add/cycle/" + cycleId);
        HttpResponse<String> response = Unirest.post(jwtResponse.get("url"))
            .header("Authorization", jwtResponse.get("jwt"))
            .header("zapiAccessKey", ConfigReader.get("zephyr.accessKey"))
            .header("Content-Type", "application/json")
            .body(bodyJson.toString())
            .asString();

    }

    private static JSONObject getIssuesByJqlStartingAt(String jql, Integer startAt) throws UnirestException{
        Base64.Encoder encoder = Base64.getEncoder();
        String signature = encoder.encodeToString(((String)ConfigReader.get("jira.userId") + ":" + ConfigReader.get("jira.apiKey")).getBytes());
        String url = ConfigReader.get("jira.baseUrl") + "/rest/api/3/search?fields=key&maxResults=100&startAt="+startAt.toString()+"&jql=" + jql;
        JSONObject response = Unirest.get(url)
            .header("Authorization", "Basic " + signature)
            .asJson().getBody().getObject();
        return (response);
    }
    
    public static List<TestCycle> getTestCycles(String projectId, String versionId) throws URISyntaxException, UnirestException, IOException{
        Map<String, String> jwtResponse  = GenerateJwt.jwt("GET", "/public/rest/api/1.0/cycles/search?versionId="+versionId+"&projectId="+projectId);
        JSONArray response = Unirest.get(jwtResponse.get("url"))
            .header("Authorization", jwtResponse.get("jwt"))
            .header("zapiAccessKey", ConfigReader.get("zephyr.accessKey"))
            .asJson().getBody().getArray();
        List<TestCycle> testCycles = new ArrayList<>();
        for(int index=0; index < response.length(); index++){
            testCycles.add(new TestCycle(response.getJSONObject(index)));
        }
        return testCycles;
    }

    public static TestCycle getTestCycleByName(String projectId, String versionId, String cycleName) throws URISyntaxException, UnirestException, IOException{
        TestCycle cycle = null;
        for(TestCycle testCycle : getTestCycles(projectId, versionId)){
            if(testCycle.name().equals(cycleName)){
                cycle = testCycle;
            }
        }
        if(cycle == null){
            throw new RuntimeException("no cycle found by given name");
        }
        return cycle;
    }

    public static List<TestExecution> getTestExecutionsByTestCycleId(String projectId, String versionId, String testCycleId) throws URISyntaxException, UnirestException, IOException{
        //TODO handle respose when cycle has more than 50 results with pagination
        Map<String, String> jwtResponse  = GenerateJwt.jwt("GET", "/public/rest/api/1.0/executions/search/cycle/"+testCycleId+"?size=50&versionId="+versionId+"&projectId="+projectId);
        JSONObject response = Unirest.get(jwtResponse.get("url"))
            .header("Authorization", jwtResponse.get("jwt"))
            .header("zapiAccessKey", ConfigReader.get("zephyr.accessKey"))
            .asJson().getBody().getObject();
        List<TestExecution> testExecutions = new ArrayList<>();
        JSONArray objectList = response.getJSONArray("searchObjectList");
        for(int index=0; index < objectList.length(); index++){
            testExecutions.add(new TestExecution(objectList.getJSONObject(index)));
        }
        return testExecutions;
    }

    public static Issue getIssue(String issueKey) throws UnirestException{
        Base64.Encoder encoder = Base64.getEncoder();
        String signature = encoder.encodeToString(((String)ConfigReader.get("jira.userId") + ":" + ConfigReader.get("jira.apiKey")).getBytes());
        String url = ConfigReader.get("jira.baseUrl") + "/rest/agile/1.0/issue/" + issueKey;
        JSONObject response = Unirest.get(url)
            .header("Authorization", "Basic " + signature)
            .asJson().getBody().getObject();
        return new Issue(response);
    }

    public static void postExecutionResult(String projectId, String versionId, String executionId, Map<String, String> body) throws URISyntaxException, UnirestException, IOException {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("status", new JSONObject(body.get("status")));
        bodyJson.put("id", body.get("id"));
        bodyJson.put("issueId", body.get("issueId"));
        bodyJson.put("cycleId", body.get("cycleId"));
        bodyJson.put("comment", body.get("comment"));
        bodyJson.put("projectId", projectId );
        bodyJson.put("versionId", versionId);
        Map<String, String> jwtResponse  = GenerateJwt.jwt("PUT", "/public/rest/api/1.0/execution/" + executionId);
        HttpResponse<String> response = Unirest.put(jwtResponse.get("url"))
            .header("Authorization", jwtResponse.get("jwt"))
            .header("zapiAccessKey", ConfigReader.get("zephyr.accessKey"))
            .header("Content-Type", "application/json")
            .body(bodyJson.toString())
            .asString();
    }

}
