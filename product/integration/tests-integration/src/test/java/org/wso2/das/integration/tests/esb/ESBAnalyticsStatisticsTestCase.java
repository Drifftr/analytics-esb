/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.das.integration.tests.esb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.analytics.dataservice.commons.AggregateField;
import org.wso2.carbon.analytics.dataservice.commons.AggregateRequest;
import org.wso2.carbon.analytics.datasource.commons.AnalyticsIterator;
import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.analytics.spark.admin.stub.AnalyticsProcessorAdminServiceStub;
import org.wso2.das.integration.common.utils.TestConstants;
import org.wso2.das.integration.tests.DASIntegrationBaseTest;
import org.wso2.das4esb.integration.common.clients.ConcurrentEventsPublisher;
import org.wso2.das4esb.integration.common.clients.DataPublisherClient;

/**
 * Class contains test cases related to statistics
 */
public class ESBAnalyticsStatisticsTestCase extends DASIntegrationBaseTest {
    
    protected static final Log log = LogFactory.getLog(ESBAnalyticsStatisticsTestCase.class);
    private static final int TOTAL_REQUESTS_PER_PROXY = 20000;
    private static final int NUMBER_OF_PROXIES = 5;
    private static final int NUMBER_OF_MEDIATORS = 10;
    private static final int NUMBER_OF_FAULTS = 20;
    private static final boolean ENABLE_PAYLOADS = false;
    private static final boolean ENABLE_PROPERTIES = false;
    private static final int SLEEP_BETWEEN_REQUESTS = 25;
    private static final int WAIT_FOR_PUBLISHING_IN_MINUTES = 12;
    private static final int WAIT_FOR_INDEXING = 120000;
    private static final int TIMEOUT = 60000;
    
