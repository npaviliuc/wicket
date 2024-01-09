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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.wicket.util.lang.Generics;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testcase used in the different wicket projects for testing for the correct ASL license headers.
 * Doesn't really make sense outside org.apache.wicket.
 * 
 * @author Frank Bille Jensen (frankbille)
 */
public abstract class ApacheLicenseHeaderTestCase
{
	/** Log. */
	private static final Logger log = LoggerFactory.getLogger(ApacheLicenseHeaderTestCase.class);
	private static final String PATH = "src/test/java";
	private static final String FILE_SEPARATOR = "file.separator";

	private static final String LINE_ENDING = System.getProperty("line.separator");
	protected List<String> javaIgnore = Generics.newArrayList();
	protected List<String> htmlIgnore = Generics.newArrayList();
	protected List<String> xmlPrologIgnore = Generics.newArrayList();
	protected List<String> propertiesIgnore = Generics.newArrayList();
	protected List<String> xmlIgnore = Generics.newArrayList();
	protected List<String> cssIgnore = Generics.newArrayList();
	protected List<String> velocityIgnore = Generics.newArrayList();
	protected List<String> javaScriptIgnore = Generics.newArrayList();
	protected boolean addHeaders = false;
	private File baseDirectory = new File("").getAbsoluteFile();
	/**
	 * Construct.
	 */
	protected ApacheLicenseHeaderTestCase()
	{

		// -------------------------------
		// Configure defaults
		// -------------------------------

		xmlIgnore.add(".settings");
		xmlIgnore.add("EclipseCodeFormat.xml");
		xmlIgnore.add("nb-configuration.xml");

		/*
		 * License header in test files lower the visibility of the test.
		 */
		htmlIgnore.add(PATH);

		/*
		 * Low level configuration files for logging. No license needed.
		 */
		propertiesIgnore.add(PATH);

		/*
		 * .html in test is very test specific and a license header would confuse and make it
		 * unclear what the test is about.
		 */
		xmlPrologIgnore.add(PATH);

		/*
		 * Ignore package.html
		 */
		xmlPrologIgnore.add("package.html");
	}

	/**
	 *
	 */
	protected void before()
	{
		// setup the base directory for when running inside maven (building a release
		// comes to mind).
		String property = System.getProperty("basedir");
		if (!Strings.isEmpty(property))
		{
			baseDirectory = new File(property).getAbsoluteFile();
		}
	}

	/**
	 * Test all the files in the project which has an associated {@link ILicenseHeaderHandler}.
	 */
	protected void licenseHeaders()
	{
		ILicenseHeaderHandler[] licenseHeaderHandlers;
		licenseHeaderHandlers = new ILicenseHeaderHandler[] {
				new JavaLicenseHeaderHandler(javaIgnore),
				new JavaScriptLicenseHeaderHandler(javaScriptIgnore),
				new XmlLicenseHeaderHandler(xmlIgnore),
				new PropertiesLicenseHeaderHandler(propertiesIgnore),
				new HtmlLicenseHeaderHandler(htmlIgnore),
				new VelocityLicenseHeaderHandler(velocityIgnore),
				new XmlPrologHeaderHandler(xmlPrologIgnore),
				new CssLicenseHeaderHandler(cssIgnore), };

		final Map<ILicenseHeaderHandler, List<File>> badFiles = new HashMap<>();

		for (final ILicenseHeaderHandler licenseHeaderHandler : licenseHeaderHandlers)
		{
			visitFiles(licenseHeaderHandler.getSuffixes(), licenseHeaderHandler.getIgnoreFiles(),
				file -> {
					if (!licenseHeaderHandler.checkLicenseHeader(file) && (!addHeaders || !licenseHeaderHandler.addLicenseHeader(file)))
					{
						List<File> files = badFiles.getOrDefault(licenseHeaderHandler, new ArrayList<>());
						files.add(file);
						badFiles.put(licenseHeaderHandler, files);
					}
				});
		}

		failIncorrectLicenceHeaders(badFiles);
	}

	private void failIncorrectLicenceHeaders(final Map<ILicenseHeaderHandler, List<File>> files)
	{
		if (files.size() > 0)
		{
			StringBuilder failString = new StringBuilder();

			for (Entry<ILicenseHeaderHandler, List<File>> entry : files.entrySet())
			{
				ILicenseHeaderHandler licenseHeaderHandler = entry.getKey();
				List<File> fileList = entry.getValue();

				failString.append('\n');
				failString.append(licenseHeaderHandler.getClass().getName());
				failString.append(" failed. The following files(");
				failString.append(fileList.size());
				failString.append(") didn't have correct license header:\n");

				for (File file : fileList)
				{
					String filename = file.getAbsolutePath();

					// Find the license type
					String licenseType = licenseHeaderHandler.getLicenseType(file);

					failString.append(Objects.requireNonNullElse(licenseType, "NONE"));
					failString.append(' ').append(filename).append(LINE_ENDING);
				}
			}


			Logger logger = LoggerFactory.getLogger(getClass().getName());

			if(logger.isInfoEnabled()) logger.info(failString.toString());

			throw new AssertionError(failString.toString());
		}
	}

