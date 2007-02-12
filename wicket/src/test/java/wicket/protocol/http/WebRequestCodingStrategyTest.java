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
package wicket.protocol.http;

import junit.framework.TestCase;
import wicket.PageParameters;
import wicket.protocol.http.request.WebRequestCodingStrategy;
import wicket.request.target.component.BookmarkablePageRequestTarget;

/**
 * Tests for WebRequestCodingStrategy
 */
public class WebRequestCodingStrategyTest extends TestCase
{
	public void testDummy() {}

	/**
	 * WICKET-65 Handle String array in PageParameters
	 */
	public void bugTestEncodeStringArray() {
		WebRequestCodingStrategy wrcs = new WebRequestCodingStrategy();
		MockWebApplication app = new MockWebApplication("");
		app.setHomePage(MockPage.class);
		PageParameters params = new PageParameters();
		params.add("a", "1");
		params.add("a", "2");
		BookmarkablePageRequestTarget requestTarget = new BookmarkablePageRequestTarget(MockPage.class, params);
		app.setupRequestAndResponse();
		CharSequence cs = wrcs.encode(app.createRequestCycle(), requestTarget);
		assertEquals("?a=1&a=2", cs.toString());
	}
}
