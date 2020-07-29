package com.qainfotech.tap;

import com.qainfotech.tap.jira.JiraAPI;
import com.qainfotech.tap.jira.models.*;

import java.util.List;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;

import kong.unirest.Unirest;

public class JiraZephyrExecutor {

  
    public static void main(String... args) throws Exception{
        Unirest.config().verifySsl(false);
        try{
            Boolean createNewCycle = true;
            Boolean buildSuite = true;
            Boolean runSuite = true;
            Boolean postResults = true;
            Boolean generateDashboard = true;
            Boolean generateCoverageReport = true;

            String suiteName = ConfigReader.get("testng.suite");

            Boolean staged = false;
            staged = ConfigReader.isFlagSet("jiraTestRunner.mode.staged");

            if(staged){
                createNewCycle = ConfigReader.isFlagSet("jiraTestRunner.step.createNewCycle");
                buildSuite = ConfigReader.isFlagSet("jiraTestRunner.step.buildSuite");
                runSuite = ConfigReader.isFlagSet("jiraTestRunner.step.runSuite");
                postResults = ConfigReader.isFlagSet("jiraTestRunner.step.postResults");
                generateDashboard = ConfigReader.isFlagSet("jiraTestRunner.step.generateDashboard");
                generateCoverageReport = ConfigReader.isFlagSet("jiraTestRunner.step.generateCoverageReport");
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
                SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
                Date date = new Date();
                testCycleName += "_" + formatter.format(date);// + "__" +  System.currentTimeMillis();
                testCycleId = JiraAPI.createTestCycle(projectId, versionId, testCycleName);
                List<String> issues = JiraAPI.getIssuesByProjectIdAndLabel(projectId, label);
                JiraAPI.addTestsToTestCycle(projectId, versionId, testCycleId, issues);
                System.out.println("=== Created Test Cycle: " + testCycleName);
                System.out.println("=== Added "+issues.size()+" tests to Test Cycle");
                Integer expectedTestsInTestCycle = issues.size();
                try{
                    System.out.println("Waiting for jira to update test cycle cache...");
                    for(int poll = 1; poll < 121; poll++){
                        System.out.print("..." + 10*poll);
                        Thread.sleep(10000);
                        List<TestExecution> testExecutions = JiraAPI.getTestExecutionsByTestCycleId(projectId, versionId, testCycleId);
                        if(testExecutions.size() == expectedTestsInTestCycle){
                            break;
                        }
                    }
                    System.out.println("...Done");
                }catch(Exception e){}
                //System.out.println(JiraAPI.getProjectNameById(projectId)); 
            }

            System.out.println("=== jiraTestRunner.step.buildSuite = " + buildSuite);
            if(buildSuite){
                System.out.println("=== Building local Test Suite for Test Cycle: " + testCycleName);
                TestCycle testCycle = JiraAPI.getTestCycleByName(projectId, versionId, testCycleName);
                List<TestExecution> testExecutions = JiraAPI.getTestExecutionsByTestCycleId(projectId, versionId, testCycle.id());
                System.out.println("==== Trying to add build local suite with " + testExecutions.size() + " tests");
                for(TestExecution testExecution:testExecutions){
                    System.out.println("==== Adding test to local test suite: " + testExecution.issueKey());
                    try{
                        Issue testIssue = JiraAPI.getIssue(testExecution.issueKey());
                        if(testIssue.taid().get("testClass") != null){
                            String testName = testExecution.issueKey() + "__" + testExecution.cycleId() + "__" + testExecution.issueId() + "__" + testExecution.id();
                            String testId = testExecution.issueKey() + "__" + testExecution.cycleId() + "__" + testExecution.issueId() + "__" + testExecution.id();
                            TestNGSuite.addTest(testName, testId, testIssue.taid().get("testClass"), testIssue.taid().get("testMethod"));
                        }else{
                            System.out.println("==== FAILED to add test to local test suite " + testExecution.issueKey());
                        }
                    }catch(Exception e){
                        System.out.println("==== FAILED to add test to local test suite " + testExecution.issueKey() + " " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                System.out.println("=== Created test suite: target/"+suiteName+".xml");
            }

            System.out.println("=== jiraTestRunner.step.runSuite = " + runSuite);
            if(runSuite){
                System.out.println("=== Running Test Suite: target/"+suiteName+".xml ");
                TestNGSuite.run();
            }

            System.out.println("=== jiraTestRunner.step.postResults = " + postResults);
            if(postResults){
                System.out.println("=== Posting results to JIRA Test Execution");
                for(Map<String, String> testResult:TestNGResults.report()){
                    JiraAPI.postExecutionResult(projectId, versionId, testResult.get("id"), testResult);
                }
            }

            System.out.println("=== jiraTestRunner.step.generateDashboard = " + generateDashboard);
            if(generateDashboard){
                System.out.println("=== Generating Dashboard");
                TestCoverageReport.generate();
            }

            System.out.println("== Done. ==");
            Unirest.shutDown();
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            Unirest.shutDown();
        }
        
    }
}
