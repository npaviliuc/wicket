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
package org.apache.wicket.extensions.markup.html.repeater.tree;

import java.util.List;
import java.util.Set;

import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.ITreeColumn;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.ITreeDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.NodeModel;
import org.apache.wicket.extensions.markup.html.repeater.tree.table.TreeDataProvider;
import org.apache.wicket.markup.repeater.IItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

/**
 * A tree with tabular markup.
 * 
 * @author svenmeier
 * 
 * @param <T>
 *            The model object type
 * @param <S>
 *            the type of the sort property
 */
public abstract class TableTree<T, S> extends AbstractTree<T>
{
	private static final long serialVersionUID = 1L;

	private final DataTable<T, S> table;

	/**
	 * Constructor
	 * 
	 * @param id
	 *            component id
	 * @param columns
	 *            list of IColumn objects
	 * @param dataProvider
	 *            imodel for data provider
	 * @param rowsPerPage
	 *            number of rows per page
	 */
	public TableTree(final String id, final List<? extends IColumn<T, S>> columns,
		final ITreeProvider<T> dataProvider, final long rowsPerPage)
	{
		this(id, columns, dataProvider, rowsPerPage, null);
	}

	/**
	 * Constructor
	 * 
	 * @param id
	 *            component id
	 * @param columns
	 *            list of IColumn objects
	 * @param provider
	 *            provider of the tree
	 * @param rowsPerPage
	 *            number of rows per page
	 * @param state
	 *            the expansion state
	 */
	public TableTree(final String id, final List<? extends IColumn<T, S>> columns,
		final ITreeProvider<T> provider, final long rowsPerPage, IModel<? extends Set<T>> state)
	{
		super(id, provider, state);

		Args.notEmpty(columns, "columns");
		for (IColumn<T, S> column : columns)
		{
			if (column instanceof ITreeColumn<?, ?>)
			{
				((ITreeColumn<T, S>)column).setTree(this);
			}
		}

		this.table = newDataTable("table", columns, newDataProvider(provider), rowsPerPage);
		add(table);

		// see #updateBranch(Object, AjaxRequestTarget)
		setOutputMarkupId(true);
	}

	/**
	 * Factory method for the wrapped {@link DataTable}.
	 * 
	 * Note: If overwritten, the DataTable's row items have to output their markupId, or
	 * {@link #updateNode(Object, IPartialPageRequestHandler)} will fail.
	 * 
	 * @param id
	 * @param columns
	 * @param dataProvider
	 * @param rowsPerPage
	 * @return nested data table
	 */
	protected DataTable<T, S> newDataTable(String id, List<? extends IColumn<T, S>> columns,
		IDataProvider<T> dataProvider, long rowsPerPage)
	{
		return new DataTable<T, S>(id, columns, dataProvider, rowsPerPage)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Item<T> newRowItem(String id, int index, IModel<T> model)
			{
				Item<T> item = TableTree.this.newRowItem(id, index, model);

				// see #update(Node);
				item.setOutputMarkupId(true);

				return item;
			}
		};
	}

	/**
	 * Get the nested table.
	 * 
	 * @return the nested table
	 */
	public DataTable<T, S> getTable()
	{
		return table;
	}

	/**
	 * Sets the item reuse strategy. This strategy controls the creation of {@link Item}s.
	 * 
	 * @see RefreshingView#setItemReuseStrategy(IItemReuseStrategy)
	 * @see IItemReuseStrategy
	 * 
	 * @param strategy
	 *            item reuse strategy
	 * @return this for chaining
	 */
	@Override
	public final TableTree<T, S> setItemReuseStrategy(final IItemReuseStrategy strategy)
	{
		table.setItemReuseStrategy(strategy);

		super.setItemReuseStrategy(strategy);

		return this;
	}

	/**
	 * For updating of a single branch the whole table is added to the ART.
	 */
	@Override
	public void updateBranch(T node, IPartialPageRequestHandler target)
	{
		// TableTree always outputs markupId
		target.add(this);
	}

	/**
	 * For an update of a node the complete row item is added to the ART.
	 */
	@Override
	public void updateNode(T t, IPartialPageRequestHandler target)
	{
		final IModel<T> model = getProvider().model(t);
		table.getBody().visitChildren(Item.class, (IVisitor<Item<T>, Void>) (item, visit) -> {
			NodeModel<T> nodeModel = (NodeModel<T>)item.getModel();

			if (model.equals(nodeModel.getWrappedModel()))
			{
				// row items are configured to output their markupId
				target.add(item);
				visit.stop();
				return;
			}
			visit.dontGoDeeper();
		});
		model.detach();
	}

	/**
	 * Hook method to create an {@link ITreeDataProvider}.
	 * 
	 * @param provider
	 *            the tree provider
	 * @return the data provider
	 */
	protected ITreeDataProvider<T> newDataProvider(ITreeProvider<T> provider)
	{
		return new TreeDataProvider<T>(provider)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean iterateChildren(T object)
			{
				return TableTree.this.getState(object) == State.EXPANDED;
			}
		};
	}

	/**
	 * Create a row item for the nested {@link DataTable}.
	 * 
	 * @param id
	 *            component id
	 * @param index
	 *            index of row
	 * @param model
	 *            model for row
	 * @return row item
	 */
	protected Item<T> newRowItem(String id, int index, IModel<T> model)
	{
		Item<T> item = new Item<>(id, index, model);

		return item;
	}
}
