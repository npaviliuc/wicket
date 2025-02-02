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

import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for WebSocketTester.
 * Uses WebSocketBehavior.
 *
 * @since 6.18.0
 */
class WebSocketTesterRequestCycleListenerTest
{
	private final AtomicBoolean beginRequestCalled = new AtomicBoolean(false);
	private final AtomicBoolean endRequestCalled = new AtomicBoolean(false);
	private final AtomicBoolean detachCalled = new AtomicBoolean(false);

	private WicketTester tester;

	@BeforeEach
	public void before()
	{
		tester = new WicketTester();
		tester.getApplication().getRequestCycleListeners().add(new IRequestCycleListener()
		{
			@Override
			public void onBeginRequest(RequestCycle cycle)
			{
				beginRequestCalled.set(true);
			}

			@Override
			public void onEndRequest(RequestCycle cycle)
			{
				endRequestCalled.set(true);
			}

			@Override
			public void onDetach(RequestCycle cycle)
			{
				detachCalled.set(true);
			}
		});
	}

	@AfterEach
	public void after()
	{
		tester.destroy();
	}

	/**
	 * A simple test that sends and receives a text message.
	 * The page asserts that it received the correct message and then
	 * pushed back the same message but capitalized.
	 */
	@Test
	void verifyRequestCycleListeners()
	{
		final String expectedMessage = "some message";

		WebSocketBehaviorTestPage page = new WebSocketBehaviorTestPage(expectedMessage);
		tester.startPage(page);

		// reset the variables after starting the page (no WebSocket related request)
		beginRequestCalled.set(false);
		endRequestCalled.set(false);
		detachCalled.set(false);

		// broadcasts WebSocket.ConnectedMessage and notifies the listeners
		WebSocketTester webSocketTester = new WebSocketTester(tester, page) {
			@Override
			protected void onOutMessage(String message)
			{
				assertEquals(Strings.capitalize(expectedMessage), message);
			}
		};

		// assert and reset
		assertTrue(beginRequestCalled.compareAndSet(true, false));
		assertTrue(endRequestCalled.compareAndSet(true, false));
		assertTrue(detachCalled.compareAndSet(true, false));

		// broadcasts WebSocket.TextMessage and notifies the listeners
		webSocketTester.sendMessage(expectedMessage);

		assertTrue(beginRequestCalled.get());
		assertTrue(endRequestCalled.get());
		assertTrue(detachCalled.get());

		webSocketTester.destroy();
	}

}
