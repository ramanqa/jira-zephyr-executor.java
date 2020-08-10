package com.qainfotech.tap;

import com.qainfotech.tap.jira.JiraAPI;
import com.qainfotech.tap.jira.models.*;

import java.util.List;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import kong.unirest.Unirest;

public class JiraZephyrExecutor {

  
    public static void main(String... args) throws Exception{
        Unirest.config().verifySsl(false);
        try{
            String suiteName = ConfigReader.get("testng.suite");

            Boolean modeDefault = true;
            Boolean modeStaged = false;
            Boolean modeRerun = false;
            modeStaged = ConfigReader.isFlagSet("jiraTestRunner.mode.staged");
            modeRerun = ConfigReader.isFlagSet("jiraTestRunner.mode.rerun");

            String testCycleName = ConfigReader.get("test.testCycleName");

            System.out.println("== JiraTestRunner ==");
            if(modeDefault){
                System.setProperty("jiraTestRunner.step.createNewCycle", "true");
                System.setProperty("jiraTestRunner.step.buildSuite", "true");
                System.setProperty("jiraTestRunner.step.runSuite", "true");
                System.setProperty("jiraTestRunner.step.postResults", "true");
                System.setProperty("jiraTestRunner.step.generateDashboard", "true");
                testCycleName = createNewCycle();
                buildSuite(testCycleName);
                runSuite();
                postResults();
                generateDashboard();
            } else if(modeStaged){
                System.out.println("=== exec.mode = staged");
                testCycleName = createNewCycle();
                buildSuite(testCycleName);
                runSuite();
                postResults();
                generateDashboard();
            } else if(modeRerun){
                testCycleName = (new String(Files.readAllBytes(Paths.get("target/testCycleName")))).trim();
                System.setProperty("test.testCycleName", testCycleName);
                Files.copy(Paths.get("target/test-report/testng-failed.xml"), Paths.get("target/failed.xml"), StandardCopyOption.REPLACE_EXISTING);
                System.setProperty("testng.suite","failed");
                System.out.println("=== exec.mode = rerun");
                runSuite();
                postResults();
                generateDashboard();
            }

            System.out.println("== Done. ==");
            Unirest.shutDown();
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            Unirest.shutDown();
        }
        
    }

    public static String createNewCycle() throws Exception {
        Boolean createNewCycle = ConfigReader.isFlagSet("jiraTestRunner.step.createNewCycle");
        String projectId = ConfigReader.get("test.projectId");
        String versionId = ConfigReader.get("test.versionId");
        String label = ConfigReader.get("test.label");
        String testCycleName = ConfigReader.get("test.testCycleName");
        System.out.println("=== jiraTestRunner.step.createNewCycle = " + createNewCycle);
        if(createNewCycle){
            System.out.println("=== Building Test Cycle from Label: " + label);
            SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
            Date date = new Date();
            testCycleName += "_" + formatter.format(date);// + "__" +  System.currentTimeMillis();
            String testCycleId = JiraAPI.createTestCycle(projectId, versionId, testCycleName);
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
        FileWriter fw = new FileWriter("target/testCycleName");
        fw.write(testCycleName);
        fw.close();
        return testCycleName;
    }

    public static void buildSuite(String testCycleName) throws Exception {
        String suiteName = ConfigReader.get("testng.suite");
        Boolean buildSuite = ConfigReader.isFlagSet("jiraTestRunner.step.buildSuite");
        String projectId = ConfigReader.get("test.projectId");
        String versionId = ConfigReader.get("test.versionId");
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
    }

    public static void runSuite() {
        String suiteName = ConfigReader.get("testng.suite");
        Boolean runSuite = ConfigReader.isFlagSet("jiraTestRunner.step.runSuite");
        System.out.println("=== jiraTestRunner.step.runSuite = " + runSuite);
        if(runSuite){
            System.out.println("=== Running Test Suite: target/"+suiteName+".xml ");
            TestNGSuite.run();
        }
        System.out.println("=== Done execution. Results available at target/test-report");
    }

    public static void postResults() throws Exception {
        Boolean postResults = ConfigReader.isFlagSet("jiraTestRunner.step.postResults");
        String projectId = ConfigReader.get("test.projectId");
        String versionId = ConfigReader.get("test.versionId");
        System.out.println("=== jiraTestRunner.step.postResults = " + postResults);
        if(postResults){
            System.out.println("=== Posting results to JIRA Test Execution");
            for(Map<String, String> testResult:TestNGResults.report()){
                JiraAPI.postExecutionResult(projectId, versionId, testResult.get("id"), testResult);
            }
        }
    }

    public static void generateDashboard() throws Exception {
        Boolean generateDashboard = ConfigReader.isFlagSet("jiraTestRunner.step.generateDashboard");
        System.out.println("=== jiraTestRunner.step.generateDashboard = " + generateDashboard);
        if(generateDashboard){
            System.out.println("=== Generating Dashboard");
            TestCoverageReport.generate();
        }
    }
}
