package com.qainfotech.tap;

import org.testng.annotations.*;
import com.qainfotech.tap.TestCoverageReport;

public class ReportTest {

    @Test
    public void fTest() throws Exception {
        TestCoverageReport.generate();
    }
}
