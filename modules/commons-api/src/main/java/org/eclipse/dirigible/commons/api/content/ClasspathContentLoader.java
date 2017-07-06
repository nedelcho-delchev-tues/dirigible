package org.eclipse.dirigible.commons.api.content;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.dirigible.commons.api.logging.LoggingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClasspathContentLoader {
	
	private static final Logger logger = LoggerFactory.getLogger(ClasspathContentLoader.class);
	
	private static final String ROOT = "/";
	
	private static Boolean LOADED = false;
	
	public static final void load() throws IOException {
		load(null);
	}
	
	public static final void load(LoggingHelper loggingHelper) throws IOException {
		synchronized (ClasspathContentLoader.class) {
			if (!LOADED) {
				ServiceLoader<IClasspathContentHandler> contentHandlers = ServiceLoader.load(IClasspathContentHandler.class);
				for (IClasspathContentHandler contentHandler : contentHandlers) {
					String message = "Registering Content Handler: " + contentHandler.getClass().getCanonicalName();
					log(loggingHelper, message);
				}
				Enumeration<URL> urls = ClasspathContentLoader.class.getClassLoader().getResources("META-INF");
				while (urls.hasMoreElements()) {
					URL url = urls.nextElement();
					JarURLConnection urlConnection = (JarURLConnection) (url.openConnection());
			        try (JarFile jar = urlConnection.getJarFile();) {
			            Enumeration<JarEntry> entries = jar.entries();
			            while (entries.hasMoreElements()) {
			            	String entry = entries.nextElement().getName();
			            	for (IClasspathContentHandler contentHandler : contentHandlers) {
			            		contentHandler.accept(ROOT + entry);
			            	}
			                logger.debug("resource found: " + entry);
			            }
			        }
				}
				LOADED = true;
			}
		}
	}

	private static void log(LoggingHelper loggingHelper, String message) {
		if (loggingHelper != null) {
			loggingHelper.info(message);
		} else {
			logger.info(message);
		}
	}

}
