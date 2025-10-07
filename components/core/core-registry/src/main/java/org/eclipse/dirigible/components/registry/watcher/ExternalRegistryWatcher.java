/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.registry.watcher;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class ExternalRegistryWatcher extends RecursiveFolderWatcher {

    private final IRepository repository;

    @Autowired
    public ExternalRegistryWatcher(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Initialize.
     *
     */
    public void initialize() {
        String source = Configuration.get("DIRIGIBLE_REGISTRY_EXTERNAL_FOLDER");
        String target = this.repository.getInternalResourcePath(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        initialize(source, target);
    }
}
