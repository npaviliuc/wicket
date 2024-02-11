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
package org.apache.wicket.core.util.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link JavaScriptStripper}
 *
 * @author <a href="mailto:jbq@apache.org">Jean-Baptiste Quenot</a>
 */
class JavaScriptStripperTest
{
	/**	 */
	@Test
	void unixWICKET501()
	{
		String s = new JavaScriptStripper().stripCommentsAndWhitespace("    // Handle the common XPath // expression\n    if ( !t.indexOf(\"//\") ) {");
		assertEquals(" \n if ( !t.indexOf(\"//\") ) {", s);
	}

	/**	 */
	@Test
	void dosWICKET501()
	{
		String s = new JavaScriptStripper().stripCommentsAndWhitespace("    // Handle the common XPath // expression\r\n    if ( !t.indexOf(\"//\") ) {");
		assertEquals(" \r\nif ( !t.indexOf(\"//\") ) {", s);
	}

	/**	 */
	@Test
	void macWICKET501()
	{
		String s = new JavaScriptStripper().stripCommentsAndWhitespace("    // Handle the common XPath // expression\r    if ( !t.indexOf(\"//\") ) {");
		assertEquals(" \r if ( !t.indexOf(\"//\") ) {", s);
	}

	/**	 */
	@Test
	void regexp()
	{
		String s = new JavaScriptStripper().stripCommentsAndWhitespace("    t = jQuery.trim(t).replace( /^\\/\\//i, \"\" );");
		assertEquals(" t = jQuery.trim(t).replace( /^\\/\\//i, \"\" );", s);
	}

	/**	 */
	@Test
	void regexp2()
	{
		String s = new JavaScriptStripper().stripCommentsAndWhitespace("foo.replace(/\"//*strip me*/, \"\"); // strip me\rdoFoo();");
		assertEquals("foo.replace(/\"/, \"\"); \rdoFoo();", s);
	}

	/**	 */
	@Test
	void regexp3()
	{
		String s = new JavaScriptStripper().stripCommentsAndWhitespace("parseFloat( elem.filter.match(/alpha\\(opacity=(.*)\\)/)[1] ) / 100 : 1;\r//foo");
		assertEquals("parseFloat( elem.filter.match(/alpha\\(opacity=(.*)\\)/)[1] ) / 100 : 1;\r",
			s);
	}

	@ParameterizedTest
    @ValueSource(strings = {
            " attr: /**/ //xyz\n /\\[((?:[\\w-]*:)?[\\w-]+)\\s*(?:([!^$*~|]?=)\\s*((['\"])([^\\4]*?)\\4|([^'\"][^\\]]*?)))?\\]/    after     regex",
            "a = [ /^(\\[) *@?([\\w-]+) *([!*$^~=]*) *('?\"?)(.*?)\\4 *\\]/ ];    b()",
            "   a   b   c",
            "   a \n  b   c\n\n",
            "return  this.__unbind__(type, fn);",
            "x++ //\nx++"
    })
    void testJavaScriptStripping(String before) {
        String after = new JavaScriptStripper().stripCommentsAndWhitespace(before);
        assertEquals(getExpectedResult(before), after);
    }

    // Add this method to provide expected results for each input string
    private String getExpectedResult(String input) {
        // Implement the logic to calculate the expected result for each input
        // For simplicity, you can manually define the expected results here
        // You might need to adjust these based on your actual requirements
        if (input.startsWith(" attr")) {
            return " attr:  \n /\\[((?:[\\w-]*:)?[\\w-]+)\\s*(?:([!^$*~|]?=)\\s*((['\"])([^\\4]*?)\\4|([^'\"][^\\]]*?)))?\\]/ after regex";
        } else if (input.startsWith("a = [")) {
            return "a = [ /^(\\[) *@?([\\w-]+) *([!*$^~=]*) *('?\"?)(.*?)\\4 *\\]/ ]; b()";
        } else if (input.startsWith("   a   b   c")) {
            return " a b c";
        } else if (input.startsWith("   a \n  b   c\n\n")) {
            return " a\nb c\n";
        } else if (input.startsWith("return  this.__unbind__(type, fn);")) {
            return "return this.__unbind__(type, fn);";
        } else if (input.startsWith("x++ //\nx++")) {
            return "x++ \nx++";
        } else {
            return "";
        }
    }	

	@Test
    void regexpDoubleSlash()
    {
	    String before = "return/[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)/gi.test(t);}}";
	    String after = new JavaScriptStripper().stripCommentsAndWhitespace(before);
	    assertEquals(before, after);
    }

	/**     */
	// @formatter:off
	private static String TESTSTRING2 =
         "   var test = function () {\n" +
         "   var c = \"!=\";\n" +
         "    /* from jquery 1.5.1 */\n" +
         "    if ( !l.match.PSEUDO.test(c) && !/!=/.test(c)) {\n" +
         "       alert(\"/something bad will happen */* \");\n" +
         "   }\n" +
         "\n" +
         "     var importantFunction = function () {alert(\"really important function \")}\n" +
         "   /*\n" +
         "     This code will be stripped\n" +
         "   */\n" +
         "\n" +
         "}" ;
	// @formatter:on

	/**	 */
	@Test
	void regExThatStartsWithExclamationMark()
	{
		String result = new JavaScriptStripper().stripCommentsAndWhitespace(TESTSTRING2);
		assertFalse(result.contains("This code will be stripped"));
		assertTrue(result.contains("something bad will happen"));
		assertTrue(result.contains("really important function"));

		System.out.println(result);
	}

	/**	*/
	@Test
	void templateLiteralWithTwoForwardSlashes() {
		final String before = "const url = `${protocol}//${hostname}:${port}`;";
		final String after = new JavaScriptStripper().stripCommentsAndWhitespace(before);

		assertEquals(before, after);
	}


}