    @BeforeClass(groups = "wso2.das4esb.stats", alwaysRun = true)
    protected void init() throws Exception {
        super.init();
        log.info("Publishing events");
        publishSampleData(NUMBER_OF_PROXIES, TOTAL_REQUESTS_PER_PROXY, NUMBER_OF_MEDIATORS, NUMBER_OF_FAULTS,
                ENABLE_PAYLOADS, ENABLE_PROPERTIES, TestConstants.TENANT_IDS);
        log.info("Publishing complete. Waiting for indexing...");
        Thread.sleep(WAIT_FOR_INDEXING);
        log.info("Indexing complete. Executing the spark scripts...");
        AnalyticsProcessorAdminServiceStub analyticsStub = getAnalyticsProcessorStub(TIMEOUT);
        analyticsStub.executeScript("EsbAnalytics-SparkScript-Realtime-Statistic");
    }

    
    /**************** Testing Overall Counts ****************/
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total invocation counts in per-second table")
    public void testSecondTableTotalCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_SECOND_ALL_TABLE, TestConstants.NUMBER_OF_INVOCATION, "ALL",
            TOTAL_REQUESTS_PER_PROXY * NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total invocation counts in per-minute table")
    public void testMinuteTableTotalCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_MINUTE_ALL_TABLE, TestConstants.NUMBER_OF_INVOCATION, "ALL",
            TOTAL_REQUESTS_PER_PROXY * NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total invocation counts in per-hour table")
    public void testHourTableTotalCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_HOUR_TABLE, TestConstants.NUMBER_OF_INVOCATION, "ALL",
            TOTAL_REQUESTS_PER_PROXY * NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total invocation counts in per-day table")
    public void testDayTableTotalCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_DAY_TABLE, TestConstants.NUMBER_OF_INVOCATION, "ALL",
            TOTAL_REQUESTS_PER_PROXY * NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total invocation counts in per-month table")
    public void testMonthTableTotalCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_MONTH_TABLE, TestConstants.NUMBER_OF_INVOCATION, "ALL",
                TOTAL_REQUESTS_PER_PROXY * NUMBER_OF_PROXIES);
    }
    
    
    /**************** Testing Mediator Counts ****************/
    
    @Test(groups = "wso2.das4esb.stats", description =  "Test mediator invocation counts in per-second table")
    public void testSecondTableMediatorCount() throws Exception {
        testMediatorInvocationCounts(TestConstants.MEDIATOR_STAT_PER_SECOND_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test mediator invocation counts in per-minute table")
    public void testMinuteTableMediatorCount() throws Exception {
        testMediatorInvocationCounts(TestConstants.MEDIATOR_STAT_PER_MINUTE_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description =  "Test mediator invocation counts in per-hour table")
    public void testHourTableMediatorCount() throws Exception {
        testMediatorInvocationCounts(TestConstants.MEDIATOR_STAT_PER_HOUR_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test mediator invocation counts in per-day table")
    public void testDayTableMediatorCount() throws Exception {
        testMediatorInvocationCounts(TestConstants.MEDIATOR_STAT_PER_DAY_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description =  "Test mediator invocation counts in per-month table")
    public void testMonthTableMediatorCount() throws Exception {
        testMediatorInvocationCounts(TestConstants.MEDIATOR_STAT_PER_MONTH_TABLE);
    }

    
    /**************** Testing Total fault Counts ****************/
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total faults count in per-second table")
    public void testSecondTableTotalErrorCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_SECOND_ALL_TABLE, TestConstants.FAULT_COUNT, "ALL", NUMBER_OF_FAULTS *
            NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total faults count in per-minute table")
    public void testMinuteTableTotalErrorCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_MINUTE_ALL_TABLE, TestConstants.FAULT_COUNT, "ALL", NUMBER_OF_FAULTS *
            NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total faults count in per-hour table")
    public void testHourTableTotalErrorCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_HOUR_TABLE, TestConstants.FAULT_COUNT, "ALL", NUMBER_OF_FAULTS *
            NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total faults count in per-day table")
    public void testDayTableTotalErrorCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_DAY_TABLE, TestConstants.FAULT_COUNT, "ALL", NUMBER_OF_FAULTS *
            NUMBER_OF_PROXIES);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test total faults count in per-month table")
    public void testMonthTableTotalErrorCount() throws Exception {
        testCounts(TestConstants.ESB_STAT_PER_MONTH_TABLE, TestConstants.FAULT_COUNT, "ALL", NUMBER_OF_FAULTS *
                NUMBER_OF_PROXIES);
    }
    
    
    /**************** Testing mediator fault counts ****************/
    
    @Test(groups = "wso2.das4esb.stats", description = "Test mediator faults count in per-second table")
    public void testSecondTableMediatorErrorCount() throws Exception {
        testMediatorFaultCounts(TestConstants.MEDIATOR_STAT_PER_SECOND_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test mediator faults count in per-minute table")
    public void testMinuteTableMediatorErrorCount() throws Exception {
        testMediatorFaultCounts(TestConstants.MEDIATOR_STAT_PER_MINUTE_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test mediator faults count in per-hour table")
    public void testHourTableMediatorErrorCount() throws Exception {
        testMediatorFaultCounts(TestConstants.MEDIATOR_STAT_PER_HOUR_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test mediator faults count in per-day table")
    public void testDayTableMediatorErrorCount() throws Exception {
        testMediatorFaultCounts(TestConstants.MEDIATOR_STAT_PER_DAY_TABLE);
    }
    
    @Test(groups = "wso2.das4esb.stats", description = "Test mediator faults count in per-month table")
    public void testMonthTableMediatorErrorCount() throws Exception {
        testMediatorFaultCounts(TestConstants.MEDIATOR_STAT_PER_MONTH_TABLE);
    }
    
    
    @AfterClass(alwaysRun = true, groups = "wso2.das4esb.publishing")
    public void cleanUpTables() throws Exception {
        restartAndCleanUpTables(120000);
    }
    
    
    /**
     * Publish sample data for tenants
     * 
     * @param tenants
     * @throws Exception
     */
    private void publishSampleData(int noOfProxies, int requestsPerProxy, int noOfMediators, int NoOfFaults, 
                boolean enablePayloads, boolean enableProperties, int [] tenants) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(noOfProxies * tenants.length);
        for (int tenantId: TestConstants.TENANT_IDS) {
            for (int i = 0; i < noOfProxies; i++) {
                DataPublisherClient  dataPublisherClient = new DataPublisherClient();
                executorService.execute(new ConcurrentEventsPublisher(dataPublisherClient, tenantId, requestsPerProxy,
                        "AccuracyTestProxy_" + i, noOfMediators, NoOfFaults, enablePayloads, 
                        enableProperties, SLEEP_BETWEEN_REQUESTS));
            }
        }
        executorService.shutdown();
        executorService.awaitTermination(WAIT_FOR_PUBLISHING_IN_MINUTES, TimeUnit.MINUTES);
    }
    
    
    /**
     * Test counts in a given table
     * 
     * @param table                 Table name               
     * @param aggregateAttribute    Aggregate attribute
     * @param componentId           Component ID
     * @param expectedCount         Expected count
     * @throws AnalyticsException
     */
    private void testCounts(String table, String aggregateAttribute, String componentId, int expectedCount) 
                throws AnalyticsException {
        for (int tenantId: TestConstants.TENANT_IDS) {
            List<AggregateField> fields = new ArrayList<AggregateField>();
            fields.add(new AggregateField(new String[] { aggregateAttribute }, "sum", TestConstants.REQUEST_COUNT));
            AggregateRequest aggregateRequest = new AggregateRequest();
            aggregateRequest.setFields(fields);
            aggregateRequest.setAggregateLevel(0);
            aggregateRequest.setParentPath(new ArrayList<String>());
            aggregateRequest.setGroupByField(TestConstants.COMPONENT_ID);
            aggregateRequest.setQuery(TestConstants.META_TENANT_ID + ":" + tenantId + " AND " + TestConstants.COMPONENT_ID +
                    ":\"" + componentId + "\"");
            aggregateRequest.setTableName(table);
            AnalyticsIterator<Record> resultItr = this.analyticsDataAPI.searchWithAggregates(-1234, aggregateRequest);
            int count = ((Double) resultItr.next().getValue(TestConstants.REQUEST_COUNT)).intValue();
            log.info("ComponentId: " + componentId + " | Expected: " + expectedCount + " | " + "Actual: "
                    + count + " | tenant: " + tenantId);
            Assert.assertEquals(count, expectedCount, aggregateAttribute + " is incorrect in " + table + 
                    " table, for tenant: " + tenantId);
        }
    }
    
    
    /**
     * Check the total number of invocations in all the mediators in a given table
     * 
     * @param table Table Name
     * @throws      AnalyticsException
     * @throws      InterruptedException 
     */
    private void testMediatorInvocationCounts(String table) throws AnalyticsException, InterruptedException {
        log.info("Checking mediator invocation count in " + table + " table:");
        for (int proxyNumber = 0; proxyNumber < NUMBER_OF_PROXIES; proxyNumber++) {
            for (int mediatorNumber = 1; mediatorNumber <= NUMBER_OF_MEDIATORS; mediatorNumber++) {
                String mediatorId = "AccuracyTestProxy_" + proxyNumber + "@" + mediatorNumber + ":mediator_" + 
                        mediatorNumber;
                testCounts(table, TestConstants.NUMBER_OF_INVOCATION, mediatorId, TOTAL_REQUESTS_PER_PROXY);
            }
            log.info("AccuracyTestProxy_" + proxyNumber + ": All mediators: Ok");
        }
    }
    
    
    /**
     * Check the total number of faults in all the mediators in a given table
     * 
     * @param table     Table Name
     * @throws          AnalyticsException
     */
    private void testMediatorFaultCounts(String table) throws AnalyticsException {
        log.info("Checking mediator faults count in " + table + " table:");
        for (int proxyNumber = 0; proxyNumber < NUMBER_OF_PROXIES; proxyNumber++) {
            for (int mediatorNumber = 1; mediatorNumber <= NUMBER_OF_MEDIATORS; mediatorNumber++) {
                int expectedFaultCount = 0;
                if (mediatorNumber == NUMBER_OF_MEDIATORS) {
                    expectedFaultCount = NUMBER_OF_FAULTS;
                }
                String mediatorId = "AccuracyTestProxy_" + proxyNumber + "@" + mediatorNumber + ":mediator_" + 
                        mediatorNumber;
                testCounts(table, TestConstants.FAULT_COUNT, mediatorId, expectedFaultCount);
            }
            log.info("AccuracyTestProxy_" + proxyNumber + ": All mediators: Ok");
        }
    }
}
