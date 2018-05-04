/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.micrometer.messagehistory;

import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.micrometer.MicrometerComponent;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ManagedMessageHistoryTest extends CamelTestSupport {

    private MeterRegistry registry = new SimpleMeterRegistry();

    @Override
    protected boolean useJmx() {
        return true;
    }

    protected MBeanServer getMBeanServer() {
        return context.getManagementStrategy().getManagementAgent().getMBeanServer();
    }
    
    // Setup the common MetricsRegistry for MetricsComponent and MetricsMessageHistoryFactory to use
    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();
        registry.bind(MicrometerComponent.METRICS_REGISTRY_NAME, registry);
        return registry;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        MicrometerMessageHistoryFactory factory = new MicrometerMessageHistoryFactory();
        factory.setPrettyPrint(true);
        factory.setMeterRegistry(registry);
        context.setMessageHistoryFactory(factory);

        return context;
    }

    @Test
    public void testMessageHistory() throws Exception {
        getMockEndpoint("mock:foo").expectedMessageCount(5);
        getMockEndpoint("mock:bar").expectedMessageCount(5);
        getMockEndpoint("mock:baz").expectedMessageCount(5);

        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                template.sendBody("seda:foo", "Hello " + i);
            } else {
                template.sendBody("seda:bar", "Hello " + i);
            }
        }

        assertMockEndpointsSatisfied();

        // there should be 3 names
        assertEquals(3, registry.getMeters().size());

        // there should be 3 mbeans
        Set<ObjectName> set = getMBeanServer().queryNames(new ObjectName("org.apache.camel.micrometer:*"), null);
        assertEquals(3, set.size());

        // get the message history service using JMX
        String name = String.format("org.apache.camel:context=%s,type=services,name=MicrometerMessageHistoryService", context.getManagementName());
        ObjectName on = ObjectName.getInstance(name);
        String json = (String) getMBeanServer().invoke(on, "dumpStatisticsAsJson", null, null);
        assertNotNull(json);
        log.info(json);
        
        assertTrue(json.contains("foo.history"));
        assertTrue(json.contains("bar.history"));
        assertTrue(json.contains("baz.history"));

        // reset
        getMBeanServer().invoke(on, "reset", null, null);

        resetMocks();
        getMockEndpoint("mock:foo").expectedMessageCount(1);

        template.sendBody("seda:foo", "Hello Again");

        assertMockEndpointsSatisfied();

        json = (String) getMBeanServer().invoke(on, "dumpStatisticsAsJson", null, null);
        assertNotNull(json);
        log.info(json);

        assertTrue(json.contains("foo.history"));
        assertFalse(json.contains("bar.history"));
        assertFalse(json.contains("baz.history"));
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("seda:foo")
                        .to("mock:foo").id("foo");

                from("seda:bar")
                        .to("mock:bar").id("bar")
                        .to("mock:baz").id("baz");
            }
        };
    }
}