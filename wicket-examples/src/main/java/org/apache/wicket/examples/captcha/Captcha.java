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
package org.apache.wicket.examples.captcha;

import org.apache.wicket.examples.WicketExamplePage;
import java.security.SecureRandom;

/**
 * Captcha example page.
 * 
 * @author Joshua Perlow
 */
public class Captcha extends WicketExamplePage
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public Captcha()
	{
		add(new CaptchaForm<Void>("wicket"));

		add(new KaptchaForm<Void>("kaptcha"));

		add(new CageForm<Void>("cage"));
	}

	static int randomInt(int min, int max)
	{
		SecureRandom secureRandom = new SecureRandom();
		return (int)(secureRandom.nextDouble() * (max - min) + min);
	}

	static String randomString(int min, int max)
	{
		int num = randomInt(min, max);
		byte b[] = new byte[num];
		for (int i = 0; i < num; i++)
			b[i] = (byte)randomInt('a', 'z');
		return new String(b);
	}
}
