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
package org.apache.wicket.cdi;

/**
 * Base class for injectors
 * 
 * @author igor
 */
class AbstractInjector
{
	public AbstractInjector()
	{
		// This default constructor is intentionally left empty and throws UnsupportedOperationException
 		// to signal that subclasses must provide their own implementation of this constructor.
	}

	protected <T> void postConstruct(T instance)
	{
		NonContextual.of(instance).postConstruct(instance);
	}

	protected <T> void inject(T instance)
	{
		NonContextual.of(instance).inject(instance);
	}
}
