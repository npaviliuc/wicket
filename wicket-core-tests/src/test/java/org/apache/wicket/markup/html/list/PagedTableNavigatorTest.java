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
package org.apache.wicket.markup.html.list;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.util.tester.DiffUtil;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * Test for simple table behavior.
 */
class PagedTableNavigatorTest extends WicketTestCase
{

	/**
	 * Test simple table behavior.
	 * 
	 * @throws Exception
	 */
	@Test
	void pagedTable() throws Exception
	{
		tester.getSession().setLocale(Locale.ENGLISH);
		tester.startPage(PagedTableNavigatorPage.class);
		PagedTableNavigatorPage page = (PagedTableNavigatorPage)tester.getLastRenderedPage();
		String document = tester.getLastResponseAsString();

		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_1.html", true);

		Link<?> link = (Link<?>)page.get("navigator:first");
		
		linkTest1(link, page);

		link = (Link<?>)page.get("navigator:next");

		tester.clickLink(link.getPageRelativePath());

		document = tester.getLastResponseAsString();
		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_2.html", true);

		linkTest2(link, page);

		linkTest3(link, page, document);
		
		linkTest4(link, page, document);

		linkTest5(link, page, document);
		
		linkTest6(link, page, document);
		
		link = (Link<?>)page.get("navigator:prev");
		tester.clickLink(link.getPageRelativePath());
		document = tester.getLastResponseAsString();
		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_7.html", true);

		link = (Link<?>)page.get("navigator:first");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:prev");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:next");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:last");
		assertTrue(link.isEnabled());

		// add entries to the model list.
		@SuppressWarnings("unchecked")
		List<String> modelData = (List<String>)page.get("table").getDefaultModelObject();
		modelData.add("add-1");
		modelData.add("add-2");
		modelData.add("add-3");

		link = (Link<?>)page.get("navigator:first");
		tester.clickLink(link.getPageRelativePath());
		document = tester.getLastResponseAsString();
		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_8.html", true);
	}

	private void linkTest1(Link<?> link, PagedTableNavigatorPage page) {
		assertFalse(link.isEnabled());

		link = (Link<?>)page.get("navigator:prev");
		assertFalse(link.isEnabled());

		link = (Link<?>)page.get("navigator:next");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:last");
		assertTrue(link.isEnabled());
	}

	private void linkTest2(Link<?> link, PagedTableNavigatorPage page) {
		
		link = (Link<?>)page.get("navigator:first");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:prev");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:next");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:last");
		assertTrue(link.isEnabled());
	}

	private void linkTest3(Link<?> link, PagedTableNavigatorPage page, String document) throws Exception {
		link = (Link<?>)page.get("navigator:prev");
		tester.clickLink(link.getPageRelativePath());
		document = tester.getLastResponseAsString();
		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_3.html", true);

		link = (Link<?>)page.get("navigator:first");
		assertFalse(link.isEnabled());

		link = (Link<?>)page.get("navigator:prev");
		assertFalse(link.isEnabled());

		link = (Link<?>)page.get("navigator:next");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:last");
		assertTrue(link.isEnabled());

	}

	private void linkTest4(Link<?> link, PagedTableNavigatorPage page, String document) throws Exception {
		link = (Link<?>)page.get("navigator:last");
		tester.clickLink(link.getPageRelativePath());
		document = tester.getLastResponseAsString();
		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_4.html", true);

		link = (Link<?>)page.get("navigator:first");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:prev");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:next");
		assertFalse(link.isEnabled());

		link = (Link<?>)page.get("navigator:last");
		assertFalse(link.isEnabled());

	}

	private void linkTest5(Link<?> link, PagedTableNavigatorPage page, String document) throws Exception {
		link = (Link<?>)page.get("navigator:first");
		tester.clickLink(link.getPageRelativePath());
		document = tester.getLastResponseAsString();
		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_5.html", true);

		link = (Link<?>)page.get("navigator:first");
		assertFalse(link.isEnabled());

		link = (Link<?>)page.get("navigator:prev");
		assertFalse(link.isEnabled());

		link = (Link<?>)page.get("navigator:next");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:last");
		assertTrue(link.isEnabled());
	}

	private void linkTest6(Link<?> link, PagedTableNavigatorPage page, String document) throws Exception {
		link = (Link<?>)page.get("navigator:navigation:2:pageLink");
		tester.clickLink(link.getPageRelativePath());
		document = tester.getLastResponseAsString();
		DiffUtil.validatePage(document, this.getClass(),
			"PagedTableNavigatorExpectedResult_6.html", true);

		link = (Link<?>)page.get("navigator:first");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:prev");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:next");
		assertTrue(link.isEnabled());

		link = (Link<?>)page.get("navigator:last");
		assertTrue(link.isEnabled());
	}
}