	private void visitFiles(final List<String> suffixes, final List<String> ignoreFiles,
		final FileVisitor fileVisitor)
	{
		visitDirectory(suffixes, ignoreFiles, baseDirectory, fileVisitor);
	}

	private void visitDirectory(final List<String> suffixes, final List<String> ignoreFiles,
		final File directory, final FileVisitor fileVisitor)
	{
		File[] files = directory.listFiles(new SuffixAndIgnoreFileFilter(suffixes, ignoreFiles));

		if (files != null)
		{
			for (File file : files)
			{
				fileVisitor.visitFile(file);
			}
		}

		// Find the directories in this directory on traverse deeper
		files = directory.listFiles(new DirectoryFileFilter());

		if (files != null)
		{
			for (File childDirectory : files)
			{
				visitDirectory(suffixes, ignoreFiles, childDirectory, fileVisitor);
			}
		}
	}

	interface FileVisitor
	{
		/**
		 * @param file
		 */
		void visitFile(File file);
	}

	private class SuffixAndIgnoreFileFilter implements FileFilter
	{
		private final List<String> suffixes;
		private final List<String> ignoreFiles;

		private SuffixAndIgnoreFileFilter(final List<String> suffixes,
			final List<String> ignoreFiles)
		{
			this.suffixes = suffixes;
			this.ignoreFiles = ignoreFiles;
		}

		@Override
		public boolean accept(final File pathname)
		{
			boolean accept = false;

			if (pathname.isFile())
			{
				if (!ignoreFile(pathname))
				{
					for (String suffix : suffixes)
					{
						if (pathname.getName().endsWith("." + suffix))
						{
							accept = true;
							break;
						}
						else
						{
							log.debug("File ignored: '{}'", pathname);
						}
					}
				}
				else
				{
					log.debug("File ignored: '{}'", pathname);
				}
			}

			return accept;
		}

		private boolean ignoreFile(final File pathname) {
			if (ignoreFiles == null) {
				return false;
			}

			String relativePathname = getRelativePathname(pathname);

			for (String ignorePath : ignoreFiles) {
				ignorePath = normalizePathSeparator(ignorePath);
				File ignoreFile = new File(baseDirectory, ignorePath);
		
				if (isDirectoryIgnore(pathname, ignoreFile)) {
					return true;
				}

				if (isFileIgnore(relativePathname, ignorePath)) {
					return true;
				}

				if (isFileNameIgnore(pathname, ignorePath)) {
					return true;
				}
			}

			return false;
		}

		private String getRelativePathname(File pathname) {
			String absolutePathname = pathname.getAbsolutePath();
			return Strings.replaceAll(absolutePathname,
					baseDirectory.getAbsolutePath() + System.getProperty(FILE_SEPARATOR), "")
					.toString();
		}

		private String normalizePathSeparator(String path) {
			return Strings.replaceAll(path, "/", System.getProperty(FILE_SEPARATOR))
					.toString();
		}

		private boolean isDirectoryIgnore(File pathname, File ignoreFile) {
			return ignoreFile.isDirectory() && pathname.getAbsolutePath().startsWith(ignoreFile.getAbsolutePath());
		}

		private boolean isFileIgnore(String relativePathname, String ignorePath) {
			return new File(baseDirectory, relativePathname).isFile() && relativePathname.equals(ignorePath);
		}

		private boolean isFileNameIgnore(File pathname, String ignorePath) {
			return pathname.getName().equals(ignorePath);
		}

	}

	private class DirectoryFileFilter implements FileFilter
	{
		private final String[] ignoreDirectory = new String[] { ".git" };

		@Override
		public boolean accept(final File pathname)
		{
			boolean accept = false;

			if (pathname.isDirectory())
			{
				String relativePathname = pathname.getAbsolutePath();
				relativePathname = Strings
					.replaceAll(relativePathname,
						baseDirectory.getAbsolutePath() + System.getProperty(FILE_SEPARATOR), "")
					.toString();
				if (!"target".equals(relativePathname))
				{
					boolean found = false;
					for (String ignore : ignoreDirectory)
					{
						if (pathname.getName().equals(ignore))
						{
							found = true;
							break;
						}
					}
					if (!found)
					{
						accept = true;
					}
				}
			}

			return accept;
		}
	}
}
