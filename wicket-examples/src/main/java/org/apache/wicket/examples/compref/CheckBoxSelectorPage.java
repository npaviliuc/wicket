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
package org.apache.wicket.examples.compref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.examples.WicketExamplePage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.CheckBoxSelector;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.form.CheckboxMultipleChoiceSelector;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * An example page for {@link CheckBoxSelector}
 */
public class CheckBoxSelectorPage extends WicketExamplePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * 
	 * @param parameters
	 *            Page parameters
	 */
	public CheckBoxSelectorPage(final PageParameters parameters)
	{
		super(parameters);

		final Form<?> form = new Form<>("form");
		add(form);
		final CheckGroup<Integer> checkgroup = new CheckGroup<>("checkgroup", Arrays.asList(
			1, 2, 3, 4));
		form.add(checkgroup);
		checkgroup.add(new Check<>("check1", Model.of(1)));
		checkgroup.add(new Check<>("check2", Model.of(2)));
		checkgroup.add(new Check<>("check3", Model.of(3)));
		checkgroup.add(new Check<>("check4", Model.of(4)));
		// one selector inside the checkgroup...
		checkgroup.add(new CheckGroupSelector("groupSelector"));

		// ...and one selector outside the checkgroup
		form.add(new CheckGroupSelector("groupSelectorOutside", checkgroup));
		
		final CheckBoxMultipleChoice<Integer> choice = new CheckBoxMultipleChoice<>(
			"choice", Model.ofList(new ArrayList<>()), Arrays.asList(1, 2, 3, 4));
		form.add(choice);
		form.add(new CheckboxMultipleChoiceSelector("choiceSelector", choice));

		final CheckBox loose1 = new CheckBox("looseCheck1", Model.of(Boolean.FALSE));
		final CheckBox loose2 = new CheckBox("looseCheck2", Model.of(Boolean.FALSE));
		final CheckBox loose3 = new CheckBox("looseCheck3", Model.of(Boolean.FALSE));
		final CheckBox loose4 = new CheckBox("looseCheck4", Model.of(Boolean.FALSE));
		form.add(loose1, loose2, loose3, loose4);
		form.add(new CheckBoxSelector("looseSelector", loose1, loose2, loose3, loose4));

		// and one which will get more choices later
		final List<Integer> extensibleChoices = new ArrayList<>(Arrays.asList(1, 2, 3));
		final Set<Integer> selected = new HashSet<>();
		final ListView<Integer> listView = new ListView<Integer>("list", extensibleChoices) {
			@Override
			protected void populateItem(final ListItem<Integer> item) {
				final CheckBox check = new CheckBox("check", new Model<Boolean>() {
					@Override
					public Boolean getObject() {
						return selected.contains(item.getModelObject());
					}

					@Override
					public void setObject(Boolean object) {
						if (Boolean.TRUE.equals(object)) {
							selected.add(item.getModelObject());
						} else {
							selected.remove(item.getModelObject());
						}
					}
				});
				check.setLabel(() -> item.getModelObject().toString());
				item.add(check);
			}
		};
		listView.setReuseItems(true);
		form.add(listView);
		form.add(new Button("addChoice") {
			@Override
			public void onSubmit() {
				extensibleChoices.add(extensibleChoices.get(extensibleChoices.size() - 1) + 1);
			}
		});
		form.add(new Button("removeChoice") {
			
			@Override
			protected void onConfigure()
			{
				super.onConfigure();

				setEnabled(extensibleChoices.size() > 1);
			}
			@Override
			public void onSubmit()
			{
				
				Integer integer = extensibleChoices.get(extensibleChoices.size() - 1);
				extensibleChoices.remove(integer);
				selected.remove(integer);
			}
		});

		final CheckBoxSelector extensibleSelector = new CheckBoxSelector("extensibleSelector") {
			@Override
			protected Iterable<CheckBox> getCheckBoxes()
			{
				return collectCheckBoxes(listView);
			}
		};
		form.add(extensibleSelector);
	}
}
