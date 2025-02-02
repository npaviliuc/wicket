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
package org.apache.wicket;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author jcompagner
 */
class TesterTest extends WicketTestCase
{
	/**
	 * 
	 */
	@Test
	void assertTest()
	{
		tester.startPage(new MyPage());
		tester.debugComponentTrees();
		try
		{
			tester.assertVisible("label");
			// If the label is visible, the assertion will pass, and this block won't be executed
       		// If the label is invisible, the assertion will throw AssertionError, which is what you want
		}
		catch (AssertionError e)
		{
		}
		catch (NullPointerException e)
		{
			fail("NullPointerException shouldn't be thrown, instead it must fail.");
		}
	}

	private static class MyPage extends WebPage
	{
		private static final long serialVersionUID = 1L;

		/**
		 * Construct.
		 */
		MyPage()
		{
			add(new Label("label")
			{

				private static final long serialVersionUID = 1L;

				@Override
				public boolean isVisible()
				{
					return false;
				}
			});
		}

	}


}
