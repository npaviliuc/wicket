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
package org.apache.wicket.util.lang;

import java.util.Collection;

import org.apache.wicket.util.string.Strings;

/**
 * Class with methods for asserting conditions on arguments. 
 */
public class Args
{

	private static final String ARGUMENT = "Argument '";

	/**
	 * Checks argument is not null
	 * 
	 * @param <T>
	 * @param argument
	 * @param name
	 * @return The 'argument' parameter
	 * @throws IllegalArgumentException
	 */
	public static <T> T notNull(final T argument, final String name)
	{
		if (argument == null)
		{
			throw new IllegalArgumentException(ARGUMENT + name + "' may not be null.");
		}
		return argument;
	}

	/**
	 * Checks argument is not empty (not null and has a non-whitespace character)
	 * 
	 * @param <T>
	 *            the type of the argument to check for emptiness
	 * 
	 * @param argument
	 *            the argument to check for emptiness
	 * @param name
	 *            the name to use in the error message
	 * @return The {@code argument} parameter if not empty
	 * @throws IllegalArgumentException
	 *             when the passed {@code argument} is empty
	 */
	public static <T extends CharSequence> T notEmpty(final T argument, final String name)
	{
		if (Strings.isEmpty(argument))
		{
			throw new IllegalArgumentException(ARGUMENT + name + "' may not be null or empty.");
		}
		return argument;
	}

	/**
	 * Checks argument is not empty (not null and has a non-whitespace character)
	 *
	 * Note: This method overloads {@link #notEmpty(CharSequence, String)} for performance reasons.
	 * 
	 * @param argument
	 *            the argument to check for emptiness
	 * @param name
	 *            the name to use in the error message
	 * @return The {@code argument} parameter if not empty
	 * @throws IllegalArgumentException
	 *             when the passed {@code argument} is empty
	 */
	public static String notEmpty(final String argument, final String name)
	{
		if (Strings.isEmpty(argument))
		{
			throw new IllegalArgumentException(ARGUMENT + name + "' may not be null or empty.");
		}
		return argument;
	}


	/**
	 * 
	 * Checks argument is not null or empty
	 * 
	 * @param collection
	 * @param message
	 * @param params
	 * @return the {code collection}
	 * @throws IllegalArgumentException
	 *             if the passed collection is either null or empty
	 */
	public static <T extends Collection<?>> T notEmpty(final T collection, final String message,
		final Object... params)
	{
		if (collection == null || collection.isEmpty())
		{
			throw new IllegalArgumentException(Args.format(message, params));
		}
		return collection;
	}

	/**
	 * Checks argument is not null or empty
	 * 
	 * @param collection
	 * @param name
	 * @return the {code collection}
	 * @throws IllegalArgumentException
	 *             if the passed collection is either null or empty
	 */
	public static <T extends Collection<?>> T notEmpty(final T collection, final String name)
	{
		return notEmpty(collection, "Collection '%s' may not be null or empty.", name);
	}

	/**
	 * Checks if argument is within a range
	 * 
	 * @param <T>
	 * @param min
	 * @param max
	 * @param value
	 * @param name
	 * @return the {code value}
	 * @throws IllegalArgumentException
	 */
	public static <T extends Comparable<? super T>> T withinRange(final T min, final T max,
		final T value, final String name)
	{
		notNull(min, name);
		notNull(max, name);
		if ((value.compareTo(min) < 0) || (value.compareTo(max) > 0))
		{
			throw new IllegalArgumentException(
				String.format("Argument '%s' must have a value within [%s,%s], but was %s", name,
					min, max, value));
		}
		return value;
	}

	/**
	 * Check if argument is true
	 * 
	 * @param argument
	 * @param msg
	 * @param params
	 * @return argument
	 */
	public static boolean isTrue(final boolean argument, final String msg, final Object... params)
	{
		if (argument == false)
		{
			throw new IllegalArgumentException(format(msg, params));
		}
		return argument;
	}

	/**
	 * Check if argument is false
	 * 
	 * @param argument
	 * @param msg
	 * @param params
	 * @return argument
	 */
	public static boolean isFalse(final boolean argument, final String msg, final Object... params)
	{
		if (argument == true)
		{
			throw new IllegalArgumentException(format(msg, params));
		}
		return argument;
	}

	/**
	 * Format the message. Allow for "{}" as well as %s etc. (see Formatter).
	 * 
	 * @param msg
	 * @param params
	 * @return formatted message
	 */
	static String format(String msg, final Object... params)
	{
		msg = msg.replace("{}", "%s");
		return String.format(msg, params);
	}
}
