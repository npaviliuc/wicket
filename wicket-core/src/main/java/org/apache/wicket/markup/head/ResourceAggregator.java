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
package org.apache.wicket.markup.head;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.DecoratingHeaderResponse;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CircularDependencyException;
import org.apache.wicket.resource.bundles.ReplacementResourceBundleReference;
import org.apache.wicket.util.lang.Classes;

/**
 * {@code ResourceAggregator} implements resource dependencies, resource bundles and sorting of
 * resources. During the rendering of components, all {@link HeaderItem}s are
 * {@linkplain RecordedHeaderItem recorded} and processed at the end.
 * 
 * @author papegaaij
 */
public class ResourceAggregator extends DecoratingHeaderResponse
{

	/**
	 * The location in which a {@link HeaderItem} is added, consisting of the component/behavior
	 * that added the item, the index in the list for that component/behavior at which the item was
	 * added and the index in the request.
	 * 
	 * @author papegaaij
	 */
	public static class RecordedHeaderItemLocation
	{
		private final Component renderBase;

		private int indexInRequest;

		private int depth = -1;

		/**
		 * Construct.
		 * 
		 * @param renderBase
		 *            The component that added the item.
		 */
		public RecordedHeaderItemLocation(Component renderBase, int indexInRequest)
		{
			this.renderBase = renderBase;
			
			this.indexInRequest = indexInRequest;
		}

		/**
		 * @return the component or behavior that added the item.
		 */
		public Object getRenderBase()
		{
			return renderBase;
		}

		/**
		 * @return the number of items added before this one in the same request.
		 */
		public int getIndexInRequest()
		{
			return indexInRequest;
		}

		public int getDepth()
		{
			if (depth == -1) {
				Component component = renderBase;
				while (component != null)  {
					depth++;
					
					component = component.getParent();
				}

			}
			return depth;
		}
		
		@Override
		public String toString()
		{
			return Classes.simpleName(renderBase.getClass());
		}
	}

	/**
	 * Contains information about an {@link HeaderItem} that must be rendered.
	 * 
	 * @author papegaaij
	 */
	public static class RecordedHeaderItem
	{
		private final HeaderItem item;

		private final List<RecordedHeaderItemLocation> locations;
		
		private int minDepth = Integer.MAX_VALUE;

		/**
		 * Construct.
		 * 
		 * @param item
		 */
		public RecordedHeaderItem(HeaderItem item)
		{
			this.item = item;
			locations = new ArrayList<>();
		}

		/**
		 * Records a location at which the item was added.
		 * 
		 * @param renderBase
		 *            The component or behavior that added the item.
		 * @param indexInRequest
		 *            Indicates the number of items added before this one in this request.
		 */
		void addLocation(Component renderBase, int indexInRequest)
		{
			locations.add(new RecordedHeaderItemLocation(renderBase, indexInRequest));
			
			minDepth = Integer.MAX_VALUE;
		}

		/**
		 * @return the actual item
		 */
		public HeaderItem getItem()
		{
			return item;
		}

		/**
		 * @return The locations at which the item was added.
		 */
		public List<RecordedHeaderItemLocation> getLocations()
		{
			return locations;
		}
		
		/**
		 * Get the minimum depth in the component tree.
		 * 
		 * @return depth
		 */
		public int getMinDepth()
		{
			if (minDepth == Integer.MAX_VALUE) {
				for (RecordedHeaderItemLocation location : locations) {
					minDepth = Math.min(minDepth, location.getDepth());
				}
			}
			
			return minDepth;
		}


		@Override
		public String toString()
		{
			return locations + ":" + item;
		}
	}

	private final Map<HeaderItem, RecordedHeaderItem> itemsToBeRendered;

	/**
	 * Header items which should be executed once the DOM is ready.
	 * Collects OnDomReadyHeaderItems and OnEventHeaderItems
	 */
	private final List<HeaderItem> domReadyItemsToBeRendered;
	private final List<OnLoadHeaderItem> loadItemsToBeRendered;

	/**
	 * The currently rendered component
	 */
	private Component renderBase;
	
	private int indexInRequest;

	/**
	 * Construct.
	 * 
	 * @param real
	 */
	public ResourceAggregator(IHeaderResponse real)
	{
		super(real);

		itemsToBeRendered = new LinkedHashMap<>();
		domReadyItemsToBeRendered = new ArrayList<>();
		loadItemsToBeRendered = new ArrayList<>();
	}

	/**
	 * Overridden to keep track of the currently rendered component.
	 * 
	 * @see Component#internalRenderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@Override
	public boolean wasRendered(Object object)
	{
		boolean ret = super.wasRendered(object);
		if (!ret && object instanceof Component objectComponent)
		{
			renderBase = objectComponent;
		}
		return ret;
	}

	/**
	 * Overridden to keep track of the currently rendered component.
	 * 
	 * @see Component#internalRenderHead(org.apache.wicket.markup.html.internal.HtmlHeaderContainer)
	 */
	@Override
	public void markRendered(Object object)
	{
		super.markRendered(object);
		if (object instanceof Component)
		{
			renderBase = null;
		}
	}

