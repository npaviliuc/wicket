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
package org.apache.wicket.cdi;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request cycle listener that fires the {@link DetachEvent} event
 * 
 * @author igor
 * 
 */
public class DetachEventEmitter implements IRequestCycleListener
{
	private static final Logger logger = LoggerFactory.getLogger(DetachEventEmitter.class);

	private static final MetaDataKey<Boolean> DETACH_SCHEDULED_KEY = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	@Inject
	Event<DetachEvent> detachEvent;

	/**
	 * Constructor
	 */
	public DetachEventEmitter()
	{
		NonContextual.of(DetachEventEmitter.class).postConstruct(this);
	}

	@Override
	public void onRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler)
	{
		// this is a wicket request, schedule detach event to be fired

		cycle.setMetaData(DETACH_SCHEDULED_KEY, true);
	}

	@Override
	public void onDetach(RequestCycle cycle)
	{
		if (Boolean.TRUE.equals(cycle.getMetaData(DETACH_SCHEDULED_KEY)))
		{
			logger.debug("Firing Detach event...");

			detachEvent.fire(new DetachEvent());

			cycle.setMetaData(DETACH_SCHEDULED_KEY, null);
		}
	}
}
