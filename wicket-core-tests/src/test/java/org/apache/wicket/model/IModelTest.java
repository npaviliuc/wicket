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
package org.apache.wicket.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.model.lambda.Address;
import org.apache.wicket.model.lambda.Person;
import org.danekja.java.util.function.serializable.SerializableBiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IModel}'s methods
 */
class IModelTest
{
	private Person person;
	private final String name = "John";
	private final String street = "Strasse";

	@BeforeEach
	void before()
	{
		person = new Person();
		person.setName(name);

		Address address = new Address();
		person.setAddress(address);
		address.setStreet(street);
		address.setNumber(123);
	}

	@Test
	void filterMatch()
	{
		IModel<Person> johnModel = Model.of(person).filter((p) -> p.getName().equals(name));

		assertEquals(person, johnModel.getObject());
	}

	@Test
	void filterNoMatch()
	{
		IModel<Person> johnModel = Model.of(person).filter((p) -> p.getName().equals("Jane"));

		assertNull(johnModel.getObject());
	}

	@Test
	void nullFilter()
	{
		IModel<Person> pers = Model.of(person);
		assertThrows(IllegalArgumentException.class, () -> pers.filter(null));
	}

	@Test
	void map()
	{
		IModel<String> personNameModel = Model.of(person).map(Person::getName);
		assertEquals(name, personNameModel.getObject());
	}

	@Test
	void map2()
	{
		IModel<String> streetModel = Model.of(person)
			.map(Person::getAddress)
			.map(Address::getStreet);
		assertEquals(street, streetModel.getObject());
	}

	@Test
	void nullMapper()
	{
		IModel<Person> pers = Model.of(person);
		assertThrows(IllegalArgumentException.class, () -> pers.map(null));

	}

	@Test
	void combineWith()
	{
		IModel<String> janeModel = Model.of("Jane");
		SerializableBiFunction<Person, String, String> function = (SerializableBiFunction<Person, String, String>)(
			person1, other) -> person1.getName() + " is in relationship with " + other;
		IModel<String> relationShipModel = Model.of(person).combineWith(janeModel, function);
		assertEquals("John is in relationship with Jane", relationShipModel.getObject());
	}

	@Test
	void combineWithNullObject()
	{
		IModel<String> janeModel = Model.of((String)null);
		SerializableBiFunction<Person, String, String> function = (SerializableBiFunction<Person, String, String>)(
			person1, other) -> person1.getName() + " is in relationship with " + other;
		IModel<String> relationShipModel = Model.of(person).combineWith(janeModel, function);
		assertNull(relationShipModel.getObject());
	}

	@Test
	void combineWithNullModel()
	{
		IModel<String> janeModel = null;
		SerializableBiFunction<Person, String, String> function = (SerializableBiFunction<Person, String, String>)(
			person1, other) -> person1.getName() + " is in relationship with " + other;

		IModel<Person> str = Model.of(person);
		assertThrows(IllegalArgumentException.class, () -> str.combineWith(janeModel, function));

	}

	@Test
	void combineWithNullCombiner()
	{
		IModel<Person> pers = Model.of(person);
		IModel<String> jane = Model.of("Jane");
		assertThrows(IllegalArgumentException.class, () -> pers.combineWith(jane, null));

	}

	@Test
	void flatMap()
	{
		IModel<String> heirModel = Model.of(person)
			.flatMap(john -> LambdaModel.of(() -> john.getName() + " is my parent", john::setName));
		assertEquals("John is my parent", heirModel.getObject());

		String newValue = "Matthias";
		heirModel.setObject(newValue);
		assertEquals("Matthias is my parent", heirModel.getObject());
	}

	@Test
	void nullFlatMapper()
	{
		assertThrows(IllegalArgumentException.class, () -> returnNullFlatMapper());
	}

	void returnNullFlatMapper() {
		Model.of(person).flatMap(null);
	}

	@Test
	void orElse()
	{
		person.setName(null);
		String defaultName = "Default name";
		IModel<String> defaultNameModel = Model.of(person).map(Person::getName).orElse(defaultName);

		assertEquals(defaultName, defaultNameModel.getObject());
	}

	@Test
	void orElseGet()
	{
		person.setName(null);
		String defaultName = "Default name";
		IModel<String> defaultNameModel = Model.of(person)
			.map(Person::getName)
			.orElseGet(() -> defaultName);

		assertEquals(defaultName, defaultNameModel.getObject());
	}

	@Test
	void orElseGetNullOther()
	{
		IModel<String> iModelName = Model.of(person).map(Person::getName);
		assertThrows(IllegalArgumentException.class, () -> iModelName.orElseGet(null));
	}

	@Test
	void isPresent()
	{
		assertEquals(true, Model.of(person).isPresent().getObject());
	}

	@Test
	void isPresentNot()
	{
		assertEquals(false, Model.of((Person)null).isPresent().getObject());
	}

	@Test
	void serializableMethodReference()
	{
		Person p = new Person();
		IModel<String> m = p::getName;
		assertNotNull(WicketObjects.cloneObject(m));
	}

	static class Account
	{
		private Person person = new Person();
		{
			person.setName("Some Name");
		}

		Person getPerson()
		{
			return person;
		}
	}

	@Test
	void serializableMethodChainReference()
	{
		IModel<Account> accountModel = LoadableDetachableModel.of(Account::new);
		IModel<Person> personModel = accountModel.map(Account::getPerson);
		IModel<String> nameModel = personModel.map(Person::getName);

		IModel<String> clone = WicketObjects.cloneObject(nameModel);
		assertNotNull(clone);
		assertEquals("Some Name", clone.getObject());
	}

	sealed interface TextMatchingStatus
	{
		record NotSubmitted() implements TextMatchingStatus {}
		record Queued() implements TextMatchingStatus {}
		record Analysed(int matchingInPercent) implements TextMatchingStatus {}
		record Error(int errorCode, String humanReadableMessage) implements TextMatchingStatus {}
	}

	@Test
	void asModelWrongClass()
	{
		IModel<TextMatchingStatus> statusModel = LoadableDetachableModel.of(() ->
				new TextMatchingStatus.Error(3, "File too big"));
		IModel<TextMatchingStatus.Queued> poly = statusModel.as(TextMatchingStatus.Queued.class);

		assertNull(poly.getObject());
	}

	@Test
	void asModelCorrectClass()
	{
		IModel<TextMatchingStatus> statusModel = LoadableDetachableModel.of(() ->
				new TextMatchingStatus.Analysed(14));
		IModel<TextMatchingStatus.Analysed> poly = statusModel.as(TextMatchingStatus.Analysed.class);

		assertNotNull(poly.getObject());
		assertEquals(new TextMatchingStatus.Analysed(14), poly.getObject());
	}

	@Test
	void nullAs()
	{
		IModel<TextMatchingStatus> statusModel = LoadableDetachableModel.of(TextMatchingStatus.NotSubmitted::new);
		assertThrows(IllegalArgumentException.class, () -> statusModel.as(null));
	}

}
