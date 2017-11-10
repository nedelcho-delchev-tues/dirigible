/*
 * Copyright (c) 2017 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * SAP - initial API and implementation
 */

package org.eclipse.dirigible.core.scheduler.api;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public abstract class AbstractSynchronizerJob implements Job {
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		getSynchronizer().synchronize();
	}

	protected abstract ISynchronizer getSynchronizer();

}
