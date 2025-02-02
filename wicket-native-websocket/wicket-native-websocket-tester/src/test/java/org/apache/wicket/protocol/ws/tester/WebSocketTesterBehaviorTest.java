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

import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;
import org.apache.wicket.protocol.ws.api.registry.PageIdKey;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for WebSocketTester.
 * Uses WebSocketBehavior.
 *
 * @since 6.0
 */
class WebSocketTesterBehaviorTest
{
	WicketTester tester;

	@BeforeEach
	public void before()
	{
		tester = new WicketTester();
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
	void sendTextMessageBehavior()
	{
		final String expectedMessage = "some message";

		WebSocketBehaviorTestPage page = new WebSocketBehaviorTestPage(expectedMessage);
		tester.startPage(page);

		WebSocketTester webSocketTester = new WebSocketTester(tester, page) {
			@Override
			protected void onOutMessage(String message)
			{
				assertEquals(Strings.capitalize(expectedMessage), message);
			}
		};

		webSocketTester.sendMessage(expectedMessage);
		webSocketTester.destroy();
	}

	/**
	 * A simple test that sends and receives a binary message.
	 * The page asserts that it received the correct message, offset and lenght and then
	 * pushes back the same message but capitalized, offset plus 1 and length minus 1.
	 */
	@Test
	void sendBinaryMessageBehavior()
	{
		final byte[] expectedMessage = "some message".getBytes(StandardCharsets.UTF_8);
		final int offset = 1;
		final int length = 2;

		WebSocketBehaviorTestPage page = new WebSocketBehaviorTestPage(expectedMessage, offset, length);
		tester.startPage(page);

		WebSocketTester webSocketTester = new WebSocketTester(tester, page)
		{
			@Override
			protected void onOutMessage(byte[] message, int off, int len)
			{
				String msg = new String(expectedMessage);
				byte[] pushedMessage = Strings.capitalize(msg).getBytes(StandardCharsets.UTF_8);

				assertArrayEquals(pushedMessage, message);
				assertEquals(offset + 1, off);
				assertEquals(length - 1, len);
			}
		};

		webSocketTester.sendMessage(expectedMessage, offset, length);
		webSocketTester.destroy();
	}

	@Test
	void serverSideBroadcast()
	{
		final String expectedMessage = "Broadcasted Message";
		final BroadcastMessage broadcastMessage = new BroadcastMessage(expectedMessage);
		final AtomicBoolean messageReceived = new AtomicBoolean(false);

		WebSocketBehaviorTestPage page = new WebSocketBehaviorTestPage(broadcastMessage);
		tester.startPage(page);

		WebSocketTester webSocketTester = new WebSocketTester(tester, page)
		{
			@Override
			protected void onOutMessage(String message)
			{
				assertEquals(expectedMessage.toUpperCase(), message);
				messageReceived.set(true);
			}
		};
		webSocketTester.broadcast(tester.getApplication(), tester.getHttpSession().getId(),
				new PageIdKey(page.getPageId(), page.getClass().getName()), broadcastMessage);

		assertTrue(messageReceived.get());
		webSocketTester.destroy();
	}

	static class BroadcastMessage implements IWebSocketPushMessage
	{
		private final String message;

		private BroadcastMessage(String message)
		{
			this.message = message;
		}

		public String getText()
		{
			return message;
		}
	}
}
