package com.qainfotech.tap;

import com.qainfotech.tap.jira.JiraAPI;
import com.qainfotech.tap.jira.models.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.io.File;
import java.io.FileWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;

public class TestCoverageReport {

    public static void generate() throws Exception {
        File file = new File("target/report");
        file.mkdir();
        File dashboard = new File("target/report/dashboard.html");
        FileUtils.copyInputStreamToFile(TestCoverageReport.class.getClassLoader().getResourceAsStream("dashboard.html"), dashboard);
        File testResults = new File("target/report/test-results.html");
        FileUtils.copyInputStreamToFile(TestCoverageReport.class.getClassLoader().getResourceAsStream("test-results.html"), testResults);
        File reportjs = new File("target/report/dashboard.js");
       
        String projectId = ConfigReader.get("test.projectId");
        String versionId = ConfigReader.get("test.versionId");
        String testCycleName = ConfigReader.get("test.testCycleName");
        String labels = "";
        String pass = "";
        String fail = "";
        String blocked = "";
        String unexecuted = "";

        TestCycle lastTestCycle = null;
        for(TestCycle testCycle:JiraAPI.getTestCycles(projectId, versionId)){
            if(testCycle.name().startsWith(testCycleName)){
                lastTestCycle = testCycle;
                labels += "\"" + testCycle.name().split("_")[testCycle.name().split("_").length-1] + "\",";
                pass += testCycle.executionSummary().get("PASS") + ",";
                fail += testCycle.executionSummary().get("FAIL") + ",";
                blocked += testCycle.executionSummary().get("BLOCKED") + ",";
                unexecuted += testCycle.executionSummary().get("UNEXECUTED") + ",";
            }
        }

        String testtrendjson = "";
        testtrendjson += "var labels = [" + labels + "];\n";
        testtrendjson += "var pass = [" + pass + "];\n";
        testtrendjson += "var fail = [" + fail + "];\n";
        testtrendjson += "var blocked = [" + blocked + "];\n";
        testtrendjson += "var unexecuted = [" + unexecuted + "];\n";

        List<TestExecution> testExecutions = JiraAPI.getTestExecutionsByTestCycleId(projectId, versionId, lastTestCycle.id());
      
        List<TestScript> testScripts = new ArrayList<>();
        List<Map<String, String>> testReport = TestNGResults.report();
        for(TestExecution testExecution:testExecutions){
            Map<String, String> report = new HashMap<>();
            for(Map<String, String> testResult:testReport){
                if(testResult.get("issueKey").equals(testExecution.issueKey())){
                    report = testResult;
                }
            }
            testScripts.add(new TestScript(testExecution, report));
        }

        // coverage summary
        Integer totalTestsCovered = 0;
        Integer totalScripts = 0;
        Integer totalScriptsPass = 0;
        Integer totalScriptsFail = 0;
        Integer totalScriptsBlocked = 0;
        Integer totalScriptsUnexecuted = 0;
        for(TestScript testScript:testScripts){
            totalScripts++;
            totalTestsCovered += testScript.coveredTests().size();
            if(testScript.result().equals("PASS")){
                totalScriptsPass++;
            }else if(testScript.result().equals("FAIL")){
                totalScriptsFail++;
            }else if(testScript.result().equals("BLOCKED")){
                totalScriptsBlocked++;
            }else if(testScript.result().equals("UNEXECUTED")){
                totalScriptsUnexecuted++;
            }
        }

        System.out.println("Total Tests:" + totalScripts);
        System.out.println("Total Tests Covered:" + totalTestsCovered);
        System.out.println("Total Tests Passed:" + totalScriptsPass);
        System.out.println("Total Tests Failed:" + totalScriptsFail);
        System.out.println("Total Tests Blocked:" + totalScriptsBlocked);
        System.out.println("Total Tests Unexecuted:" + totalScriptsUnexecuted);

        String summaryjs = "";
        summaryjs += "document.querySelector('#tdTotalScripts').innerText=" + totalScripts + ";\n";
        summaryjs += "document.querySelector('#tdTotalTestsCovered').innerText=" + totalTestsCovered + ";\n";
        summaryjs += "document.querySelector('#tdTotalScriptsPass').innerText=" + totalScriptsPass + ";\n";
        summaryjs += "document.querySelector('#tdTotalScriptsFail').innerText=" + totalScriptsFail + ";\n";
        summaryjs += "document.querySelector('#tdTotalScriptsBlocked').innerText=" + totalScriptsBlocked + ";\n";
        summaryjs += "document.querySelector('#tdTotalScriptsUnexecuted').innerText=" + totalScriptsUnexecuted + ";\n";

        summaryjs += "\n" + testtrendjson;

        FileWriter writer = new FileWriter("target/report/dashboard.js");
        writer.write(summaryjs);
        writer.close();


        String resultsjs = "";
        JSONArray resultsJson = new JSONArray();
        if(ConfigReader.isFlagSet("jiraTestRunner.mode.rerun")){
            String resultsJs = (new String(Files.readAllBytes(Paths.get("target/report/test-results.js")))).trim();
            resultsJs = "[{" + resultsJs.split("var results = \\[\\{")[1];
            resultsJs = resultsJs.substring(0, resultsJs.length() -1);
            resultsJson = new JSONArray(resultsJs);
        }
        for(TestScript testScript:testScripts){
          JSONObject testScriptResult = new JSONObject();
          testScriptResult.put("key", testScript.key());
          testScriptResult.put("summary", StringEscapeUtils.escapeHtml(testScript.summary().toString().trim()));
          testScriptResult.put("result", testScript.result());
          JSONArray coveredTestsJson = new JSONArray();
          for(Map<String, String> coveredTest:testScript.coveredTests()){
             JSONObject coveredTestJson = new JSONObject();
             coveredTestJson.put("key", coveredTest.get("key"));
             coveredTestJson.put("summary", coveredTest.get("summary"));
             coveredTestsJson.put(coveredTestJson);
          }
          testScriptResult.put("coveredTests", coveredTestsJson);
          JSONObject resultDetails = new JSONObject();
          resultDetails.put("url", testScript.report().get("detailedResultsUrl"));
          resultDetails.put("log", testScript.report().get("log"));
          testScriptResult.put("details", resultDetails);
          if(ConfigReader.isFlagSet("jiraTestRunner.mode.rerun")){
              for(int index=0; index<resultsJson.length(); index++){
                  JSONObject oldResult = resultsJson.getJSONObject(index);
                  if(oldResult.getString("key").equals(testScriptResult.get("key"))){
                      resultsJson.put(index, testScriptResult);
                  }
              }
          }else{
              resultsJson.put(testScriptResult);
          }
        }
        resultsjs += "var results = " + resultsJson.toString() + ";";
        FileWriter rwriter = new FileWriter("target/report/test-results.js");
        rwriter.write(resultsjs);
        rwriter.close();
    }    
}