	private void recordHeaderItem(HeaderItem item, Set<HeaderItem> depsDone)
	{
		renderDependencies(item, depsDone);
		RecordedHeaderItem recordedItem = itemsToBeRendered.get(item);
		if (recordedItem == null)
		{
			recordedItem = new RecordedHeaderItem(item);
			itemsToBeRendered.put(item, recordedItem);
		}
		recordedItem.addLocation(renderBase, indexInRequest);
		indexInRequest++;
	}

	private void renderDependencies(HeaderItem item, Set<HeaderItem> depsDone)
	{
		for (HeaderItem curDependency : item.getDependencies())
		{
			curDependency = getItemToBeRendered(curDependency);
			if (depsDone.add(curDependency))
			{
				recordHeaderItem(curDependency, depsDone);
			}
			else
			{
				throw new CircularDependencyException(depsDone, curDependency);
			}
			depsDone.remove(curDependency);
		}
	}

	@Override
	public void render(HeaderItem item)
	{
		item = getItemToBeRendered(item);
		if (item instanceof OnDomReadyHeaderItem || item instanceof OnEventHeaderItem)
		{
			renderDependencies(item, new LinkedHashSet<HeaderItem>());
			domReadyItemsToBeRendered.add(item);
		}
		else if (item instanceof OnLoadHeaderItem)
		{
			renderDependencies(item, new LinkedHashSet<HeaderItem>());
			loadItemsToBeRendered.add((OnLoadHeaderItem)item);
		}
		else
		{
			Set<HeaderItem> depsDone = new LinkedHashSet<>();
			depsDone.add(item);
			recordHeaderItem(item, depsDone);
		}
	}

	@Override
	public void close()
	{
		renderHeaderItems();

		if (RequestCycle.get().find(IPartialPageRequestHandler.class).isPresent())
		{
			renderSeparateEventScripts();
		}
		else
		{
			renderCombinedEventScripts();
		}
		super.close();
	}

	/**
	 * Renders all normal header items, sorting them and taking bundles into account.
	 */
	private void renderHeaderItems()
	{
		List<RecordedHeaderItem> sortedItemsToBeRendered = new ArrayList<>(
			itemsToBeRendered.values());
		Comparator<? super RecordedHeaderItem> headerItemComparator = Application.get()
			.getResourceSettings()
			.getHeaderItemComparator();
		if (headerItemComparator != null)
		{
			Collections.sort(sortedItemsToBeRendered, headerItemComparator);
		}
		for (RecordedHeaderItem curRenderItem : sortedItemsToBeRendered)
		{
			if (markItemRendered(curRenderItem.getItem()))
			{
				getRealResponse().render(curRenderItem.getItem());
			}
		}
	}

	/**
	 * Combines all DOM ready and onLoad scripts and renders them as 2 script tags.
	 */
	private void renderCombinedEventScripts()
	{
		// make a rough estimate of the size to which this StringBuilder will grow
		int domReadyLength = domReadyItemsToBeRendered.size() * 256;
		StringBuilder domReadScript = new StringBuilder(domReadyLength);
		for (HeaderItem curItem : domReadyItemsToBeRendered)
		{
			if (markItemRendered(curItem))
			{
				domReadScript.append('\n');
				if (curItem instanceof OnDomReadyHeaderItem ondomreadyheaderitem)
				{
					domReadScript.append(ondomreadyheaderitem.getJavaScript());
				} else if (curItem instanceof OnEventHeaderItem oneventheaderitem)
				{
					domReadScript.append(oneventheaderitem.getCompleteJavaScript());
				}
				domReadScript.append(';');
			}
		}
		if (domReadScript.length() > 0)
		{
			domReadScript.append("\nWicket.Event.publish(Wicket.Event.Topic.AJAX_HANDLERS_BOUND);\n");
			getRealResponse().render(OnDomReadyHeaderItem.forScript(domReadScript));
		}

		int onLoadLength = loadItemsToBeRendered.size() * 256;
        StringBuilder onLoadScript = new StringBuilder(onLoadLength);
		
		for (OnLoadHeaderItem curItem : loadItemsToBeRendered)
		{
			if (markItemRendered(curItem))
			{
			    onLoadScript.append('\n');
			    onLoadScript.append(curItem.getJavaScript());
			    onLoadScript.append(';');
			}
		}
		if (onLoadScript.length() > 0)
		{
			getRealResponse().render(
				OnLoadHeaderItem.forScript(onLoadScript.append('\n')));
		}
	}

	/**
	 * Renders the DOM ready and onLoad scripts as separate tags.
	 */
	private void renderSeparateEventScripts()
	{
		for (HeaderItem curItem : domReadyItemsToBeRendered)
		{
			if (markItemRendered(curItem))
			{
				getRealResponse().render(curItem);
			}
		}

		for (OnLoadHeaderItem curItem : loadItemsToBeRendered)
		{
			if (markItemRendered(curItem))
			{
				getRealResponse().render(curItem);
			}
		}
	}

