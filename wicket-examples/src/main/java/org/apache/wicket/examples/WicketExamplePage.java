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
package org.apache.wicket.examples;

import org.apache.wicket.examples.source.SourcesPage;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;

/**
 * Base class for all example pages.
 * 
 * @author Jonathan Locke
 */
public class WicketExamplePage extends WebPage
{
	private static final long serialVersionUID = 1L;

	private static final String SCREEN_CONST = "screen";

	/**
	 * Constructor
	 */
	public WicketExamplePage()
	{
		this(new PageParameters());
	}

	/**
	 * Constructor
	 * 
	 * @param pageParameters
	 */
	public WicketExamplePage(final PageParameters pageParameters)
	{
		super(pageParameters);

		BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("sources",
			SourcesPage.class, SourcesPage.generatePageParameters(this));
		add(link);
		
		link.setVisible(showSourceButton());
		
		PopupSettings settings = new PopupSettings("sources", PopupSettings.RESIZABLE);
		settings.setWidth(800);
		settings.setHeight(600);
		link.setPopupSettings(settings);
		
		add(buildHeader("pageHeader"));
		
		explain();
	}

	protected boolean showSourceButton() 
	{
		return true;
	}

	protected Panel buildHeader(String id) 
	{
		return new WicketExampleHeader(id);
	}


	/**
	 * Construct.
	 * 
	 * @param model
	 */
	public WicketExamplePage(IModel<?> model)
	{
		super(model);
	}

	/**
	 * Override base method to provide an explanation
	 */
	protected void explain()
	{
	}
	
	@Override
	public void renderHead(IHeaderResponse response)
	{
		response.render(CssHeaderItem.forReference(
				new CssResourceReference(WicketExamplePage.class, "fonts/source-code-pro/stylesheet.css"), SCREEN_CONST));
		response.render(CssHeaderItem.forReference(
				new CssResourceReference(WicketExamplePage.class, "fonts/source-sans-pro/stylesheet.css"), SCREEN_CONST));
		response.render(CssHeaderItem.forReference(new CssResourceReference(WicketExamplePage.class, "style.css"),SCREEN_CONST));
	}
}
