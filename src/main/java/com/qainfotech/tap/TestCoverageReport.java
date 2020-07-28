package com.qainfotech.tap;

import com.qainfotech.tap.jira.JiraAPI;
import com.qainfotech.tap.jira.models.*;
import java.util.List;

public class TestCoverageReport {

    public static void generate() throws Exception {

        String projectId = ConfigReader.get("test.projectId");
        String versionId = ConfigReader.get("test.versionId");

        List<TestCycle> testCycles = JiraAPI.getTestCycles(projectId, versionId);
        TestCycle lastTestCycle = null;
        System.out.println(testCycles);
        System.out.println(testCycles.size());
        for(TestCycle testCycle:testCycles){
            if(testCycle.name().startsWith("Jenkins_")){
                lastTestCycle = testCycle;
            }
        }

        List<TestExecution> testExecutions = JiraAPI.getTestExecutionsByTestCycleId(projectId, versionId, lastTestCycle.id());
       
        for(TestExecution testExecution:testExecutions){
            Issue testIssue = JiraAPI.getIssue(testExecution.issueKey());
            System.out.println(testIssue.key());
            System.out.println(testIssue.relatesToIssues());
        }
        System.out.println(testExecutions.get(0));
        


    }    
    
}
