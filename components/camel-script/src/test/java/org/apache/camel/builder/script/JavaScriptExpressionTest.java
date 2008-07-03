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
package org.apache.camel.builder.script;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.builder.RouteBuilder;

/**
 * Tests a routing expression using JavaScript
 */
public class JavaScriptExpressionTest extends ContextTestSupport {
    public void testSendMatchingMessage() throws Exception {
        // Currently, this test fails because the JavaScript expression in createRouteBuilder
        // below returns false
        // To fix that, we need to figure out how to get the expression to return the right value
        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:unmatched").expectedMessageCount(0);

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "bar");
        sendBody("direct:start", "hello", headers);

        assertMockEndpointsSatisifed();
    }

    public void testSendNonMatchingMessage() throws Exception {
        getMockEndpoint("mock:result").expectedMessageCount(0);
        getMockEndpoint("mock:unmatched").expectedMessageCount(1);

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("foo", "foo");
        sendBody("direct:start", "hello", headers);

        assertMockEndpointsSatisifed();
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:start").choice().
                        // The following java script expression should return a boolean
                        // but it seems to always return false -- what's up with that?
                        when().javaScript("request.headers.get('foo') == 'bar'").to("mock:result")
                        .otherwise().to("mock:unmatched");
            }
        };
    }
}