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
package org.apache.wicket.bean.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import jakarta.validation.metadata.ConstraintDescriptor;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;

/**
 * Validator that delegates to the bean validation framework. The integration has to be first
 * configured using {@link BeanValidationConfiguration}.
 * 
 * <p>
 * The validator must be provided a {@link Property}, unless one can be resolved from the component
 * implicitly. By default the configuration contains the {@link DefaultPropertyResolver} so
 * {@link PropertyModel}s are supported out of the box - when attached to a component with a
 * property model the property does not need to be specified explicitly.
 * </p>
 * 
 * <p>
 * The validator will set the required flag on the form component it is attached to based on the
 * presence of the @NotNull annotation, see {@link BeanValidationContext#isRequiredConstraint(ConstraintDescriptor)}
 * for details. Notice, the required flag will only be set to {@code true},
 * components with the required flag already set to {@code true} will not have the flag set to
 * {@code false} by this validator.
 * </p>
 * 
 * <p>
 * The validator will allow {@link ITagModifier}s registered on {@link BeanValidationContext}
 * to mutate the markup tag of the component it is attached to, e.g. add a <code>maxlength</code> attribute.
 * </p>
 * 
 * <p>
 * The validator specifies default error messages in the {@code PropertyValidator.properties} file.
 * These values can be overridden in the application subclass' property files globally or in the
 * page or panel properties locally. See this file for the default messages supported.
 * </p>
 * 
 * @author igor
 * 
 * @param <T>
 */
public class PropertyValidator<T> extends Behavior implements INullAcceptingValidator<T>
{
	private static final Class<?>[] EMPTY = new Class<?>[0];

	private FormComponent<T> component;

	// the trailing underscore means that these members should not be used
	// directly. ALWAYS use the respective getter instead.
	private Property property_;

	private final IModel<Class<?>[]> groups_;

	/**
	 * A flag indicating whether the component has been configured at least once.
	 */
	private boolean requiredFlagSet;

	public PropertyValidator(Class<?>... groups)
	{
		this(null, groups);
	}

	public PropertyValidator(IModel<Class<?>[]> groups)
	{
		this(null, groups);
	}

	public PropertyValidator(Property property, Class<?>... groups)
	{
		this(property, new GroupsModel(groups));
	}

	public PropertyValidator(Property property, IModel<Class<?>[]> groups)
	{
		this.property_ = property;
		this.groups_ = groups;
	}

	/**
	 * To support debugging, trying to provide useful information where possible
	 * 
	 * @return
	 */
	private String createUnresolvablePropertyMessage(FormComponent<T> component)
	{
		String baseMessage = "Could not resolve Bean Property from component: " + component
			+ ". (Hints:) Possible causes are a typo in the PropertyExpression, a null reference or a model that does not work in combination with a "
			+ IPropertyResolver.class.getSimpleName() + ".";
		IModel<?> model = ValidationModelResolver.resolvePropertyModelFrom(component);
		if (model != null)
		{
			baseMessage += " Model : " + model;
		}
		return baseMessage;
	}

	private Property getProperty()
	{
		if (property_ == null)
		{
			BeanValidationContext config = BeanValidationConfiguration.get();
			property_ = config.resolveProperty(component);
			if (property_ == null)
			{
				throw new IllegalStateException(createUnresolvablePropertyMessage(component));
			}
		}
		return property_;
	}

	private Class<?>[] getGroups()
	{
		if (groups_ == null)
		{
			return EMPTY;
		}
		return groups_.getObject();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void bind(Component component)
	{
		if (this.component != null)
		{
			throw new IllegalStateException( //
				"This validator has already been added to component: " + this.component
					+ ". This validator does not support reusing instances, please create a new one");
		}

		if (!(component instanceof FormComponent))
		{
			throw new IllegalStateException(
				getClass().getSimpleName() + " can only be added to FormComponents");
		}

		// TODO add a validation key that appends the type so we can have
		// different messages for
		// @Size on String vs Collection - done but need to add a key for each
		// superclass/interface

		this.component = (FormComponent<T>)component;
	}

	@Override
	public void onConfigure(Component component)
	{
		super.onConfigure(component);
		if (requiredFlagSet == false)
		{
			// "Required" flag is calculated upon component's model property, so
			// we must ensure,
			// that model object is accessible (i.e. component is already added
			// in a page).
			requiredFlagSet = true;

			if (isRequired())
			{
				this.component.setRequired(true);
			}
		}
	}

	@Override
	public void detach(Component component)
	{
		super.detach(component);
		if (groups_ != null)
		{
			groups_.detach();
		}
	}

	/**
	 * Should this property make the owning component required.
	 * 
	 * @return <code>true</code> if required
	 * 
	 * @see BeanValidationContext#isRequiredConstraint(ConstraintDescriptor)
	 */
	protected boolean isRequired()
	{
		BeanValidationContext config = BeanValidationConfiguration.get();

		HashSet<Class<?>> groups = new HashSet<>(Arrays.asList(getGroups()));

		Iterator<ConstraintDescriptor<?>> it = new ConstraintIterator(config.getValidator(), getProperty());
		while (it.hasNext())
		{
			ConstraintDescriptor<?> constraint = it.next();
			
			if (config.isRequiredConstraint(constraint))
			{
				if (canApplyToDefaultGroup(constraint) && groups.isEmpty())
				{
					return true;
				}
		
				for (Class<?> constraintGroup : constraint.getGroups())
				{
					if (groups.contains(constraintGroup))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean canApplyToDefaultGroup(ConstraintDescriptor<?> constraint)
	{
		Set<Class<?>> groups = constraint.getGroups();
		//the constraint can be applied to default group either if its group array is empty
		//or if it contains jakarta.validation.groups.Default
		return groups.size() == 0 || groups.contains(Default.class);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);

		BeanValidationContext config = BeanValidationConfiguration.get();
		Validator validator = config.getValidator();
		Property property = getProperty();

		// find any tag modifiers that apply to the constraints of the property
		// being validated
		// and allow them to modify the component tag

		Iterator<ConstraintDescriptor<?>> it = new ConstraintIterator(validator, property,
			getGroups());

		while (it.hasNext())
		{
			ConstraintDescriptor<?> desc = it.next();

			ITagModifier modifier = config.getTagModifier(desc.getAnnotation().annotationType());

			if (modifier != null)
			{
				modifier.modify((FormComponent<?>)component, tag, desc.getAnnotation());
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void validate(IValidatable<T> validatable)
	{
		BeanValidationContext config = BeanValidationConfiguration.get();
		Validator validator = config.getValidator();

		Property property = getProperty();

		// validate the value using the bean validator

		Set<?> violations = validator.validateValue(property.getOwner(), property.getName(),
			validatable.getValue(), getGroups());

		// iterate over violations and report them

		for (ConstraintViolation<?> violation : (Set<ConstraintViolation<?>>)violations)
		{
			validatable.error(config.getViolationTranslator().convert(violation));
		}
	}

}
