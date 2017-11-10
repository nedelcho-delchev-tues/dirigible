/*
 * Copyright (c) 2017 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * SAP - initial API and implementation
 */

package org.eclipse.dirigible.database.sql.dialects.derby;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.records.SelectBuilder;

public class DerbySelectBuilder extends SelectBuilder {

	public DerbySelectBuilder(ISqlDialect dialect) {
		super(dialect);
	}

	@Override
	protected void generateLimitAndOffset(StringBuilder sql, int limit, int offset) {

		if (offset > -1) {
			sql.append(SPACE).append(KEYWORD_OFFSET).append(SPACE).append(offset).append(SPACE).append(KEYWORD_ROWS);
		}

		if (limit > -1) {
			sql.append(SPACE).append(KEYWORD_FETCH).append(SPACE).append(KEYWORD_NEXT).append(SPACE).append(limit).append(SPACE).append(KEYWORD_ROWS)
					.append(SPACE).append(KEYWORD_ONLY);
		}

	}

}
