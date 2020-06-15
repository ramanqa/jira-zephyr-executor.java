package com.qainfotech.tap;

import com.qainfotech.tap.jira.JiraAPI;
import com.qainfotech.tap.jira.models.TestCycle;
import com.qainfotech.tap.jira.models.TestExecution;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

import kong.unirest.UnirestException;

public class Dashboard {

    public static void generate() throws URISyntaxException, UnirestException, IOException{
        String projectId = ConfigReader.get("test.projectId");
        String versionId = ConfigReader.get("test.versionId");

        try{Thread.sleep();}catch(Exception e){}

        List<TestCycle> testCycles = JiraAPI.getTestCycles(projectId, versionId);
        String labels = "";
        String pass = "";
        String fail = "";
        String skip = "";
        String unexecuted = "";

        String lastTestCycleId = "";

        for(TestCycle testCycle:testCycles){
            if(testCycle.name().startsWith("Jenkins_")){
                lastTestCycleId = testCycle.id();
                labels += "\"" + testCycle.name().split("Jenkins_TestCycle_")[1] + "\",";
                pass += testCycle.executionSummary().get("PASS") + ",";
                fail += testCycle.executionSummary().get("FAIL") + ",";
                skip += testCycle.executionSummary().get("SKIP") + ",";
                unexecuted += testCycle.executionSummary().get("UNEXECUTED") + ",";
            }
        }

        String json = "";
        json += "var labels = [" + labels + "];\n";
        json += "var pass = [" + pass + "];\n";
        json += "var fail = [" + fail + "];\n";
        json += "var skip = [" + skip + "];\n";
        json += "var unexecuted = [" + unexecuted + "];\n";

        List<TestExecution> testExecutions = JiraAPI.getTestExecutionsByTestCycleId(projectId, versionId, lastTestCycleId);

        String lastExecutionResultTable = "var lastExecutionResults = [\n";
        for(TestExecution testExecution:testExecutions){
            String row = "{";
            row += "key: '" + testExecution.issueKey() + "',";
            row += "summary: '" + testExecution.issueSummary() +"',";
            row += "result: '" + testExecution.result() + "',";
            row += "executionId: '" + testExecution.id() + "',";
            row += "issueId: '" + testExecution.issueId() + "',";
            row += "component: '" + testExecution.component() + "'";
            row += "}\n";
            lastExecutionResultTable += row + ",\n";
        }
        lastExecutionResultTable += "];\n";
        json += lastExecutionResultTable;
        json += "var projectId = " + projectId + ";";
        json += "var versionId = " + versionId + ";";

        List<String> radarLabels = new ArrayList<>();
        Map<String, Integer> radarComponentTotal = new HashMap<>();
        Map<String, Integer> radarComponentPass = new HashMap<>();
        Map<String, Integer> radarComponentFail = new HashMap<>();
        for(TestExecution testExecution:testExecutions){
            if(!radarLabels.contains(testExecution.component())){
                radarLabels.add(testExecution.component());
            }
            if(radarComponentTotal.containsKey(testExecution.component())){
                radarComponentTotal.put(testExecution.component(), radarComponentTotal.get(testExecution.component()) + 1);
            }else{
                radarComponentTotal.put(testExecution.component(), 1);
            }
            if(testExecution.result().equals("PASS")){
                if(radarComponentPass.containsKey(testExecution.component())){
                    radarComponentPass.put(testExecution.component(), radarComponentPass.get(testExecution.component()) + 1);
                }else{
                    radarComponentPass.put(testExecution.component(), 1);
                }
            }
            if(testExecution.result().equals("FAIL")){
                if(radarComponentFail.containsKey(testExecution.component())){
                    radarComponentFail.put(testExecution.component(), radarComponentFail.get(testExecution.component()) + 1);
                }else{
                    radarComponentFail.put(testExecution.component(), 1);
                }
            }
        }
        String testResultsRadar = "\n\nvar testResultsRadarLabels = [";
        for(String component:radarLabels){
            testResultsRadar += "'" + component + "',";
        }
        testResultsRadar += "]\n\n";
        testResultsRadar += "var testResultsRadarTotalData = [";
        for(String component:radarLabels){
            testResultsRadar += radarComponentTotal.get(component) + ",";
        }
        testResultsRadar += "];\n\n";
        testResultsRadar += "var testResultsRadarPassData = [";
        for(String component:radarLabels){
            testResultsRadar += radarComponentPass.get(component) + ",";
        }
        testResultsRadar += "];\n\n";
        testResultsRadar += "var testResultsRadarFailData = [";
        for(String component:radarLabels){
            testResultsRadar += radarComponentFail.get(component) + ",";
        }
        testResultsRadar += "];\n\n";

        json += testResultsRadar;

        File testTrend = new File("target/test-trend.js");
        try (FileWriter writer = new FileWriter(testTrend)) {
          writer.write(json);
          writer.flush();
          writer.close();
        }
    }
}
