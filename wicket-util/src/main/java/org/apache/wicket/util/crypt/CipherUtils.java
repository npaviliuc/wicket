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
package org.apache.wicket.util.crypt;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class meant to help building {@link Cipher}.
 */
public class CipherUtils
{

	// Private constructor to hide the implicit public one
	private CipherUtils(){
		// private constructor to prevent instantiation
	}

	/**
	 * Generate a new {@link SecretKey} based on the given algorithm and with the given length.
	 * 
	 * @param algorithm
	 *              the algorithm that will be used to build the key.
	 * @param keyLength
	 *              the key length
	 * @return a new {@link SecretKey}
	 * 
	 * @throws KeyGenerationException 
	 * 				if there is an issue generating the key.
	 */
	public static SecretKey generateKey(String algorithm, int keyLength, SecureRandom secureRandom) throws KeyGenerationException
	{
		try
		{
			KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
			keyGenerator.init(keyLength, secureRandom);
			return keyGenerator.generateKey();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new KeyGenerationException("Error generating key", e);
		}
	}

	/**
	 * 
	 * 
	 * @param password
	 *              the password that will be used to build the key.
	 * @param pbeAlgorithm
	 *              the password-based algorithm that will be used to build the key.
	 * @param keyAlgorithm
	 *              the algorithm that will be used to build the key.
	 * @param salt
	 *              salt for encryption.
	 * @param iterationCount
	 * 				iteration count.
	 * @param keyLength
	 *              the key length.
	 * @return a new {@link SecretKey}
	 * 
	 * @throws KeyGenerationException 
	 * 				if there is an issue generating the key
	 */
	public static SecretKey generatePBEKey(String password, String pbeAlgorithm,
		String keyAlgorithm, byte[] salt, int iterationCount, int keyLength) throws KeyGenerationException
	{
		try
		{
			SecretKeyFactory factory = SecretKeyFactory.getInstance(pbeAlgorithm);
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
			return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), keyAlgorithm);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			throw new KeyGenerationException("Error generating PBE key", e);
		}
	}
}