package com.qainfotech.tap;

import com.qainfotech.tap.jira.JiraAPI;
import com.qainfotech.tap.jira.models.*;

import java.util.List;
import java.util.Map;

import com.mashape.unirest.http.Unirest;

public class JiraZephyrExecutor {

  
    public static void main(String... args) throws Exception{
        try{
            Boolean createNewCycle = true;
            Boolean buildSuite = true;
            Boolean runSuite = true;
            Boolean postResults = true;

            Boolean staged = false;
            staged = ConfigReader.isFlagSet("jiraTestRunner.mode.staged");

            if(staged){
                createNewCycle = ConfigReader.isFlagSet("jiraTestRunner.step.createNewCycle");
                buildSuite = ConfigReader.isFlagSet("jiraTestRunner.step.buildSuite");
                runSuite = ConfigReader.isFlagSet("jiraTestRunner.step.runSuite");
                postResults = ConfigReader.isFlagSet("jiraTestRunner.step.postResults");
            }
          
            String projectId = ConfigReader.get("test.projectId");
            String versionId = ConfigReader.get("test.versionId");
            String label = ConfigReader.get("test.label");
            String testCycleName = ConfigReader.get("test.testCycleName");
            String testCycleId;

            System.out.println("== JiraTestRunner ==");
            if(staged){
                System.out.println("=== exec.mode = staged");
            }

            System.out.println("=== jiraTestRunner.step.createNewCycle = " + createNewCycle);
            if(createNewCycle){
                System.out.println("=== Building Test Cycle from Label: " + label);
                testCycleName += "__" +  System.currentTimeMillis();
                testCycleId = JiraAPI.createTestCycle(projectId, versionId, testCycleName);
                List<String> issues = JiraAPI.getIssuesByProjectIdAndLabel(projectId, label);
                JiraAPI.addTestsToTestCycle(projectId, versionId, testCycleId, issues);
                System.out.println("=== Created Test Cycle: " + testCycleName);
                System.out.println("=== Added "+issues.size()+" tests to Test Cycle");
                //System.out.println(JiraAPI.getProjectNameById(projectId)); 
            }

            System.out.println("=== jiraTestRunner.step.buildSuite = " + buildSuite);
            if(buildSuite){
                System.out.println("=== Building local Test Suite for Test Cycle: " + testCycleName);
                TestCycle testCycle = JiraAPI.getTestCycleByName(projectId, versionId, testCycleName);
                List<TestExecution> testExecutions = JiraAPI.getTestExecutionsByTestCycleId(projectId, versionId, testCycle.id());
                for(TestExecution testExecution:testExecutions){
                    Issue testIssue = JiraAPI.getIssue(testExecution.issueKey());
                    if(testIssue.taid().get("testClass") != null){
                        String testName = testExecution.issueKey() + "__" + testExecution.cycleId() + "__" + testExecution.issueId() + "__" + testExecution.id();
                        TestNGSuite.addTest(testName, testIssue.taid().get("testClass"), testIssue.taid().get("testMethod"));
                    }
                }
                System.out.println("=== Created test suite: target/JIRATestNGTestSuite.xml");
            }

            System.out.println("=== jiraTestRunner.step.runSuite = " + runSuite);
            if(runSuite){
                System.out.println("=== Running Test Suite: target/JIRATestNGTestSuite.xml ");
                TestNGSuite.run();
            }

            System.out.println("=== jiraTestRunner.step.postResults = " + postResults);
            if(postResults){
                System.out.println("=== Posting results to JIRA Test Execution");
                for(Map<String, String> testResult:TestNGResults.report()){
                    JiraAPI.postExecutionResult(projectId, versionId, testResult.get("id"), testResult);
                }
            }
            System.out.println("== Done. ==");
            Unirest.shutdown();
        }catch(Exception e){
        }finally{
            Unirest.shutdown();
        }
        
    }
}
