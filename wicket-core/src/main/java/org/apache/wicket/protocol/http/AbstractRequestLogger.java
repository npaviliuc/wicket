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

import static java.lang.System.arraycopy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class that collects request and session information for request logging to enable rich
 * information about the events that transpired during a single request. Typical HTTPD and/or
 * Servlet container log files are unusable for determining what happened in the application since
 * they contain the requested URLs of the form http://example.com/app?wicket:interface:0:0:0, which
 * doesn't convey any useful information. Requestloggers can show which page was the target of the
 * request, and which page was rendered as a response, and anything else: resources, Ajax request,
 * etc.
 * <p>
 * The information in the log files can take any format, depending on the request logger
 * implementation: currently Wicket supports two formats: a {@link RequestLogger legacy, log4j
 * compatible format}, and a <em>JsonRequestLogger</em> JSON format.
 */
public abstract class AbstractRequestLogger implements IRequestLogger
{
	private static final Logger LOG = LoggerFactory.getLogger(AbstractRequestLogger.class);

	private static final ZoneId ZID = ZoneId.of("GMT");
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

	/**
	 * Key for storing request data in the request cycle's meta data.
	 */
	private static MetaDataKey<RequestData> newRequestData = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	/**
	 * Key for storing session data in the request cycle's meta data.
	 */
	private static MetaDataKey<SessionData> sessionData2 = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	private final AtomicInteger totalCreatedSessions = new AtomicInteger();

	private final AtomicInteger peakSessions = new AtomicInteger();

	private final Map<String, SessionData> liveSessions;

	private final AtomicInteger activeRequests = new AtomicInteger();

	private final AtomicInteger peakActiveRequests = new AtomicInteger();

	/**
	 * Rounded request buffer that keeps the request data for the last N requests in the buffer.
	 */
	private RequestData[] requestWindow;

	/**
	 * A special object used as a lock before accessing {@linkplain #requestWindow}.
	 * Needed because {@linkplain #requestWindow} is being reassigned in some cases,
	 * e.g. {@link #resizeBuffer()}
	 */
	private final Object requestWindowLock = new Object();

	/**
	 * Cursor pointing to the current writable location in the buffer. Points to the first empty
	 * slot or if the buffer has been filled completely to the oldest request in the buffer.
	 */
	private int indexInWindow = 0;

	/**
	 * records the total request time across the sliding request window so that it can be used to
	 * calculate the average request time across the window duration.
	 */
	private long totalRequestTime = 0l;

	/**
	 * records the start time of the oldest request across the sliding window so that it can be used
	 * to calculate the average request time across the window duration.
	 */
	private Date startTimeOfOldestRequest;

	/**
	 * Construct.
	 */
	public AbstractRequestLogger()
	{
		int requestsWindowSize = getRequestsWindowSize();
		requestWindow = new RequestData[requestsWindowSize];
		liveSessions = new ConcurrentHashMap<>();
	}

	@Override
	public int getCurrentActiveRequestCount()
	{
		return activeRequests.get();
	}

	@Override
	public int getPeakActiveRequestCount()
	{
		return peakActiveRequests.get();
	}

	@Override
	public SessionData[] getLiveSessions()
	{
		final SessionData[] sessions = liveSessions.values().toArray(new SessionData[0]);
		Arrays.sort(sessions);
		return sessions;
	}

	@Override
	public int getPeakSessions()
	{
		return peakSessions.get();
	}

	@Override
	public List<RequestData> getRequests()
	{
		synchronized (requestWindowLock)
		{
			RequestData[] result = new RequestData[hasBufferRolledOver() ? requestWindow.length
				: indexInWindow];
			copyRequestsInOrder(result);
			return Arrays.asList(result);
		}
	}

	/**
	 * Copies all request data into {@code copy} such that the oldest request is in slot 0 and the
	 * most recent request is in slot {@code copy.length}
	 * 
	 * @param copy
	 *            the target, has to have a capacity of at least {@code requestWindow.length}
	 */
	private void copyRequestsInOrder(RequestData[] copy)
	{
		int destPos = 0;
		
		if (hasBufferRolledOver())
		{
			destPos = requestWindow.length - indexInWindow;
			
			// first copy the oldest requests stored behind the cursor into the copy
			arraycopy(requestWindow, indexInWindow, copy, 0, destPos);
		}
		
		arraycopy(requestWindow, 0, copy, destPos, indexInWindow);
	}

	/**
	 * @return whether the buffer has been filled to capacity at least once
	 */
	private boolean hasBufferRolledOver()
	{
		return requestWindow.length > 0 && requestWindow[requestWindow.length - 1] != null;
	}

	@Override
	public int getTotalCreatedSessions()
	{
		return totalCreatedSessions.get();
	}

