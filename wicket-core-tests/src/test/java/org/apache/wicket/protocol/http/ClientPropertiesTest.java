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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for ClientProperties that failed on Mac OS X Java platform.
 *
 * @author Martijn Dashorst
 */
class ClientPropertiesTest
{
	@ParameterizedTest
    @ValueSource(strings = {"-2.0", "+2.0", "+11.0", "+2.5", "-2.5", "3", "-3"})
    void testTimezone(String utc) {
        ClientProperties props = new ClientProperties();
        props.setUtcOffset(utc);

        String expectedTimeZoneId = getExpectedTimeZoneId(utc);
        assertEquals(TimeZone.getTimeZone(expectedTimeZoneId), props.getTimeZone());
    }

    private String getExpectedTimeZoneId(String utc) {
        // Mapping UTC offsets to expected TimeZone IDs
        switch (utc) {
            case "-2.0": return "GMT-2:00";
            case "+2.0": return "GMT+2:00";
            case "+11.0": return "GMT+11:00";
            case "+2.5": return "GMT+2:30";
            case "-2.5": return "GMT-2:30";
            case "3": return "GMT+3:00";
            case "-3": return "GMT-3:00";
            default: throw new IllegalArgumentException("Unexpected UTC offset: " + utc);
        }
    }

	/**
	 * WICKET-5396.
	 */
	@Test
	void integerToString()
	{
		ClientProperties props = new ClientProperties();

		assertFalse(props.toString().contains("browserHeight"));

		props.setBrowserHeight(666);

		assertTrue(props.toString().contains("browserHeight=666"));
	}

	/**
	 * WICKET-6689.
	 */
	@Test
	void timezoneAET()
	{
		ClientProperties props = new ClientProperties();
		props.setUtcOffset("11");
		props.setUtcDSTOffset("10");

		assertEquals(TimeZone.getTimeZone("AET"), props.getTimeZone());
	}

	/**
	 * jsTimeZone "positive" test
	 */
	@Test
	void timezoneJsPositive()
	{
		ClientProperties props = new ClientProperties();
		props.setJsTimeZone("Asia/Novosibirsk");

		assertEquals(TimeZone.getTimeZone("Asia/Novosibirsk"), props.getTimeZone());
	}

	/**
	 * jsTimeZone "negative" test
	 */
	@Test
	void timezoneJsNegative()
	{
		ClientProperties props = new ClientProperties();
		props.setUtcOffset("11");
		props.setUtcDSTOffset("10");
		props.setJsTimeZone("aaa");

		assertEquals(TimeZone.getTimeZone("AET"), props.getTimeZone());
	}
}
