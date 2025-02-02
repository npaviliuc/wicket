/*
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
package org.apache.wicket.protocol.ws.tester;

import org.apache.wicket.mock.MockApplication;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.mock.MockHttpServletRequest;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.protocol.ws.api.WebSocketConnectionOriginFilter;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for https://issues.apache.org/jira/browse/WICKET-5860
 */
class WebSocketTesterProcessorTest
{
	final static AtomicBoolean messageReceived = new AtomicBoolean(false);

	private static class TestProcessor extends TestWebSocketProcessor
	{
		private TestProcessor(HttpServletRequest request, WebApplication application)
		{
			super(request, application);
		}

		@Override
		protected void onOutMessage(String message)
		{
			messageReceived.set(true);
		}

		@Override
		protected void onOutMessage(byte[] message, int offset, int length)
		{
			messageReceived.set(true);
		}
	}

	WicketTester tester;
	WebApplication application = new MockApplication()
	{
		@Override
		protected void init()
		{
			super.init();

			getSharedResources().add(TestWebSocketResource.TEXT, new TestWebSocketResource("expected"));
		}
	};

	@BeforeEach
	public void before()
	{
		tester = new WicketTester(application);
		application.getWicketFilter().setFilterPath("");
	}

	@AfterEach
	public void after()
	{
		tester.destroy();
		TestWebSocketResource.ON_ABORT_CALLED.set(false);
	}

	@Test
	void onConnectNoOrigin()
	{
		// Given header 'Origin' is missing
		configureRequest(new String[] { "http://www.example.com" }, new String[] {});

		// When we open a connection
		TestWebSocketProcessor processor = new TestProcessor(tester.getRequest(), tester.getApplication());
		processor.onOpen(new Object());

		// Then it fails
		assertTrue(TestWebSocketResource.ON_ABORT_CALLED.get());
	}

	@Test
	void onConnectMultipleOrigins()
	{
		// Given the request contains multiple header 'Origin's
		configureRequest(new String[] { "http://www.example.com" }, new String[] { "http://www.example.com", "http://ww2.example.com" });

		// When we open a connection
		TestWebSocketProcessor processor = new TestProcessor(tester.getRequest(), tester.getApplication());
		processor.onOpen(new Object());

		// Then it fails
		assertTrue(TestWebSocketResource.ON_ABORT_CALLED.get());
	}

	@Test
	void onConnectMatchingOrigin()
	{
		// Given header 'Origin' matches the host origin
		configureRequest(new String[] { "http://www.example.com" }, new String[] { "http://www.example.com" });

		// When we open a connection
		TestWebSocketProcessor processor = new TestProcessor(tester.getRequest(), tester.getApplication());
		processor.onOpen(new Object());

		// Then it succeeds
		assertFalse(TestWebSocketResource.ON_ABORT_CALLED.get());
	}

	@Test
	void onConnectMismatchingOrigin()
	{
		// Given header 'Origin' does not match the host origin
		configureRequest(new String[] { "http://www.example.com" }, new String[] { "http://ww2.example.com" });

		// When we open a connection
		TestWebSocketProcessor processor = new TestProcessor(tester.getRequest(), tester.getApplication());
		processor.onOpen(new Object());

		// Then it fails
		assertTrue(TestWebSocketResource.ON_ABORT_CALLED.get());
	}

	protected void configureRequest(String[] allowedDomains, String[] origins)
	{
		WebSocketSettings webSocketSettings = WebSocketSettings.Holder.get(application);
		WebSocketConnectionOriginFilter connectionFilter = new WebSocketConnectionOriginFilter(Arrays.asList(allowedDomains));
		webSocketSettings.setConnectionFilter(connectionFilter);
		MockHttpServletRequest request = tester.getRequest();
		for (String origin : origins)
		{
			request.addHeader("Origin", origin);
		}
		request.addParameter("resourceName", TestWebSocketResource.TEXT);
		request.addParameter(WebRequest.PARAM_AJAX_BASE_URL, ".");
	}

}
