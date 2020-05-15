package com.qainfotech.tap;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;
import java.util.Scanner;

public class TestNGResults{

    public static List<Map<String, String>> report() throws FileNotFoundException{
        List<Map<String, String>> results = new ArrayList<>();
        Scanner executionStatusScanner = new Scanner(TestNGResults.class.getClassLoader().getResourceAsStream("executionStatus.json"), "UTF-8");
        String executionStatusText = executionStatusScanner.useDelimiter("\\A").next();
        JSONObject executionStatus = new JSONObject(executionStatusText);
        executionStatusScanner.close();

        XML report = new XMLDocument(new File("target/test-report/testng-results.xml"));

        for(XML test:report.nodes("//test")){
            Map<String, String> result = new HashMap<>(); 
            String testName = test.node().getAttributes().getNamedItem("name").getNodeValue();
            String issueKey = test.node().getAttributes().getNamedItem("name").getNodeValue().split("__")[0];
            String testCycleId = test.node().getAttributes().getNamedItem("name").getNodeValue().split("__")[1];
            String testExecutionIssueId = test.node().getAttributes().getNamedItem("name").getNodeValue().split("__")[2];
            String testExecutionId = test.node().getAttributes().getNamedItem("name").getNodeValue().split("__")[3];
            XML classNode = new XMLDocument(test.toString()).nodes("//class").get(0);
            String className = classNode.node().getAttributes().getNamedItem("name").getNodeValue();
            String methodName = "";
            String status = "";
            String comment = "";
            for(XML methodNode:classNode.nodes("//test-method")){
                Boolean isTestMethod = true;
                try{
                    if(methodNode.node().getAttributes().getNamedItem("is-config").getNodeValue().equalsIgnoreCase("true")){
                        isTestMethod = false;
                    }
                }catch(NullPointerException e){
                }
                if(isTestMethod){
                    methodName = methodNode.node().getAttributes().getNamedItem("name").getNodeValue();
                    String executionLine = className + "#" + methodName;
                    String reportLines = "";
                    String testData = "";
                    if(!status.equals("FAIL")){
                        status = methodNode.node().getAttributes().getNamedItem("status").getNodeValue();
                    }
                    for(XML param:new XMLDocument(methodNode.toString()).nodes("//params/param")){
                        testData += param.node().getTextContent().toString().trim() + ",";
                    }
                    if(testData != ""){
                        testData = testData.substring(0, testData.length()-1);
                    }
                    executionLine += "(" + testData + ")";
                    comment += methodNode.node().getAttributes().getNamedItem("status").getNodeValue().toUpperCase() + " : " + executionLine + "\n";
                    for(XML line:new XMLDocument(methodNode.toString()).nodes("//reporter-output/line")){
                        reportLines += line.node().getTextContent().toString().trim() + "\n";
                    }
                    //TODO: hotlink to test result report in jenkins
                    //comment += "Logs:\n" + reportLines;
                }
            }
            
            result.put("issueKey", issueKey);
            result.put("cycleId", testCycleId);
            result.put("issueId", testExecutionIssueId);
            result.put("id", testExecutionId);
            result.put("status", executionStatus.getJSONObject(status).toString());
            comment += "\n Detailed Report: " + ConfigReader.get("jenkins.url") + "/job/" 
                + ConfigReader.get("jenkins.jobPath") + "/artifact/target/test-report/JIRATestNGTestSuite/"
                + testName + ".html";
            result.put("comment", comment);
            results.add(result);
        }

        return results;
    }
}
