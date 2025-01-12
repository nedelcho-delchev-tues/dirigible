/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
export function getTemplate() {
	return {
		name: 'report-model',
		label: 'Report Model',
		extension: 'report',
		data: JSON.stringify({
			name: 'MYREPORT',
			alias: 'mt',
			table: 'MYTABLE',
			columns: [
				{
					alias: 'Customer',
					name: 'CUSTOMER_NAME',
					type: 'VARCHAR',
					aggregate: 'NONE',
					table: 'mt',
					select: true
				}
			]
		}, null, 4)
	};
};