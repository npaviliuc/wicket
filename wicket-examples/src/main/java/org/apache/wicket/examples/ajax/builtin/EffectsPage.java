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
package org.apache.wicket.examples.ajax.builtin;

import java.util.Optional;

import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxChannel.Type;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;

/**
 * Demonstrates ajax effects
 */
public class EffectsPage extends BasePage
{
	private int counter1 = 0;
	private int counter2 = 0;
	private int counter3 = 0;

	private static final String EFFECTS_CONST = "effects";

	/**
	 * @return Value of counter1
	 */
	public int getCounter1()
	{
		return counter1;
	}

	/**
	 * @param counter1
	 *            New value for counter1
	 */
	public void setCounter1(int counter1)
	{
		this.counter1 = counter1;
	}

	/**
	 * @return Value for counter2
	 */
	public int getCounter2()
	{
		return counter2;
	}

	/**
	 * @param counter2
	 *            New value for counter2
	 */
	public void setCounter2(int counter2)
	{
		this.counter2 = counter2;
	}


	/**
	 * @return Value for counter3
	 */
	public int getCounter3()
	{
		return counter3;
	}

	/**
	 * @param counter3
	 *            New value for counter3
	 */
	public void setCounter3(int counter3)
	{
		this.counter3 = counter3;
	}

	/**
	 * Constructor
	 */
	public EffectsPage()
	{
		final Label c1 = new Label("c1", new PropertyModel<>(this, "counter1"));
		c1.setOutputMarkupId(true);
		add(c1);

		final Label c2 = new Label("c2", new PropertyModel<>(this, "counter2"));
		c2.setOutputMarkupId(true);
		add(c2);

		final Label c3 = new Label("c3", new PropertyModel<>(this, "counter3"));
		c3.setOutputMarkupId(true);
		add(c3);

		add(new AjaxLink<Void>("c1-link")
		{
			@Override
			public void onClick(AjaxRequestTarget target)
			{
				counter1++;
				target.add(c1);
				target.appendJavaScript(String.format("jQuery('#%s').effect('shake');",
					c1.getMarkupId()));
			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				attributes.setChannel(new AjaxChannel(EFFECTS_CONST, Type.DROP));

				super.updateAjaxAttributes(attributes);
			}
		});

		add(new AjaxFallbackLink<Void>("c2-link")
		{
			@Override
			public void onClick(Optional<AjaxRequestTarget> targetOptional)
			{
				counter2++;
				targetOptional.ifPresent(target -> {
					target.add(c2);
					target.appendJavaScript(String.format("jQuery('#%s').effect('highlight');",
							c2.getMarkupId()));
				});
			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				attributes.setChannel(new AjaxChannel(EFFECTS_CONST, Type.DROP));

				super.updateAjaxAttributes(attributes);
			}
		});

		add(new AjaxFallbackLink<Void>("c3-link")
		{
			@Override
			public void onClick(Optional<AjaxRequestTarget> targetOptional)
			{
				counter3++;
				targetOptional.ifPresent(target -> {
					target.prependJavaScript((String.format("jQuery('#%s').fadeOut(500, Wicket.Ajax.suspendCall());", c3.getMarkupId())));
					target.add(c3);
					target.appendJavaScript((String.format("jQuery('#%s').hide().fadeIn(500);", c3.getMarkupId())));
				});
			}

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				attributes.setChannel(new AjaxChannel(EFFECTS_CONST, Type.DROP));

				super.updateAjaxAttributes(attributes);
			}
		});
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(JavaScriptHeaderItem.forUrl("jquery-ui-1.10.3.custom.js"));
		response.render(OnDomReadyHeaderItem.forScript("jQuery.noConflict();"));
		// make effects work nicely with inline elements 
		response.render(CssHeaderItem.forCSS(
			"div.container { font-size: 14pt; } .ui-effects-wrapper { display:inline; }",
			EFFECTS_CONST));
	}

}
