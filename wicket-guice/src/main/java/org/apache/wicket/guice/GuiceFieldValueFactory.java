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
package org.apache.wicket.guice;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentMap;

import jakarta.inject.Qualifier;

import org.apache.wicket.injection.IFieldValueFactory;
import org.apache.wicket.proxy.LazyInitProxyFactory;

import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import org.apache.wicket.util.lang.Generics;

/**
 *
 */
public class GuiceFieldValueFactory implements IFieldValueFactory
{
	private final ConcurrentMap<GuiceProxyTargetLocator, Object> cache = Generics.newConcurrentHashMap();
	private static final Object NULL_SENTINEL = new Object();

	private final boolean wrapInProxies;

	/**
	 * Construct.
	 *
	 * @param wrapInProxies
	 */
	GuiceFieldValueFactory(final boolean wrapInProxies)
	{
		this.wrapInProxies = wrapInProxies;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
public Object getFieldValue(final Field field, final Object fieldOwner) {
    Object target = null;

    if (supportsField(field)) {
        Inject injectAnnotation = field.getAnnotation(Inject.class);
        jakarta.inject.Inject jakartaInjectAnnotation = field.getAnnotation(jakarta.inject.Inject.class);

        if (!isFieldStatic(field) && hasInjectAnnotation(injectAnnotation, jakartaInjectAnnotation)) {
            try {
                boolean optional = injectAnnotation != null && injectAnnotation.optional();
                Annotation bindingAnnotation = findBindingAnnotation(field.getAnnotations());
                target = locateAndWrapProxy(field, bindingAnnotation, optional);
                cacheTargetIfSingleton(field, target, bindingAnnotation, optional);

                makeFieldAccessible(field, fieldOwner);
            } catch (MoreThanOneBindingException e) {
                handleMoreThanOneBindingException(field);
            }
        }
    }

    return target;
}

private boolean isFieldStatic(Field field) {
    return Modifier.isStatic(field.getModifiers());
}

private boolean hasInjectAnnotation(Inject injectAnnotation, jakarta.inject.Inject jakartaInjectAnnotation) {
    return !isInjectAnnotationMissing(injectAnnotation) || jakartaInjectAnnotation != null;
}

private boolean isInjectAnnotationMissing(Inject injectAnnotation) {
    return injectAnnotation == null;
}

private Object locateAndWrapProxy(Field field, Annotation bindingAnnotation, boolean optional) {
    GuiceProxyTargetLocator locator = new GuiceProxyTargetLocator(field, bindingAnnotation, optional);
    Object cachedValue = cache.get(locator);

    if (cachedValue != null) {
        return cachedValue == NULL_SENTINEL ? null : cachedValue;
    }

    Object target = locator.locateProxyTarget();

    return wrapInProxies ? LazyInitProxyFactory.createProxy(field.getType(), locator) : target;
}

private void cacheTargetIfSingleton(Field field, Object target, Annotation bindingAnnotation, boolean optional) {
    if (target != null && isSingletonScope(bindingAnnotation)) {
        Object tmpTarget = cache.putIfAbsent(new GuiceProxyTargetLocator(field, bindingAnnotation, optional), target == null ? NULL_SENTINEL : target);
        if (tmpTarget != null) {
            target = tmpTarget;
        }
    }
}

private boolean isSingletonScope(Annotation bindingAnnotation) {
    return bindingAnnotation != null; // replace with actual condition for singleton scope
}

private void makeFieldAccessible(Field field, Object fieldOwner) {
    if (!field.canAccess(fieldOwner)) {
        field.setAccessible(true);
    }
}

private void handleMoreThanOneBindingException(Field field) {
    throw new RuntimeException("Can't have more than one BindingAnnotation on field " + field.getName() +
            " of class " + field.getDeclaringClass().getName());
}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsField(final Field field)
	{
		return field.isAnnotationPresent(Inject.class) || field.isAnnotationPresent(jakarta.inject.Inject.class);
	}

	/**
	 *
	 * @param annotations
	 * @return Annotation
	 * @throws MoreThanOneBindingException
	 */
	private Annotation findBindingAnnotation(final Annotation[] annotations)
			throws MoreThanOneBindingException
	{
		Annotation bindingAnnotation = null;

		// Work out if we have a BindingAnnotation on this parameter.
		for (Annotation annotation : annotations)
		{
			if (annotation.annotationType().getAnnotation(BindingAnnotation.class) != null ||
					annotation.annotationType().getAnnotation(Qualifier.class) != null)
			{
				if (bindingAnnotation != null)
				{
					throw new MoreThanOneBindingException();
				}
				bindingAnnotation = annotation;
			}
		}
		return bindingAnnotation;
	}

	/**
	 *
	 */
	public static class MoreThanOneBindingException extends Exception
	{
		private static final long serialVersionUID = 1L;
	}
}
