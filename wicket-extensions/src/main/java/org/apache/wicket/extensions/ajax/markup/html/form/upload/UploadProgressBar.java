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
package org.apache.wicket.extensions.ajax.markup.html.form.upload;

import java.util.Formatter;

import org.apache.wicket.Application;
import org.apache.wicket.IInitializer;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;

/**
 * A panel to show the progress of an HTTP upload.
 * <p>
 * Note: For this to work upload progress monitoring must be enabled in the wicket application.
 * Example:
 * 
 * <pre>
 * <code>
 *  public class App extends WebApplication {
 * 
 * 	&#64;Override
 * 	protected void init() {
 * 		super.init();
 * 
 * 		&lt;b&gt;getApplicationSettings().setUploadProgressUpdatesEnabled(true);&lt;/b&gt; // &lt;--
 * 	}
 * }
 * </code>
 * </pre>
 * 
 * For customizing starting text see {@link #RESOURCE_STARTING}.
 * 
 * Implementation detail: Despite being located in an Ajax package, the progress communication is
 * not done via Ajax but with an IFrame instead due to a bug in Webkit based browsers, see
 * WICKET-3202.
 * 
 * @author Andrew Lombardi
 */
public class UploadProgressBar extends Panel
{
	/**
	 * Resource key used to retrieve starting message for.
	 * 
	 * Example: UploadProgressBar.starting=Upload starting...
	 */
	public static final String RESOURCE_STARTING = "UploadProgressBar.starting";

	/**
	 * Initializer for this component; binds static resources.
	 */
	public final static class ComponentInitializer implements IInitializer
	{
		@Override
		public void init(final Application application)
		{
			// register the upload status resource
			application.getSharedResources().add(RESOURCE_NAME, new UploadStatusResource());
		}

		@Override
		public String toString()
		{
			return "UploadProgressBar initializer";
		}

		@Override
		public void destroy(final Application application)
		{
		}
	}

	private static final ResourceReference JS = new JavaScriptResourceReference(
		UploadProgressBar.class, "progressbar.js");

	private static final ResourceReference CSS = new CssResourceReference(
		UploadProgressBar.class, "UploadProgressBar.css");

	private static final String RESOURCE_NAME = UploadProgressBar.class.getName();

	private static final long serialVersionUID = 1L;

	private Form<?> form;

	private MarkupContainer statusDiv;

	private MarkupContainer barDiv;

	private final FileUploadField uploadField;

	/**
	 * Constructor that will display the upload progress bar for every submit of the given form.
	 *
	 * @param id
	 *            component id (not null)
	 * @param uploadField
	 *            the file upload field to check for a file upload, or null to display the upload
	 *            field for every submit of the given form
	 */
	public UploadProgressBar(final String id,  final FileUploadField uploadField)
	{
		super(id);

		this.uploadField = uploadField;
		if (uploadField != null)
		{
			uploadField.setOutputMarkupId(true);
		}

		setRenderBodyOnly(true);
	}



	/**
	 * Constructor that will display the upload progress bar for every submit of the given form.
	 * 
	 * @param id
	 *            component id (not null)
	 * @param form
	 *            form that will be submitted (not null)
	 */
	public UploadProgressBar(final String id, final Form<?> form)
	{
		this(id, form, null);
	}

	/**
	 * Constructor that will display the upload progress bar for submissions of the given form, that
	 * include a file upload in the given file upload field; i.e. if the user did not select a file
	 * in the given file upload field, the progess bar is not displayed.
	 * 
	 * @param id
	 *            component id (not null)
	 * @param form
	 *            form that is submitted (not null)
	 * @param uploadField
	 *            the file upload field to check for a file upload, or null to display the upload
	 *            field for every submit of the given form
	 */
	public UploadProgressBar(final String id, final Form<?> form, final FileUploadField uploadField)
	{
		super(id);

		this.uploadField = uploadField;
		if (uploadField != null)
		{
			uploadField.setOutputMarkupId(true);
		}

		this.form = Args.notNull(form, "form");
		form.setOutputMarkupId(true);

		setRenderBodyOnly(true);
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		Form<?> formCallback = getCallbackForm();
		if (formCallback != null) {
			formCallback.setOutputMarkupId(true);
		}

		barDiv = newBarComponent("bar");
		add(barDiv);

		statusDiv = newStatusComponent("status");
		add(statusDiv);
	}

	/**
	 * Creates a component for the status text
	 *
	 * @param id
	 *          The component id
	 * @return the status component
	 */
	protected MarkupContainer newStatusComponent(String id)
	{
		WebMarkupContainer status = new WebMarkupContainer(id);
		status.setOutputMarkupId(true);
		return status;
	}

	/**
	 * Creates a component for the bar
	 *
	 * @param id
	 *          The component id
	 * @return the bar component
	 */
	protected MarkupContainer newBarComponent(String id)
	{
		WebMarkupContainer bar = new WebMarkupContainer(id);
		bar.setOutputMarkupId(true);
		return bar;
	}

	/**
	 * Override this to provide your own CSS, or return null to avoid including the default.
	 * 
	 * @return ResourceReference for your CSS.
	 */
	protected ResourceReference getCss()
	{
		return CSS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void renderHead(final IHeaderResponse response)
	{
		super.renderHead(response);

		CoreLibrariesContributor.contributeAjax(getApplication(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		ResourceReference css = getCss();
		if (css != null)
		{
			response.render(CssHeaderItem.forReference(css));
		}

		ResourceReference ref = new SharedResourceReference(RESOURCE_NAME);

		final String uploadFieldId = (uploadField == null) ? "null" : ("'" + uploadField.getMarkupId() + "'");

		final String status = new StringResourceModel(RESOURCE_STARTING, this, null).getString();

		CharSequence url;
		if (form != null) {
			url = urlFor(ref, UploadStatusResource.newParameter(getPage().getId()));
		}
		else if (uploadField != null) {
			url = urlFor(ref, UploadStatusResource.newParameter(uploadField.getMarkupId()));
		}
		else {
			url = urlFor(ref, UploadStatusResource.newParameter("null"));
		}
				
		StringBuilder builder = new StringBuilder(128);
		try(Formatter formatter = new Formatter(builder)) {
			Form<?> formCallback = getCallbackForm();

			formatter.format("%s = new Wicket.WUPB(%s, '%s', '%s', '%s', %s, '%s', %s);",
				getVarName(), (formCallback != null) ? String.format("'%s'", formCallback.getMarkupId()) : "null",
				statusDiv.getMarkupId(), barDiv.getMarkupId(), url, uploadFieldId, status, getOnProgressUpdatedCallBack());

			response.render(OnDomReadyHeaderItem.forScript(builder.toString()));
		}
	}

	/**
	 * Allows to pass a JavaScript function that is called when progress in updated.
	 *
	 * @return A JavaScript function.
	 */
	protected String getOnProgressUpdatedCallBack()
	{
		return "function(percent) {}";
	}

	private String getVarName() {
		return "window.upb_" + barDiv.getMarkupId();
	}


	public void start(IPartialPageRequestHandler handler)
	{
		handler.appendJavaScript(getVarName() + ".start();");
	}

	/**
	 * Form on where will be installed the JavaScript callback to present the progress bar.
	 * 
	 * @return form
	 */
	private Form<?> getCallbackForm()
	{
		if (form == null)
		{
			return null;
		}
		return form.getRootForm();
	}
}