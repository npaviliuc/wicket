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
package org.apache.wicket.util.license;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;

import org.apache.wicket.util.io.IOUtils;
import org.apache.wicket.util.string.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractLicenseHeaderHandler implements ILicenseHeaderHandler
{
	private static final Logger log = LoggerFactory.getLogger(AbstractLicenseHeaderHandler.class);
	protected static final String LINE_ENDING = System.getProperty("line.separator");
	private final List<String> ignoreFiles;
	private String licenseHeader;

	/**
	 * Construct.
	 * 
	 * @param ignoreFiles
	 */
	public AbstractLicenseHeaderHandler(final List<String> ignoreFiles)
	{
		this.ignoreFiles = ignoreFiles;
	}

	@Override
	public List<String> getIgnoreFiles()
	{
		return ignoreFiles;
	}

	@Override
	public boolean addLicenseHeader(final File file)
	{
		System.out.println("Not supported yet.");
		return false;
	}

	@Override
	public String getLicenseType(final File file)
	{
		return null;
	}

	protected String extractLicenseHeader(final File file, final int start, final int length)
	{
		StringBuilder header = new StringBuilder();

		try (FileReader fileReader = new FileReader(file);
		LineNumberReader lineNumberReader = new LineNumberReader(fileReader))
		{

			for (int i = start; i < length; i++)
			{
				header.append(lineNumberReader.readLine());
				header.append(LINE_ENDING);
			}
		}
		catch (Exception e)
		{
			throw new AssertionError(e.getMessage());
		}

		return header.toString().trim();
	}

	/**
	 * Add the license header to the start of the file without caring about existing license
	 * headers.
	 * 
	 * @param file
	 *            The file to add the license header to.
	 */
	protected void prependLicenseHeader(final File file)
	{
		try
		{
			String content = new org.apache.wicket.util.file.File(file).readString();
			content = getLicenseHeader() + LINE_ENDING + content;
			new org.apache.wicket.util.file.File(file).write(content);
		}
		catch (Exception e)
		{
			throw new AssertionError(e.getMessage());
		}
	}

	protected String getLicenseHeader()
	{
		if (Strings.isEmpty(licenseHeader))
		{
			InputStream inputStream = ApacheLicenseHeaderTestCase.class
					.getResourceAsStream(getLicenseHeaderFilename());

			try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				LineNumberReader lineNumberReader = new LineNumberReader(inputStreamReader))
			{
				StringBuilder header = new StringBuilder();
				String line = lineNumberReader.readLine();
				while (line != null)
				{
					header.append(line);
					header.append(LINE_ENDING);
					line = lineNumberReader.readLine();
				}

				licenseHeader = header.toString().trim();
			}
			catch (Exception e)
			{
				throw new AssertionError(e.toString());
			}
			finally
			{
				IOUtils.closeQuietly(inputStream);
			}
		}

		return licenseHeader;
	}

	protected abstract String getLicenseHeaderFilename();
}
