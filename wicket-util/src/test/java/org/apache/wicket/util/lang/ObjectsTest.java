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
package org.apache.wicket.util.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Objects class.
 * 
 * @author Martijn Dashorst
 */
class ObjectsTest
{
	/**
	 * Test method for 'org.apache.wicket.util.lang.Objects.areEquals(Object, Object)'
	 */
	@Test
	public void equal()
	{
		Object object = new Object();
		assertTrue(Objects.areEquals(object, object));

		assertFalse(Objects.areEquals(null, object));
		assertFalse(Objects.areEquals(object, null));
		assertTrue(Objects.areEquals(null, null));

		assertFalse(Objects.areEquals(new Object(), new Object()));
		assertTrue(Objects.areEquals(1, 1));
		assertFalse(Objects.areEquals("1", 1));
		assertFalse(Objects.areEquals(1, "1"));
		assertTrue(Objects.areEquals("1", Integer.toString(1)));
		assertTrue(Objects.areEquals(Integer.toString(1), "1"));
	}
}
