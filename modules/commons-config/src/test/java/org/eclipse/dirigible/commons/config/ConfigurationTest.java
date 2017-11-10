/*
 * Copyright (c) 2017 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * SAP - initial API and implementation
 */

package org.eclipse.dirigible.commons.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConfigurationTest {
	
	@Test
	public void initTest() {
		String value = Configuration.get("DIRIGIBLE_INSTANCE_NAME");
		assertEquals("local", value);
	}
	
	@Test
	public void updateTest() {
		String value = Configuration.get("DIRIGIBLE_INSTANCE_NAME");
		assertEquals("local", value);
		
		System.setProperty("DIRIGIBLE_INSTANCE_NAME", "test");
		
		Configuration.update();
		
		value = Configuration.get("DIRIGIBLE_INSTANCE_NAME");
		assertEquals("test", value);
		
		System.setProperty("DIRIGIBLE_INSTANCE_NAME", "local");
		
		Configuration.update();
	}
	
	@Test
	public void customTest() {
		Configuration.load("/test.properties");
		String value = Configuration.get("DIRIGIBLE_TEST_PROPERTY");
		assertEquals("test", value);
	}

}