	@Override
	public void objectCreated(Object value)
	{
	}

	@Override
	public void objectRemoved(Object value)
	{
	}

	@Override
	public void objectUpdated(Object value)
	{
	}

	@Override
	public void requestTime(long timeTaken) {
		RequestData newRequestData = getRequestData();
		
		if (newRequestData != null) {
			updateActiveRequests(newRequestData);
			updateRequestData(newRequestData, timeTaken);
			updateSessionData(newRequestData);
		}
	}

	private RequestData getRequestData() {
		return RequestCycle.get().getMetaData(newRequestData);
	}

	private void updateActiveRequests(RequestData newRequestData) {
		if (activeRequests.get() > 0) {
			newRequestData.setActiveRequest(activeRequests.decrementAndGet());
		}
	}

	private void updateRequestData(RequestData newRequestData, long timeTaken) {
		Session session = Session.exists() ? Session.get() : null;
		String sessionId = session != null ? session.getId() : "N/A";
		newRequestData.setSessionId(sessionId);

		Object sessionInfo = getSessionInfo(session);
		newRequestData.setSessionInfo(sessionInfo);

		long sizeInBytes = getSessionSize(session);
		newRequestData.setSessionSize(sizeInBytes);
		newRequestData.setTimeTaken(timeTaken);

		addRequest(newRequestData);
	}

	private long getSessionSize(Session session) {
		long sizeInBytes = -1;
		if (Application.exists() && Application.get().getRequestLoggerSettings().getRecordSessionSize()) {
			try {
				sizeInBytes = session != null ? session.getSizeInBytes() : -1;
			} catch (Exception e) {
				LOG.error("Exception while determining the size of the session in the request logger: " +
						e.getMessage(), e);
			}
		}
		return sizeInBytes;
	}

	private void updateSessionData(RequestData newRequestData) {
		String sessionId = newRequestData.getSessionId();
		if (sessionId != null) {
			SessionData sessionData = updateSessionData(newRequestData, sessionId);
			RequestCycle.get().setMetaData(sessionData2, sessionData);
		}
	}

	private SessionData updateSessionData(RequestData newRequestData, String sessionId) {
		SessionData sessionData = liveSessions.get(sessionId);
		if (sessionData == null) {
			sessionCreated(sessionId);
			sessionData = liveSessions.get(sessionId);
		}
		if (sessionData != null) {
			sessionData.setSessionInfo(newRequestData.getSessionInfo());
			sessionData.setSessionSize(newRequestData.getSessionSize());
			sessionData.addTimeTaken(newRequestData.getTimeTaken());
		}
		return sessionData;
	}

	@Override
	public void sessionCreated(String sessionId)
	{
		liveSessions.put(sessionId, new SessionData(sessionId));
		if (liveSessions.size() > peakSessions.get())
		{
			peakSessions.set(liveSessions.size());
		}
		totalCreatedSessions.incrementAndGet();
	}

	@Override
	public void sessionDestroyed(String sessionId)
	{
		RequestCycle requestCycle = RequestCycle.get();
		SessionData sessionData = liveSessions.remove(sessionId);
		if (requestCycle != null)
			requestCycle.setMetaData(sessionData2, sessionData);
	}

	@Override
	public RequestData getCurrentRequest()
	{
		RequestCycle requestCycle = RequestCycle.get();
		RequestData rd = requestCycle.getMetaData(newRequestData);
		if (rd == null)
		{
			rd = new RequestData();
			requestCycle.setMetaData(newRequestData, rd);
			int activeCount = activeRequests.incrementAndGet();

			if (activeCount > peakActiveRequests.get())
			{
				peakActiveRequests.set(activeCount);
			}
		}
		return rd;
	}

	@Override
	public void performLogging()
	{
		RequestData requestdata = RequestCycle.get().getMetaData(newRequestData);
		if (requestdata != null)
		{
			// log the request- and sessiondata (the latter can be null)
			SessionData sessiondata = RequestCycle.get().getMetaData(sessionData2);
			log(requestdata, sessiondata);
		}
	}

	protected abstract void log(RequestData rd, SessionData sd);

	private Object getSessionInfo(Session session)
	{
		if (session instanceof ISessionLogInfo)
		{
			return ((ISessionLogInfo)session).getSessionInfo();
		}
		return "";
	}

