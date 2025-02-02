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
package org.apache.wicket.util.convert.converter;

import org.apache.wicket.util.convert.ConversionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link ZonedDateTimeConverter}
 */
class ZonedDateTimeConverterTest
{
	private final ZoneId zoneUCT = ZoneId.of("Etc/UCT");
	private final ZoneId zoneUTC = ZoneId.of("Etc/UTC");

	@Test
	@EnabledOnJre({JRE.JAVA_17, JRE.JAVA_18, JRE.JAVA_19})
	void convertToString_upto_jdk19() {
		ZonedDateTimeConverter converter = new ZonedDateTimeConverter();
		String date = converter.convertToString(ZonedDateTime.of(2016, 7, 11, 1, 2, 3, 0, zoneUCT), Locale.ENGLISH);
		assertEquals("Jul 11, 2016, 1:02:03 AM Coordinated Universal Time", date);
	}

	@Test
	@EnabledOnJre({JRE.JAVA_20, JRE.JAVA_21}) // See https://bugs.openjdk.org/browse/JDK-8304925
	void convertToString_jdk20_and_newer() {
		ZonedDateTimeConverter converter = new ZonedDateTimeConverter();
		String date = converter.convertToString(ZonedDateTime.of(2016, 7, 11, 1, 2, 3, 0, zoneUCT), Locale.ENGLISH);
		assertEquals("Jul 11, 2016, 1:02:03 AM Coordinated Universal Time", date);
	}

	@Test
	@EnabledOnJre({JRE.JAVA_17, JRE.JAVA_18, JRE.JAVA_19})
	void convertToObject_upto_jdk19() {
		ZonedDateTimeConverter converter = new ZonedDateTimeConverter();
		ZonedDateTime date = converter.convertToObject("Jul 11, 2016, 1:02:03 AM Coordinated Universal Time", Locale.ENGLISH);
		assertEquals(ZonedDateTime.of(2016, 7, 11, 1, 2, 3, 0, zoneUTC), date);
	}

	@Test
	@EnabledOnJre({JRE.JAVA_20, JRE.JAVA_21}) // See https://bugs.openjdk.org/browse/JDK-8304925
	void convertToObject_jdk20_and_newer() {
		ZonedDateTimeConverter converter = new ZonedDateTimeConverter();
		ZonedDateTime date = converter.convertToObject("Jul 11, 2016, 1:02:03 AM Coordinated Universal Time", Locale.ENGLISH);
		assertEquals(ZonedDateTime.of(2016, 7, 11, 1, 2, 3, 0, zoneUTC), date);
	}

	@Test
	void convertFails() {
		ZonedDateTimeConverter converter = new ZonedDateTimeConverter();

		try {
			converter.convertToObject("aaa", Locale.ENGLISH);
		} catch (ConversionException expected) {
		}
	}
}
