/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

const DataStoreFacade = Java.type("org.eclipse.dirigible.components.api.db.DataStoreFacade");

export class Store {

	public static save(name: string, entry: any): void {
		DataStoreFacade.save(name, JSON.stringify(entry));
	}
	
	public static saveOrUpdate(name: string, entry: any): void {
		DataStoreFacade.saveOrUpdate(name, JSON.stringify(entry));
	}
	
	public static update(name: string, entry: any): void {
		DataStoreFacade.update(name, JSON.stringify(entry));
	}
	
	public static list(name: string, options?: string): any[] {
		const result = DataStoreFacade.list(name, options);
		return JSON.parse(result);
	}

	public static get(name: string, id: string): any | undefined {
		const result = DataStoreFacade.get(name, id);
		return JSON.parse(result);
	};

	public static remove(name: string, id: string): void {
		DataStoreFacade.deleteEntry(name, id);
	}
	
	public static find(name: string, example: string, limit: number = 100, offset: number = 0): any[] {
			const result = DataStoreFacade.find(name, example, limit, offset);
			return JSON.parse(result);
		}
	
	public static query(name: string, limit: number = 100, offset: number = 0): any[] {
		const result = DataStoreFacade.query(name, limit, offset);
		return JSON.parse(result);
	}
	
	public static queryNative(name: string): any[] {
			const result = DataStoreFacade.queryNative(name);
			return JSON.parse(result);
		}
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Store;
}