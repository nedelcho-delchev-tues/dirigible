/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.handler;

import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.tracing.TaskState;
import org.eclipse.dirigible.components.tracing.TaskStateService;
import org.eclipse.dirigible.components.tracing.TaskStateUtil;
import org.eclipse.dirigible.components.tracing.TaskType;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The built-in scripting service job handler.
 */
public class JobHandler implements Job {

    /** The Constant TENANT_PARAMETER. */
    public static final String TENANT_PARAMETER = "tenant-id";

    /** The job execution service. */
    @Autowired
    private JobExecutionService jobExecutionService;

    /** The task state service. */
    @Autowired
    private TaskStateService taskStateService;

    /** The tenant context. */
    @Autowired
    private TenantContext tenantContext;

    /**
     * Execute.
     *
     * @param context the context
     * @throws JobExecutionException the job execution exception
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap params = context.getJobDetail()
                                   .getJobDataMap();
        Tenant tenant = (Tenant) params.get(TENANT_PARAMETER);
        if (tenant == null) {
            throw new MissingTenantException(
                    "Missing tenant parameter with key [" + TENANT_PARAMETER + "] for job with details: " + context.getJobDetail());
        }

        TaskState taskState = null;
        if (taskStateService.isTracingEnabled()) {
            Map<String, String> input = TaskStateUtil.getVariables(context.getMergedJobDataMap()
                                                                          .getWrappedMap());
            taskState = taskStateService.taskStarted(TaskType.JOB, context.getFireInstanceId(), "execution", input);
            taskState.setDefinition(context.getJobDetail()
                                           .getKey()
                                           .getGroup()
                    + ":" + context.getJobDetail()
                                   .getKey()
                                   .getName());
            taskState.setTenant(tenant.getName());

        }
        try {
            tenantContext.execute(tenant, () -> {
                jobExecutionService.executeJob(context);
                return null;
            });

            if (taskStateService.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(context.getMergedJobDataMap()
                                                                               .getWrappedMap());
                taskStateService.taskSuccessful(taskState, output);
            }
        } catch (RuntimeException ex) {
            if (taskStateService.isTracingEnabled()) {
                Map<String, String> output = TaskStateUtil.getVariables(context.getMergedJobDataMap()
                                                                               .getWrappedMap());
                taskStateService.taskFailed(taskState, output, ex.getMessage());
            }
            throw new JobExecutionException("Failed to execute job with details " + context.getJobDetail(), ex);
        }
    }

}
