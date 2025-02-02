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
package org.apache.wicket.protocol.http;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.wicket.util.WicketTestTag;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test for https://issues.apache.org/jira/browse/WICKET-6169
 */
@Tag(WicketTestTag.SLOW)
class RequestLoggerLiveSessionsTest
{
	private final RequestLogger requestLogger = new RequestLogger();
	
	private final ArrayList<String> sessionIds = new ArrayList<>();

	@Test
	void concurrentModification() {
		SessionCreateThread sct = new SessionCreateThread();
		SessionDestroyThread sdt = new SessionDestroyThread();
		sct.start();
		sdt.start();
		AtomicBoolean nullPointerExceptionThrown = new AtomicBoolean(false);

		int count = 10000000;

		while (count-- > 0)
		{
			try {
				requestLogger.getLiveSessions();
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
				nullPointerExceptionThrown.set(true);
				break;
			}
		}

		sct.interrupt();
		sdt.interrupt();

		if (nullPointerExceptionThrown.get()) {
			fail("The test should not fail with NullPointerException");
		}
	}
	
	private class SessionCreateThread extends Thread
	{
		private final Random random = new Random();
		
		public void run()
		{
			while (!isInterrupted())
			{
				if (sessionIds.size() < 50)
				{
					String sessionId = UUID.randomUUID().toString();
					synchronized (sessionIds) {
						sessionIds.add(sessionId);
					}
					requestLogger.sessionCreated(sessionId);
				}
				
				try
				{
					synchronized (sessionIds) {
						sessionIds.wait(random.nextInt(20));
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private class SessionDestroyThread extends Thread
	{
		private final Random random = new Random();
		
		public void run()
		{
			while (!isInterrupted())
			{
				if (sessionIds.size() > 0)
				{
					String sessionId = sessionIds.get(random.nextInt(sessionIds.size()));
					requestLogger.sessionDestroyed(sessionId);
					synchronized (sessionIds) {
						sessionIds.remove(sessionId);
					}
				}
				
				try
				{
					synchronized (sessionIds) {
						sessionIds.wait(random.nextInt(20));
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
