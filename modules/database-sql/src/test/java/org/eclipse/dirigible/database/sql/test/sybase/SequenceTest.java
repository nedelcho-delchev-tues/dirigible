/*
 * Copyright (c) 2017 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * SAP - initial API and implementation
 */

package org.eclipse.dirigible.database.sql.test.sybase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.dialects.sybase.SybaseSqlDialect;
import org.junit.Test;

public class SequenceTest {
	
	@Test
	public void createSequence() {
		String sql = SqlFactory.getNative(new SybaseSqlDialect())
			.create()
			.sequence("CUSTOMERS_SEQUENCE")
			.build();
		
		assertNotNull(sql);
		assertEquals("CREATE SEQUENCE CUSTOMERS_SEQUENCE", sql);
	}
	
	@Test
	public void dropSequnce() {
		String sql = SqlFactory.getNative(new SybaseSqlDialect())
			.drop()
			.sequence("CUSTOMERS_SEQUENCE")
			.build();
		
		assertNotNull(sql);
		assertEquals("DROP SEQUENCE CUSTOMERS_SEQUENCE", sql);
	}
	
	@Test
	public void nextvalSequnce() {
		String sql = SqlFactory.getNative(new SybaseSqlDialect())
			.nextval("CUSTOMERS_SEQUENCE")
			.build();
		
		assertNotNull(sql);
		assertEquals("SELECT CUSTOMERS_SEQUENCE.NEXTVAL", sql);
	}

}
