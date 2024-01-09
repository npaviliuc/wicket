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
package org.apache.wicket.util.collections;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * An iterator over {@link List} which goes from the end to the start
 * 
 * @param <E>
 */
public class ReverseListIterator<E> implements Iterator<E>, Iterable<E>
{
	private final List<E> list;
	private ListIterator<E> delegateIterator;

	/**
	 * Construct.
	 * 
	 * @param list
	 *            the list which will be iterated in reverse order
	 */
	public ReverseListIterator(final List<E> list)
	{
		this.list = list;
		resetIterator();
	}

	private void resetIterator()
	{
		int start = list.size();
		this.delegateIterator = list.listIterator(start);
	}

	@Override
	public boolean hasNext()
	{
		return delegateIterator.hasPrevious();
	}

	@Override
	public E next()
	{
		return delegateIterator.previous();
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException();
	}

	/**
     * Reset the iterator to the start of the list
     */
    public void reset() {
        resetIterator();
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return ReverseListIterator.this.hasNext();
            }

            @Override
            public E next() {
                return ReverseListIterator.this.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
	}
}