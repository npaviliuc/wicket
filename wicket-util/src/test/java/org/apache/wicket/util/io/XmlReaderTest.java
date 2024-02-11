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
package org.apache.wicket.util.io;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("javadoc")
class XmlReaderTest
{
	@Test
	 void readHtmlFileWithoutAnyTags() throws Exception
	{
		XmlReader reader = new XmlReader(this.getClass().getResourceAsStream("test_1.html"), null);
		assertNull(reader.getEncoding());

		try (BufferedReader bufReader = new BufferedReader(reader);)
		{
			assertEquals("Zeile 1", bufReader.readLine());

			assertNull(bufReader.readLine());
		}
	}

	@Test
	 void readHtmlFileWithHtmlAndBody() throws Exception
	{
		XmlReader reader = new XmlReader(this.getClass().getResourceAsStream("test_2.html"), null);
		assertNull(reader.getEncoding());

		try (BufferedReader bufReader = new BufferedReader(reader);)
		{
			assertEquals("<html>", bufReader.readLine());
			assertEquals("<body>", bufReader.readLine());
		}
	}

	@Test
	 void readHtmlFileWithXmlPreambleSansVersionAndHtmlTag() throws Exception
	{
		XmlReader reader = new XmlReader(this.getClass().getResourceAsStream("test_3.html"), null);
		assertNull(reader.getEncoding());

		try (BufferedReader bufReader = new BufferedReader(reader);)
		{
			assertEquals("", bufReader.readLine().trim());
			assertEquals("<html>", bufReader.readLine());
			assertNull(bufReader.readLine());
		}
	}

	@Test
	 void readHtmlFileWithXmlPreambleWithVersionAndHtmlTag() throws Exception
	{
		XmlReader reader = new XmlReader(this.getClass().getResourceAsStream("test_4.html"), null);
		assertNull(reader.getEncoding());

		try (BufferedReader bufReader = new BufferedReader(reader);)
		{
			assertEquals("", bufReader.readLine().trim());
			assertEquals("<html>", bufReader.readLine());
			assertNull(bufReader.readLine());
		}
	}

	 private static Stream<String> testFiles() {
        return Stream.of("test_5.html", "test_6.html", "test_7.html", "test_8.html");
    }

    @ParameterizedTest
    @MethodSource("testFiles")
     void readHtmlFileWithXmlPreambleAndHtmlTag(String fileName) throws Exception {
        // Construct the test file path
        String filePath = getClass().getResource(fileName).getPath();

        // Use try-with-resources to close the reader automatically
        try (XmlReader reader = new XmlReader(getClass().getResourceAsStream(fileName), null);
             BufferedReader bufReader = new BufferedReader(reader)) {

            // Assert the encoding is UTF-8
            assertEquals("UTF-8", reader.getEncoding().toUpperCase());

            // Assert the first line is an empty string
            assertEquals("", bufReader.readLine().trim());

            // Assert the second line is <html>
            assertEquals("<html>", bufReader.readLine());

            // Assert the third line is null
            assertNull(bufReader.readLine());
        }
    }
}
