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
package org.apache.wicket.markup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Markup} class.
 */
class MarkupTest extends WicketTestCase
{
	@Test
	void testFind()
	{
		MarkupFactory markupFactory = tester.getApplication().getMarkupSettings().getMarkupFactory();
		Markup markup = markupFactory.getMarkup(new MarkupTest_Find_3(), false);

		IMarkupFragment childMarkup = null;

		/*
		 * Ensure we can find inside <head>
		 */
		testA1(markup, childMarkup);

		/*
		 * Ensure we can find in body
		 */
		testA2(markup, childMarkup);
		/*
		 * Ensure we cannot find inside component tag
		 */
		assertNull(markup.find("a3"));

		/*
		 * Ensure we can find after other component tag
		 */
		testA4(markup, childMarkup);

		/*
		 * Ensure we can find after wicket:child
		 */
		testA5(markup, childMarkup);

		/*
		 * Ensure we can find after fragment
		 */
		testA6(markup, childMarkup);

		/*
		 * Ensure we cannot find inside fragment
		 */
		assertNull(markup.find("a7"));

		/*
		 * Ensure we can find in subclass <wicket:head> section
		 */
		testB1(markup, childMarkup);

		/*
		 * Ensure we can find fragment in subclass
		 */
		testB2(markup, childMarkup);

		/*
		 * Ensure we cannot find inside fragment in subclass
		 */
		assertNull(markup.find("b3"));

		/*
		 * Ensure we can find in subclass <wicket:extend> section
		 */
		testB4(markup, childMarkup);

		/*
		 * Ensure we cannot find inside component tag in subclass
		 */
		assertNull(markup.find("b5"));

		/*
		 * Ensure we cannot find inside component tag in subclass after wicket:child
		 */
		assertNull(markup.find("b6"));

		/*
		 * Ensure we can find in subclass <wicket:head> section
		 */
		testC1(markup, childMarkup);

		/*
		 * Ensure we can find fragment in subclass
		 */
		testC2(markup, childMarkup);

		/*
		 * Ensure we cannot find inside fragment in subclass
		 */
		assertNull(markup.find("c3"));

		/*
		 * Ensure we can find in subclass <wicket:extend> section
		 */
		testC4(markup, childMarkup);

		/*
		 * Ensure we cannot find inside component tag in subclass
		 */
		assertNull(markup.find("c5"));
	}

	private void testA1(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("a1");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("a1", ((ComponentTag)mkFragment.get(0)).getId());
	}

	private void testA2(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("a2");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("a2", ((ComponentTag)mkFragment.get(0)).getId());
	}

	private void testA4(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("a4");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("a4", ((ComponentTag)mkFragment.get(0)).getId());
	}

	private void testA5(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("a5");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("a5", ((ComponentTag)mkFragment.get(0)).getId());
	}

	private void testA6(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("a6");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof WicketTag);
		assertEquals("a6", ((ComponentTag)mkFragment.get(0)).getId());
		assertTrue(((WicketTag)mkFragment.get(0)).isFragmentTag());
	}

	private void testB1(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("b1");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("b1", ((ComponentTag)mkFragment.get(0)).getId());
	}

	private void testB2(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("b2");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof WicketTag);
		assertEquals("b2", ((ComponentTag)mkFragment.get(0)).getId());
		assertTrue(((WicketTag)mkFragment.get(0)).isFragmentTag());
	}

	private void testB4(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("b4");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("b4", ((ComponentTag)mkFragment.get(0)).getId());
	}

	private void testC1(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("c1");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("c1", ((ComponentTag)mkFragment.get(0)).getId());
	}

	private void testC2(Markup mk, IMarkupFragment mkFragment) {
		mkFragment = mk.find("c2");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof WicketTag);
		assertEquals("c2", ((ComponentTag)mkFragment.get(0)).getId());
		assertTrue(((WicketTag)mkFragment.get(0)).isFragmentTag());
	}

	private void testC4(Markup mk, IMarkupFragment mkFragment) {
		mkFragment= mk.find("c4");
		assertNotNull(mkFragment);
		assertTrue(mkFragment.get(0) instanceof ComponentTag);
		assertEquals("c4", ((ComponentTag)mkFragment.get(0)).getId());
	}

}
