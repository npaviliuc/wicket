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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.Collection;

import org.apache.wicket.core.util.resource.locator.IResourceStreamLocator;
import org.apache.wicket.core.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.pages.PageExpiredErrorPage;
import org.apache.wicket.markup.parser.XmlTag.TagType;
import org.apache.wicket.markup.parser.filter.WicketTagIdentifier;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test cases for markup parser.
 * 
 * @author Jonathan Locke
 */
final class MarkupParserTest extends WicketTestCase
{
	private static final Logger log = LoggerFactory.getLogger(MarkupParserTest.class);

	/**
	 * 
	 * @throws StringValueConversionException
	 * @throws Exception
	 */
	@Test
	void tagParsing() throws Exception
	{
		final MarkupParser parser = new MarkupParser(
			"This is a test <a componentName:id=\"a\" href=\"foo.html\"> <b componentName:id=\"b\">Bold!</b> "
				+ "<img componentName:id=\"img\" width=9 height=10 src=\"foo\"> <marker componentName:id=\"marker\"/> </a>");
		parser.setWicketNamespace("componentName");

		final IMarkupFragment markup = parser.parse();
		final MarkupStream markupStream = new MarkupStream(markup);
		final ComponentTag aOpen = (ComponentTag)markupStream.next();

		log.info("", aOpen);
		assertEquals("a", aOpen.getName());
		assertEquals("foo.html", aOpen.getAttributes().getString("href"));

		markupStream.next();

		final ComponentTag boldOpen = (ComponentTag)markupStream.next();

		log.info("", boldOpen);
		assertEquals("b", boldOpen.getName());
		assertEquals(TagType.OPEN, boldOpen.getType());

		markupStream.next();

		final ComponentTag boldClose = (ComponentTag)markupStream.next();

		log.info("", boldClose);
		assertEquals("b", boldClose.getName());
		assertEquals(TagType.CLOSE, boldClose.getType());

		markupStream.next();

		final ComponentTag img = (ComponentTag)markupStream.next();

		log.info("", img);
		assertEquals("img", img.getName());
		assertEquals(9, img.getAttributes().getInt("width"));
		assertEquals(10, img.getAttributes().getInt("height"));
		assertEquals(TagType.OPEN, img.getType());

		markupStream.next();

		final ComponentTag marker = (ComponentTag)markupStream.next();

		log.info("", marker);
		assertEquals("marker", marker.getName());
		assertEquals(TagType.OPEN_CLOSE, marker.getType());

		markupStream.next();

		final ComponentTag aClose = (ComponentTag)markupStream.next();

		log.info("", aClose);
		assertEquals("a", aClose.getName());

		assertNull(markupStream.next());
	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	final void test1() throws Exception
	{
		final MarkupParser parser = new MarkupParser(
			"This is a test <a componentName:id=9> <b>bold</b> <b componentName:id=10></b></a> of the emergency broadcasting system");
		parser.setWicketNamespace("componentName");
		final IMarkupFragment tokens = parser.parse();

		log.info("tok(0)=" + tokens.get(0));
		log.info("tok(1)=" + tokens.get(1));
		log.info("tok(2)=" + tokens.get(2));
		log.info("tok(3)=" + tokens.get(3));
		log.info("tok(4)=" + tokens.get(4));
		log.info("tok(5)=" + tokens.get(5));

		assertEquals("This is a test ", tokens.get(0).toString());

		final ComponentTag a = (ComponentTag)tokens.get(1);

		assertEquals(9, a.getAttributes().getInt("componentName:id"));
		assertEquals(" <b>bold</b> ", tokens.get(2).toString());

		final ComponentTag b = (ComponentTag)tokens.get(3);

		assertEquals(10, b.getAttributes().getInt("componentName:id"));

		final ComponentTag closeA = (ComponentTag)tokens.get(5);

		assertEquals("a", closeA.getName());
		assertEquals(" of the emergency broadcasting system", tokens.get(6).toString());
	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	void xhtmlDocument() throws Exception
	{
		final String docText = ""
			+ "<?xml version='1.0' encoding='iso-8859-1' ?>"
			+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">"
			+ "<html>" + "<head><title>Some Page</title></head>"
			+ "<body><h1>XHTML Test</h1></body>" + "</html>";
		final MarkupParser parser = new MarkupParser(docText);
		parser.setWicketNamespace("componentName");
		final IMarkupFragment tokens = parser.parse();

		log.info("tok(0)=" + tokens.get(0));

		// without HtmlHeaderSectionHandler
		assertEquals(docText.substring(44), tokens.get(0).toString());
		// with HtmlHeaderSectionHandler
		// assertEquals(docText.substring(44, 147),
		// tokens.get(0).toString());
	}

	private MarkupResourceStream newMarkupResourceStream(final IResourceStreamLocator locator,
		final Class<?> cls, final String style, final String variation, final Locale locale,
		final String extension)
	{
		final String path = cls.getName().replace('.', '/');
		final IResourceStream resource = locator.locate(cls, path, style, variation, locale,
			extension, false);

		return new MarkupResourceStream(resource, null, null);
	}

	/**
	 * 
	 * @throws ParseException
	 * @throws ResourceStreamNotFoundException
	 * @throws IOException
	 */
	@Test
	void fileDocument() throws ParseException, ResourceStreamNotFoundException, IOException
	{
		IResourceStreamLocator locator = new ResourceStreamLocator();
		MarkupResourceStream resource = newMarkupResourceStream(locator, getClass(), "1", null,
			null, "html");

		MarkupParser parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");

		IMarkupFragment tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "2", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "3", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "4", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		// File from jar (URL resource)
		resource = newMarkupResourceStream(locator, PageExpiredErrorPage.class, null, null, null,
			"html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "5", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "6", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "7", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "8", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());

		resource = newMarkupResourceStream(locator, getClass(), "9", null, null, "html");
		parser = new MarkupParser(resource);
		parser.setWicketNamespace("wcn");
		tokens = parser.parse();
		log.info("tok(0)=" + tokens.get(0));
		// assertEquals(docText, tokens.get(0).toString());
	}

	/**
	 * Test &lt;wicket: .
	 * 
	 * @throws ParseException
	 * @throws ResourceStreamNotFoundException
	 * @throws IOException
	 */
	@Test
	void wicketTag() throws ParseException, ResourceStreamNotFoundException, IOException
	{
		new MarkupParser("<span wicket:id=\"test\"/>").parse();
		new MarkupParser("<span wicket:id=\"test\">Body</span>").parse();
		new MarkupParser("This is a test <span wicket:id=\"test\"/>").parse();
		new MarkupParser("This is a test <span wicket:id=\"test\">Body</span>").parse();
		new MarkupParser("<a wicket:id=\"[autolink]\" href=\"test.html\">Home</a>").parse();

		new MarkupParser("<wicket:body/>").parse();
		new MarkupParser("<wicket:border/>").parse();
		new MarkupParser("<wicket:panel/>").parse();

		try
		{
			new MarkupParser("<wicket:remove/>").parse();
			assertTrue(false, "Should have thrown an exception");
		}
		catch (MarkupException ex)
		{
			assertTrue(ex.getMessage()
				.startsWith(
					"Wicket remove tag must not be an open-close tag: '<wicket:remove/>' (line 1, column 1)"));
		}

		IMarkupFragment markup = new MarkupParser("<wicket:remove>  </wicket:remove>").parse();
		assertEquals(0, markup.size());

		markup = new MarkupParser("<wicket:remove> <span id=\"test\"/> </wicket:remove>").parse();
		assertEquals(0, markup.size());

		markup = new MarkupParser("<div><wicket:remove> <span id=\"test\"/> </wicket:remove></div>").parse();
		assertEquals(2, markup.size());
		assertEquals("<div>", markup.get(0).toString());
		assertEquals("</div>", markup.get(1).toString());

		try
		{
			new MarkupParser("<wicket:remove> <wicket:remove> </wicket:remove> </wicket:remove>").parse();
			assertTrue(false, "Should have thrown an exception: remove regions must not contain wicket-components");
		}
		catch (MarkupException ex)
		{
			assertTrue(ex.getMessage()
				.startsWith(
					"Markup remove regions must not contain Wicket component tags: '<wicket:remove>' (line 1, column 17)"));
		}

		new MarkupParser(
			"<wicket:panel><div id=\"definitionsContentBox\"><span wicket:id=\"contentPanel\"/></div></wicket:panel>").parse();
	}

	/**
	 * Test &lt;wicket: .
	 * 
	 * @throws ParseException
	 * @throws ResourceStreamNotFoundException
	 * @throws IOException
	 */
	@Test
	void defaultWicketTag() throws ParseException, ResourceStreamNotFoundException,
		IOException
	{
		MarkupParser parser = new MarkupParser("<image wcn:id=\"test\"/>");
		parser.setWicketNamespace("wcn");

		IMarkupFragment markup = parser.parse();
		assertEquals(1, markup.size());

		markup = new MarkupParser("<image wicket:id=\"test\"/>").parse();
		assertEquals(1, markup.size());

		WicketTagIdentifier.registerWellKnownTagName("xxx");
		parser = new MarkupParser("<wcn:xxx>  </wcn:xxx>");
		parser.setWicketNamespace("wcn");
		markup = parser.parse();
		assertEquals(3, markup.size());
	}
	
	private static Collection<Object[]> scriptTestData() {
        return Arrays.asList(new Object[][] {
            {"<script language=\"JavaScript\">... <x a> ...</script>", 5, "html", "html", "\n/*<![CDATA[*/\n... <x a> ...\n/*]]>*/\n"},
            {"<script type=\"text/x-jquery-tmpl\" language=\"JavaScript\">... <x a/> ...</script>", 5, "html", "html", "... <x a/> ..."},
            {"<script type='text/x-jquery-tmpl' language=\"JavaScript\">... <x a/> ...</script>", 5, "html", "html", "... <x a/> ..."}
        });
    }

    @ParameterizedTest
    @MethodSource("scriptTestData")
    void script(String inputMarkup, int expectedSize, String firstTagName, String lastTagName, String expectedContent)
            throws ParseException, ResourceStreamNotFoundException, IOException {
        final MarkupParser parser = new MarkupParser("<html wicket:id=\"test\">" + inputMarkup + "</html>");

        IMarkupFragment markup = parser.parse();
        assertEquals(expectedSize, markup.size());
        assertEquals(firstTagName, ((ComponentTag) markup.get(0)).getName());
        assertEquals(lastTagName, ((ComponentTag) markup.get(expectedSize - 1)).getName());
        assertEquals(expectedContent, markup.get(2).toString());
    }

	/**
	 * 
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 */
	@Test
	void balancing() throws IOException, ResourceStreamNotFoundException
	{
		final MarkupParser parser = new MarkupParser(
			"<span wicket:id=\"span\"><img wicket:id=\"img\"><span wicket:id=\"span2\"></span></span>");

		// Note: <img> is one of these none-balanced HTML tags
		IMarkupFragment markup = parser.parse();

		ComponentTag t = (ComponentTag)markup.get(0);
		assertEquals(t.getId(), "span");

		t = (ComponentTag)markup.get(1);
		assertEquals(t.getId(), "img");

		t = (ComponentTag)markup.get(2);
		assertEquals(t.getId(), "span2");
	}

	/**
	 * 
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 */
	@Test
	void comments() throws IOException, ResourceStreamNotFoundException
	{
		tester.getApplication().getMarkupSettings().setStripComments(true);
		final MarkupParser parser = new MarkupParser(
			"<span><!-- c1 --> <!-- c2 --><!-- c3 --></span>");
		IMarkupFragment markup = parser.parse();

		RawMarkup raw = (RawMarkup)markup.get(0);
		assertEquals("<span> </span>", raw.toString());
	}

	/**
	 * <a href="https://issues.apache.org/jira/browse/WICKET-3648">WICKET-3648</a>
	 * 
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 */
	@Test
	void commentsWithNestedElements() throws IOException, ResourceStreamNotFoundException
	{
		tester.getApplication().getMarkupSettings().setStripComments(true);
		final MarkupParser parser = new MarkupParser(
// @formatter:off
			"<span><!--[if lt IE 8 ]>\n"
			+ "<script src=\"js/ie7.js\"></script>\n" + 
			"<![endif]--></span>"
			// @formatter:on
		);
		IMarkupFragment markup = parser.parse();

		String parsedMarkup = markup.toString(true);
		assertEquals("<span><!--[if lt IE 8 ]>\n" + "<script src=\"js/ie7.js\"></script>\n"
			+ "<![endif]--></span>", parsedMarkup);
	}

	/**
	 * 
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 */
	@Test
	void script1() throws IOException, ResourceStreamNotFoundException
	{
		IMarkupFragment markup = new MarkupParser("<script/>").parse();
		assertEquals(1, markup.size());
		MarkupElement tag = markup.get(0);
		assertEquals("<script/>", tag.toString());

		markup = new MarkupParser("<script></script>").parse();
		assertEquals(2, markup.size());
		tag = markup.get(0);
		assertEquals("<script>", tag.toString());
		tag = markup.get(1);
		assertEquals("</script>", tag.toString());

		markup = new MarkupParser("<script> text </script>").parse();
		assertEquals(3, markup.size());
		tag = markup.get(0);
		assertEquals("<script>", tag.toString());
		tag = markup.get(1);
		assertEquals("\n/*<![CDATA[*/\n text \n/*]]>*/\n", tag.toString());
		tag = markup.get(2);
		assertEquals("</script>", tag.toString());

		markup = new MarkupParser("<script><!-- text --></script>").parse();
		assertEquals(3, markup.size());
		tag = markup.get(0);
		assertEquals("<script>", tag.toString());
		tag = markup.get(1);
		assertEquals("<!-- text -->", tag.toString());
		tag = markup.get(2);
		assertEquals("</script>", tag.toString());

		markup = new MarkupParser("<script> <!-- text --> </script>").parse();
		assertEquals(3, markup.size());
		tag = markup.get(0);
		assertEquals("<script>", tag.toString());
		tag = markup.get(1);
		assertEquals(" <!-- text --> ", tag.toString());
		tag = markup.get(2);
		assertEquals("</script>", tag.toString());

		scriptTest1(markup, tag);

	}

	private void scriptTest1(IMarkupFragment markup, MarkupElement tag) throws IOException, ResourceStreamNotFoundException {
		markup = new MarkupParser("<style><![CDATA[ text ]]></style>").parse();
		assertEquals(3, markup.size());
		tag = markup.get(0);
		assertEquals("<style>", tag.toString());
		tag = markup.get(1);
		assertEquals("<![CDATA[ text ]]>", tag.toString());
		tag = markup.get(2);
		assertEquals("</style>", tag.toString());

		markup = new MarkupParser("<html><script> text </script></html>").parse();
		assertEquals(5, markup.size());
		tag = markup.get(1);
		assertEquals("<script>", tag.toString());
		tag = markup.get(2);
		assertEquals("\n/*<![CDATA[*/\n text \n/*]]>*/\n", tag.toString());
		tag = markup.get(3);
		assertEquals("</script>", tag.toString());

		markup = new MarkupParser("<html wicket:id='xx'><script> text </script></html>").parse();
		assertEquals(5, markup.size());
		tag = markup.get(1);
		assertEquals("<script>", tag.toString());
		tag = markup.get(2);
		assertEquals("\n/*<![CDATA[*/\n text \n/*]]>*/\n", tag.toString());
		tag = markup.get(3);
		assertEquals("</script>", tag.toString());
	}


	/**
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 * @throws ParseException
	 */
	@Test
	void parseConditionalComment() throws IOException, ResourceStreamNotFoundException,
		ParseException
	{
		String x = "  <!--[if IE]>\r\n" + //
			"    <a href=\"SimplePage_3.html\">Link</a>\r\n" + //
			"  <![endif]-->";
		MarkupParser parser = new MarkupParser(x);
		Markup markup = parser.parse();
		assertEquals(x, markup.toString(true));
	}

	/**
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 */
	@Test
	void parseTagToBeExpanded() throws IOException, ResourceStreamNotFoundException
	{
		String x = "<html xmlns:wicket>\r\n<body>\r\n <span wicket:id=\"myPanel\"/>\r\n</body>\r\n</html>\r\n";
		MarkupParser parser = new MarkupParser(x);
		Markup markup = parser.parse();
		assertEquals(
			"<html xmlns:wicket>\r\n<body>\r\n <span wicket:id=\"myPanel\"></span>\r\n</body>\r\n</html>\r\n",
			markup.toString(true));
	}

	/**
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 */
	@Test
	void parseBorderSintax() throws IOException, ResourceStreamNotFoundException
	{
		tester.getApplication().getPageSettings().addComponentResolver(new Border("test_resolver")
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
		});
		String x = "<wicket:border>before body - <wicket:body/> - after body</wicket:border>";
		MarkupParser parser = new MarkupParser(x);
		Markup markup = parser.parse();
		assertEquals(x, markup.toString(true));
	}

	/**
	 * WICKET-3500
	 * 
	 * @throws IOException
	 * @throws ResourceStreamNotFoundException
	 */
	@Test
	void rawMakupParsingWithStripCommentsSetTrue() throws IOException,
		ResourceStreamNotFoundException
	{
		tester.getApplication().getMarkupSettings().setStripComments(true);
		String conditionalComment = "\r\n <!--[if IE 6]>\r\n<![endif]-->";
		MarkupParser parser = new MarkupParser(conditionalComment);
		Markup markup = parser.parse();
		assertEquals(conditionalComment, markup.get(0).toString());
	}

	/**
	 * @see <a href="https://issues.apache.org/jira/browse/WICKET-3500">WICKET-3500</a>
	 */
	@Test
	void ppenConditionalCommentPattern()
	{
		assertFalse(AbstractMarkupParser.CONDITIONAL_COMMENT_OPENING.matcher(
			"<!--x--> <!--[if IE]>").find());

		String markup = " <!--[if IE]> <![endif]--><!--[if IE]>--><!--<![endif]--><!--[if IE]><!--><!--<![endif]--><!--[if IE]><! --><!--<![endif]-->";
		Matcher m = AbstractMarkupParser.CONDITIONAL_COMMENT_OPENING.matcher(markup);
		assertTrue(m.find());
		assertEquals(" <!--[if IE]>", m.group());
		assertFalse(m.find());

		markup = " <!--[if IE]>--> <![endif]--><!--[if IE]>--><!--<![endif]--><!--[if IE]><!--><!--<![endif]--><!--[if IE]><! --><!--<![endif]-->";
		m = AbstractMarkupParser.CONDITIONAL_COMMENT_OPENING.matcher(markup);
		assertTrue(m.find());
		assertEquals(" <!--[if IE]>-->", m.group());
		assertFalse(m.find());

		markup = " <!--[if IE]><!--> <![endif]--><!--[if IE]>--><!--<![endif]--><!--[if IE]><!--><!--<![endif]--><!--[if IE]><! --><!--<![endif]-->";
		m = AbstractMarkupParser.CONDITIONAL_COMMENT_OPENING.matcher(markup);
		assertTrue(m.find());
		assertEquals(" <!--[if IE]><!-->", m.group());
		assertFalse(m.find());

		markup = " <!--[if IE]><! --> <![endif]--><!--[if IE]>--><!--<![endif]--><!--[if IE]><!--><!--<![endif]--><!--[if IE]><! --><!--<![endif]-->";
		m = AbstractMarkupParser.CONDITIONAL_COMMENT_OPENING.matcher(markup);
		assertTrue(m.find());
		assertEquals(" <!--[if IE]><! -->", m.group());
		assertFalse(m.find());

	}

	/**
	 * Tests that IE conditional comments are properly preserved when
	 * {@link org.apache.wicket.settings.MarkupSettings#setStripComments(boolean)} is set to true
	 * 
	 * @see <a href="https://issues.apache.org/jira/browse/WICKET-3648">WICKET-3648</a>
	 * 
	 * @throws Exception
	 */
	@Test
	void IEConditionalComments() throws Exception
	{
		boolean stripComments = tester.getApplication().getMarkupSettings().getStripComments();
		try
		{
			tester.getApplication().getMarkupSettings().setStripComments(false);
			executeTest(IEConditionalCommentsPage.class, "IEConditionalCommentsPage-expected.html");

			tester.getApplication().getMarkupSettings().setStripComments(true);
			executeTest(IEConditionalCommentsPage.class, "IEConditionalCommentsPage-expected.html");
		}
		finally
		{
			tester.getApplication().getMarkupSettings().setStripComments(stripComments);
		}
	}
}
