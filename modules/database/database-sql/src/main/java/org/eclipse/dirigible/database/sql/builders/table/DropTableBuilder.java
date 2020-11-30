/*
 * Copyright (c) 2010-2020 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2020 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.table;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractDropSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Drop Table Builder.
 */
public class DropTableBuilder extends AbstractDropSqlBuilder {

	private static final Logger logger = LoggerFactory.getLogger(DropTableBuilder.class);

	private String table = null;

	/**
	 * Instantiates a new drop table builder.
	 *
	 * @param dialect
	 *            the dialect
	 * @param table
	 *            the table
	 */
	public DropTableBuilder(ISqlDialect dialect, String table) {
		super(dialect);
		this.table = table;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.dirigible.database.sql.ISqlBuilder#generate()
	 */
	@Override
	public String generate() {

		StringBuilder sql = new StringBuilder();

		// DROP
		generateDrop(sql);

		// TABLE
		generateTable(sql);

		String generated = sql.toString();

		logger.trace("generated: " + generated);

		return generated;
	}

	/**
	 * Generate table.
	 *
	 * @param sql
	 *            the sql
	 */
	protected void generateTable(StringBuilder sql) {
		String tableName = (isCaseSensitive()) ? encapsulate(this.getTable()) : this.getTable();
		sql.append(SPACE).append(KEYWORD_TABLE).append(SPACE).append(tableName);
	}
	
	/**
	 * Getter for the table
	 * 
	 * @return the table
	 */
	public String getTable() {
		return table;
	}

}
