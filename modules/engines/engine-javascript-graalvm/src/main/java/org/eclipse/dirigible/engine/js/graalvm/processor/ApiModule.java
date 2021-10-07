/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.js.graalvm.processor;

public class ApiModule {
    private final String name;
    private final String api;
    private final String[] versionedPaths;
    private final String pathDefault;
    private final boolean isPackageDescription;

    public ApiModule(String name, String api, String[] versionedPaths, String pathDefault, boolean isPackageDescription) {
        this.name = name;
        this.api = api;
        this.versionedPaths = versionedPaths;
        this.pathDefault = pathDefault;
        this.isPackageDescription = isPackageDescription;
    }

    public String getName() {
        return name;
    }

    public String getApi() {
        return api;
    }

    public String[] getVersionedPaths() {
        return versionedPaths;
    }

    public String getPathDefault() {
        return pathDefault;
    }

    public boolean isPackageDescription() {
        return isPackageDescription;
    }
}
