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
package org.apache.wicket.validation.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.Validatable;
import org.apache.wicket.validation.validator.CreditCardValidator.CreditCard;
import org.junit.jupiter.api.Test;

/**
 * Tests a few valid and invalid credit card numbers.
 * 
 * @author Joachim F. Rohde
 */
class CreditCardValidatorTest
{
	/**
	 * Tests a couple of credit card numbers that shouldn't be valid.
	 */
	@Test
	void invalidCreditCardNumbers()
	{
		// null value
		CreditCardValidator test = new CreditCardValidator();
		IValidatable<String> validatable = new Validatable<String>(null);
		test.validate(validatable);
		assertEquals(false, validatable.isValid());

		// too short
		validatable = new Validatable<String>("9845");
		test.validate(validatable);
		assertEquals(false, validatable.isValid());

		// too long
		validatable = new Validatable<String>("1234678910111213141516");
		test.validate(validatable);
		assertEquals(false, validatable.isValid());

		// contains a char
		validatable = new Validatable<String>("3782822X6310005");
		test.validate(validatable);
		assertEquals(false, validatable.isValid());

		// invalid number
		validatable = new Validatable<String>("840898920205250");
		test.validate(validatable);
		assertEquals(false, validatable.isValid());
	}

	/**
	 * Tests a couple of credit card numbers that should be valid. Those numbers has been taken from
	 * https://www.paypal.com/en_US/vhelp/paypalmanager_help/credit_card_numbers.htm
	 */
	

	@ParameterizedTest
	@ValueSource(strings = {
			"378282246310005", "371449635398431", "378734493671000", "378 - 7344-9367 1000",
			"30569309025904", "6011111111111117", "6011000990139424", "3530111333300000",
			"3566002020360505", "5555555555554444", "5105105105105100", "4111111111111111",
			"4012888888881881", "4222222222222", "6331101999990016"
	})

	void validCreditCardNumbers(String creditCardNumber) {
		CreditCardValidator test = new CreditCardValidator();
		IValidatable<String> validatable = new Validatable<>(creditCardNumber);
		test.validate(validatable);
		assertEquals(true, validatable.isValid());
	}

	/**
	 * https://issues.apache.org/jira/browse/WICKET-3998
	 */
	@Test
	void isVisa()
	{
		CreditCardValidator validator = new CreditCardValidator();
		assertEquals(CreditCard.VISA, validator.determineCardId("4111111111111111"));
	}

	/**
	 * https://issues.apache.org/jira/browse/WICKET-5891
	 */
	@Test
	void isChinaUnionPay()
	{
		CreditCardValidator validator = new CreditCardValidator();
		CreditCard creditCard = validator.determineCardId("6222601010012578692");
		assertEquals(CreditCard.CHINA_UNIONPAY, creditCard);
	}
}
