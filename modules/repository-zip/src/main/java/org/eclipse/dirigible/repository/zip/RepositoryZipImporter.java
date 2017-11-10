/*
 * Copyright (c) 2017 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * SAP - initial API and implementation
 */

package org.eclipse.dirigible.repository.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.ContentTypeHelper;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class which imports all the content from a given zip
 */
public class RepositoryZipImporter {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryZipImporter.class);

	/**
	 * Import all the content from a given zip to the target repository instance
	 * within the given path
	 *
	 * @param repository the target {@link IRepository} instance
	 * @param zipInputStream the content input stream
	 * @param relativeRoot the relative root
	 * @throws RepositoryImportException in case the content cannot be imported
	 */
	public static void importZip(IRepository repository, ZipInputStream zipInputStream, String relativeRoot) throws RepositoryImportException {
		importZip(repository, zipInputStream, relativeRoot, false);
	}

	/**
	 * Import all the content from a given zip to the target repository instance
	 * within the given path, overrides files during the pass
	 *
	 * @param repository the target {@link IRepository} instance
	 * @param zipInputStream the content input stream
	 * @param relativeRoot the relative root
	 * @param override whether to override existing
	 * @throws RepositoryImportException in case the content cannot be imported
	 */
	public static void importZip(IRepository repository, ZipInputStream zipInputStream, String relativeRoot, boolean override) throws RepositoryImportException {
		importZip(repository, zipInputStream, relativeRoot, override, false);
	}

	/**
	 * Import all the content from a given zip to the target repository instance
	 * within the given path, overrides files during the pass and removes the root folder name
	 *
	 * @param repository the target {@link IRepository} instance
	 * @param zipInputStream the content input stream
	 * @param relativeRoot the relative root
	 * @param override whether to override existing
	 * @param excludeRootFolderName
	 * @throws RepositoryImportException in case the content cannot be imported
	 */
	public static void importZip(IRepository repository, ZipInputStream zipInputStream, String relativeRoot, boolean override,
			boolean excludeRootFolderName) throws RepositoryImportException {
		importZip(repository, zipInputStream, relativeRoot, override, excludeRootFolderName, null);
	}

	/**
	 * Import all the content from a given zip to the target repository instance
	 * within the given path, overrides files during the pass and removes the root folder name
	 *
	 * @param repository the target {@link IRepository} instance
	 * @param zipInputStream the content input stream
	 * @param relativeRoot the relative root
	 * @param override whether to override existing
	 * @param excludeRootFolderName
	 * @param filter
	 *            map of old/new string for replacement in paths
	 * @throws RepositoryImportException in case the content cannot be imported
	 */
	public static void importZip(IRepository repository, ZipInputStream zipInputStream, String relativeRoot, boolean override,
			boolean excludeRootFolderName, Map<String, String> filter) throws RepositoryImportException {

		logger.debug("importZip started...");

		try {
			try {
				ZipEntry entry;
				String parentFolder = null;
				while ((entry = zipInputStream.getNextEntry()) != null) {
	
					if (excludeRootFolderName && (parentFolder == null)) {
						parentFolder = entry.getName();
						logger.debug("importZip parentFolder: " + parentFolder);
						continue;
					}
	
					String entryName = getEntryName(entry, parentFolder, excludeRootFolderName);
					logger.debug("importZip entryName: " + entryName);
					String outpath = relativeRoot + ((relativeRoot.endsWith(IRepository.SEPARATOR)) ? "" : IRepository.SEPARATOR) + entryName;
					logger.debug("importZip outpath: " + outpath);
	
					if (filter != null) {
						for (Map.Entry<String, String> forReplacement : filter.entrySet()) {
							outpath = outpath.replace(forReplacement.getKey(), forReplacement.getValue());
						}
					}
					logger.debug("importZip outpath replaced: " + outpath);
	
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					try {
						IOUtils.copy(zipInputStream, output);
						try {
							if (output.toByteArray().length > 0) {
								// TODO filter for binary extensions
	
								String extension = ContentTypeHelper.getExtension(entry.getName());
								String mimeType = ContentTypeHelper.getContentType(extension);
								boolean isBinary = ContentTypeHelper.isBinary(mimeType);
								if (mimeType != null) {
									logger.debug("importZip creating resource: " + outpath);
									logger.debug("importZip creating resource is binary?: " + isBinary);
									repository.createResource(outpath, output.toByteArray(), isBinary, mimeType, override);
	
								} else {
									logger.debug("importZip creating resource: " + outpath);
									repository.createResource(outpath, output.toByteArray(), true, ContentTypeHelper.APPLICATION_OCTET_STREAM, override);
								}
							} else {
								if (outpath.endsWith(IRepository.SEPARATOR)) {
									logger.debug("importZip creating collection: " + outpath);
									repository.createCollection(outpath);
								}
							}
						} catch (Exception e) {
							logger.error(String.format("Error importing %s", outpath), e);
						}
					} finally {
						output.close();
					}
				}
			} finally {
				zipInputStream.close();
			}
		} catch(IOException e) {
			throw new RepositoryImportException(e);
		}
		
		logger.debug("importZip ended.");
	}

	private static String getEntryName(ZipEntry entry, String parentFolder, boolean excludeParentFolder) {
		return excludeParentFolder ? entry.getName().substring(parentFolder.length()) : entry.getName();
	}

}