	private boolean markItemRendered(HeaderItem item)
	{
		if (wasRendered(item))
			return false;

		if (item instanceof IWrappedHeaderItem)
		{
			getRealResponse().markRendered(((IWrappedHeaderItem)item).getWrapped());
		}
		getRealResponse().markRendered(item);
		for (HeaderItem curProvided : item.getProvidedResources())
		{
			getRealResponse().markRendered(curProvided);
		}
		return true;
	}

	/**
	 * Resolves the actual item that needs to be rendered for the given item. This can be a
	 * {@link NoHeaderItem} when the item was already rendered. It can also be a bundle or the item
	 * itself, when it is not part of a bundle.
	 * 
	 * @param item
	 * @return The item to be rendered
	 */
	private HeaderItem getItemToBeRendered(HeaderItem item)
	{
		HeaderItem innerItem = item;
		while (innerItem instanceof IWrappedHeaderItem)
		{
			innerItem = ((IWrappedHeaderItem)innerItem).getWrapped();
		}
		if (getRealResponse().wasRendered(innerItem))
		{
			return NoHeaderItem.get();
		}

		HeaderItem bundle = Application.get().getResourceBundles().findBundle(innerItem);
		if (bundle == null)
		{
			return item;
		}

		bundle = preserveDetails(item, bundle);

		if (item instanceof IWrappedHeaderItem)
		{
			bundle = ((IWrappedHeaderItem)item).wrap(bundle);
		}
		return bundle;
	}

	/**
	 * Preserves the resource reference details for resource replacements.
	 *
	 * For example if CSS resource with media <em>screen</em> is replaced with
	 * {@link org.apache.wicket.protocol.http.WebApplication#addResourceReplacement(org.apache.wicket.request.resource.CssResourceReference, org.apache.wicket.request.resource.ResourceReference)} then the replacement will
	 * will inherit the media attribute
	 *
	 * @param item   The replaced header item
	 * @param bundle The bundle that represents the replacement
	 * @return the bundle with the preserved details
	 */
	protected HeaderItem preserveDetails(HeaderItem item, HeaderItem bundle)
	{
		HeaderItem resultBundle;
		if (item instanceof CssReferenceHeaderItem && bundle instanceof CssReferenceHeaderItem)
		{
			CssReferenceHeaderItem originalHeaderItem = (CssReferenceHeaderItem) item;
			resultBundle = preserveCssDetails(originalHeaderItem, (CssReferenceHeaderItem) bundle);
		}
		else if (item instanceof JavaScriptReferenceHeaderItem && bundle instanceof JavaScriptReferenceHeaderItem)
		{
			JavaScriptReferenceHeaderItem originalHeaderItem = (JavaScriptReferenceHeaderItem) item;
			resultBundle = preserveJavaScriptDetails(originalHeaderItem, (JavaScriptReferenceHeaderItem) bundle);
		}
		else
		{
			resultBundle = bundle;
		}

		return resultBundle;
	}

	/**
	 * Preserves the resource reference details for JavaScript resource replacements.
	 *
	 * For example if CSS resource with media <em>screen</em> is replaced with
	 * {@link org.apache.wicket.protocol.http.WebApplication#addResourceReplacement(org.apache.wicket.request.resource.JavaScriptResourceReference, org.apache.wicket.request.resource.ResourceReference)} then the replacement will
	 * will inherit the media attribute
	 *
	 * @param item   The replaced header item
	 * @param bundle The bundle that represents the replacement
	 * @return the bundle with the preserved details
	 */
	private HeaderItem preserveJavaScriptDetails(JavaScriptReferenceHeaderItem item, JavaScriptReferenceHeaderItem bundle)
	{
		HeaderItem resultBundle;
		ResourceReference bundleReference = bundle.getReference();
		if (bundleReference instanceof ReplacementResourceBundleReference)
		{
			resultBundle = JavaScriptHeaderItem.forReference(bundleReference,
					item.getPageParameters(),
					item.getId()
			).setCharset(item.getCharset()).setDefer(item.isDefer()).setAsync(item.isAsync()).setNonce(item.getNonce());
		}
		else
		{
			resultBundle = bundle;
		}
		return resultBundle;
	}

	/**
	 * Preserves the resource reference details for CSS resource replacements.
	 *
	 * For example if CSS resource with media <em>screen</em> is replaced with
	 * {@link org.apache.wicket.protocol.http.WebApplication#addResourceReplacement(org.apache.wicket.request.resource.CssResourceReference, org.apache.wicket.request.resource.ResourceReference)} then the replacement will
	 * will inherit the media attribute
	 *
	 * @param item   The replaced header item
	 * @param bundle The bundle that represents the replacement
	 * @return the bundle with the preserved details
	 */
	protected HeaderItem preserveCssDetails(CssReferenceHeaderItem item, CssReferenceHeaderItem bundle)
	{
		HeaderItem resultBundle;
		ResourceReference bundleReference = bundle.getReference();
		if (bundleReference instanceof ReplacementResourceBundleReference)
		{
			resultBundle = CssHeaderItem.forReference(bundleReference,
					item.getPageParameters(),
					item.getMedia());
		}
		else
		{
			resultBundle = bundle;
		}
		return resultBundle;
	}
}