	protected void addRequest(RequestData rd)
	{
		// ensure the buffer has the proper installed length
		resizeBuffer();

		synchronized (requestWindowLock)
		{
			// if the requestWindow is a zero-length array, nothing gets stored
			if (requestWindow.length == 0)
				return;

			// use the oldest request data to recalculate the average request time
			RequestData old = requestWindow[indexInWindow];

			// replace the oldest request with the nweset request
			requestWindow[indexInWindow] = rd;

			// move the cursor to the next writable position containing the oldest request or if the
			// buffer hasn't been filled completely the first empty slot
			indexInWindow = (indexInWindow + 1) % requestWindow.length;
			if (old != null)
			{
				startTimeOfOldestRequest = requestWindow[indexInWindow].getStartDate();
				totalRequestTime -= old.getTimeTaken();
			}
			else
			{
				if (startTimeOfOldestRequest == null)
					startTimeOfOldestRequest = rd.getStartDate();
			}
			totalRequestTime += rd.getTimeTaken();
		}
	}

	private int getWindowSize()
	{
		if (requestWindow[requestWindow.length - 1] == null)
			return indexInWindow;
		else
			return requestWindow.length;
	}

	@Override
	public long getAverageRequestTime()
	{
		synchronized (requestWindowLock)
		{
			int windowSize = getWindowSize();
			if (windowSize == 0)
				return 0;
			return totalRequestTime / windowSize;
		}
	}

	@Override
	public long getRequestsPerMinute()
	{
		synchronized (requestWindowLock)
		{
			int windowSize = getWindowSize();
			if (windowSize == 0)
				return 0;
			long start = startTimeOfOldestRequest.getTime();
			long end = System.currentTimeMillis();
			double diff = (double)end - start;
			return Math.round(windowSize / (diff / 60000.0));
		}
	}

	@Override
	public void logEventTarget(IRequestHandler requestHandler)
	{
		RequestData newRequestData = getCurrentRequest();
		if (newRequestData != null)
		{
			newRequestData.setEventTarget(requestHandler);
		}
	}

	@Override
	public void logRequestedUrl(String url)
	{
		getCurrentRequest().setRequestedUrl(url);
	}

	@Override
	public void logResponseTarget(IRequestHandler requestHandler)
	{
		RequestData newRequestData = getCurrentRequest();
		if (newRequestData != null)
			newRequestData.setResponseTarget(requestHandler);
	}

	/**
	 * Resizes the request buffer to match the
	 * {@link org.apache.wicket.settings.RequestLoggerSettings#getRequestsWindowSize() configured window size}
	 */
	private void resizeBuffer()
	{
		int newCapacity = getRequestsWindowSize();

		// do nothing if the capacity requirement hasn't changed
		if (newCapacity == requestWindow.length)
			return;

		RequestData[] newRequestWindow = new RequestData[newCapacity];
		synchronized (requestWindowLock)
		{
			int oldCapacity = requestWindow.length;
			int oldNumberOfElements = hasBufferRolledOver() ? oldCapacity : indexInWindow;

			if (newCapacity > oldCapacity)
			{
				// increase the capacity of the buffer when more requests need to be stored
				// and preserve the order of the requests while copying them into the new buffer.
				copyRequestsInOrder(newRequestWindow);

				// the next writable position is at the first non-copied element in the buffer
				indexInWindow = oldNumberOfElements;
				requestWindow = newRequestWindow;
			}
			else if (newCapacity < oldCapacity)
			{
				// sort the requests in the current buffer such that the oldest request is in slot 0
				RequestData[] sortedRequestWindow = new RequestData[oldCapacity];
				copyRequestsInOrder(sortedRequestWindow);

				// determine the number of elements that need to be copied into the smaller target
				int numberOfElementsToCopy = Math.min(newCapacity, oldNumberOfElements);

				// determine the position from where the copying must start
				int numberOfElementsToSkip = Math.max(0, oldNumberOfElements -
					numberOfElementsToCopy);

				// fill the new buffer with the leftovers of the old buffer, skipping the oldest
				// requests
				arraycopy(sortedRequestWindow, numberOfElementsToSkip, newRequestWindow, 0,
					numberOfElementsToCopy);

				// the next writable position is 0 when the buffer is filled to capacity, or the
				// number of copied elements when the buffer isn't filled to capacity.
				indexInWindow = numberOfElementsToCopy >= newCapacity ? 0 : numberOfElementsToCopy;
				requestWindow = newRequestWindow;
			}
		}
	}


	/**
	 * Thread-safely formats the passed date in format 'yyyy-MM-dd hh:mm:ss,SSS' with GMT timezone
	 * 
	 * @param date
	 *            the date to format
	 * @return the formatted date
	 */
	protected String formatDate(final Date date)
	{
		Args.notNull(date, "date");

		LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZID);
		return ldt.format(FORMATTER);
	}

	private int getRequestsWindowSize()
	{
		int requestsWindowSize = 0;
		if (Application.exists())
		{
			requestsWindowSize = Application.get()
				.getRequestLoggerSettings()
				.getRequestsWindowSize();
		}
		return requestsWindowSize;
	}
}
