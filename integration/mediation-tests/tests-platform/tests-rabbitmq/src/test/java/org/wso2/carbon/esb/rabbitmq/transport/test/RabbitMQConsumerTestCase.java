/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.rabbitmq.transport.test;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.esb.rabbitmq.utils.RabbitMQServerInstance;
import org.wso2.carbon.esb.rabbitmq.utils.RabbitMQTestUtils;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.clients.rabbitmqclient.RabbitMQProducerClient;
import org.wso2.esb.integration.common.utils.common.FixedSizeSymbolGenerator;

import java.io.File;

/**
 * RabbitMQConsumerTestCase tests EI as a rabbitmq consumer for small messages as well as large messages.
 */
public class RabbitMQConsumerTestCase extends ESBIntegrationTest {

    private LogViewerClient logViewer;
    private RabbitMQProducerClient sender;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        sender = RabbitMQServerInstance.createProducerWithDeclaration("exchange2", "simple_consumer_test");
        //The consumer proxy cannot be pre-deployed since the queue declaration(which is done in 'initRabbitMQBroker')
        // must happen before deployment.
        loadESBConfigurationFromClasspath(File.separator + "artifacts" + File.separator
                                          + "ESB" + File.separator + "rabbitmq" + File.separator +
                                          "transport" + File.separator + "rabbitmq_consumer_proxy.xml");
        logViewer = new LogViewerClient(contextUrls.getBackEndUrl(), getSessionCookie());
    }

    @Test(groups = { "wso2.esb" }, description = "Test ESB as a RabbitMQ Consumer ")
    public void testRabbitMQConsumer() throws Exception {
        logViewer.clearLogs();

        String message = "<ser:placeOrder xmlns:ser=\"http://services.samples\">\n" + "<ser:order>\n"
                + "<ser:price>100</ser:price>\n" + "<ser:quantity>2000</ser:quantity>\n"
                + "<ser:symbol>RMQ</ser:symbol>\n" + "</ser:order>\n" + "</ser:placeOrder>";
        for (int i = 0; i < 200; i++) {
            sender.sendMessage(message, "text/plain");
        }

        RabbitMQTestUtils.waitForLogToGetUpdated();

        LogEvent[] logs = logViewer.getAllRemoteSystemLogs();
        int count = 0;

        for (LogEvent logEvent : logs) {
            if (logEvent == null) {
                continue;
            }
            String logMessage = logEvent.getMessage();
            if (logMessage.contains("received = true")) {
                count++;
            }
        }
        Assert.assertEquals(count, 200, "All messages are not received from queue");
    }

    @Test(groups = { "wso2.esb" }, description = "Test ESB as a RabbitMQ Consumer with large messages ~10KB")
    public void testRabbitMQConsumerLargeMessage() throws Exception {
        logViewer.clearLogs();

        String message = FixedSizeSymbolGenerator.generateMessageKB(10);
        for (int i = 0; i < 200; i++) {
            sender.sendMessage(message, "text/plain");
        }

        // Wait for the log to get updated
        Thread.sleep(20000);

        LogEvent[] logs = logViewer.getAllRemoteSystemLogs();
        int count = 0;

        for (LogEvent logEvent : logs) {
            String logMessage = logEvent.getMessage();
            if (logMessage.contains("received = true")) {
                count++;
            }
        }

        Assert.assertEquals(count, 200, "All messages are not received from queue");
    }

    @AfterClass(alwaysRun = true)
    public void end() throws Exception {
        super.cleanup();
        sender.disconnect();
        sender = null;
        logViewer = null;
    }
}
