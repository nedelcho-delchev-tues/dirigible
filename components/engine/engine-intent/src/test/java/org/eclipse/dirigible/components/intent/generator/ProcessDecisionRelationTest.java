/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.dirigible.components.intent.generator.bpmn.BpmnIntentGenerator;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.StepIntent;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * A process decision branching on a TO-ONE RELATION of its trigger entity (issue #7648). "Send it
 * by e-mail or print it" is a property of the document, held as a foreign key into a small
 * nomenclature - but only the entity's own FIELDS were ever loaded before a gateway, so the
 * identifier resolved solely when a task form happened to post it as a variable, and a completion
 * through the Inbox API carries none: Flowable then failed the expression on the unknown property
 * and the instance stopped at the gateway.
 *
 * <p>
 * The foreign key is a number on the row like any other column, so all three halves are the ones
 * the field path already walks: the loader looks for relation names too, the gateway's condition is
 * rewritten to the PascalCase the loader publishes, and a seeded NAME on the right-hand side is
 * resolved against the nomenclature THAT relation points at.
 */
class ProcessDecisionRelationTest {

    private static final String YAML = """
            name: dispatch
            entities:
              - name: SendMethod
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: LetterStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Letter
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: sendMethod, kind: manyToOne, to: SendMethod }
                  - { name: status,     kind: manyToOne, to: LetterStatus, function: EntityStatus, init: 1 }
            seeds:
              - name: send-methods
                entity: SendMethod
                rows:
                  - { id: 1, name: PRINT }
                  - { id: 2, name: EMAIL }
              - name: letter-statuses
                entity: LetterStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SENT }
            processes:
              - name: Dispatch
                trigger: { onCreate: Letter }
                steps:
                  - { name: route,  kind: decision,    args: { if: "sendMethod == EMAIL", then: mail, else: print } }
                  - { name: mail,   kind: serviceTask, args: { setRelationField: status, value: SENT, next: done } }
                  - { name: print,  kind: serviceTask, args: { setRelationField: status, value: SENT, next: done } }
                  - { name: done,   kind: end }
            permissions:
              - { role: Clerk, description: Clerk, can: [Letter:read] }
            """;

    /**
     * The right-hand side is a seeded name of the nomenclature the RELATION points at - never the
     * record's own status, which is a different nomenclature carrying a different id for the same word.
     */
    @Test
    void aSeededNameIsResolvedAgainstTheRelationsOwnNomenclature() {
        StepIntent decision = IntentParser.parse(YAML)
                                          .getProcesses()
                                          .get(0)
                                          .getSteps()
                                          .get(0);

        assertEquals("sendMethod == 2", decision.getArgs()
                                                .get("if"));
    }

    /** The loader must carry the foreign key, or the gateway has nothing to read. */
    @Test
    void theForeignKeyIsLoadedBeforeTheGateway() {
        List<ProcessFieldLoadSupport.FieldLoad> loads = ProcessFieldLoadSupport.fieldLoads(IntentParser.parse(YAML));

        assertEquals(1, loads.size(), "one loader, before the one decision: " + loads);
        ProcessFieldLoadSupport.FieldLoad load = loads.get(0);
        assertEquals("route", load.beforeStep());
        assertEquals("Letter", load.ownerEntity());
        assertEquals(List.of("SendMethod"), load.fields(), "the relation is published under its PascalCase name");
    }

    /** ...and the gateway reads it under exactly the name the loader publishes. */
    @Test
    void theGatewayConditionIsRewrittenToTheLoadedName() {
        String bpmn = bpmn();

        assertTrue(bpmn.contains("SendMethod == 2"), "the condition must name the loaded variable:\n" + bpmn);
        assertTrue(bpmn.contains("gen.events.dispatch.LoadDispatchRoute"), "the loader must be a step of the flow:\n" + bpmn);
        assertTrue(bpmn.contains("sourceRef=\"loadDispatchRoute\" targetRef=\"route\""),
                "the loader must run before the gateway:\n" + bpmn);
    }

    private static String bpmn() {
        IntentModel model = IntentParser.parse(YAML);
        IRepository repository = mock(IRepository.class);
        IResource missing = mock(IResource.class);
        when(repository.getResource(anyString())).thenReturn(missing);
        when(missing.exists()).thenReturn(false);
        IntentGenerationContext context = new IntentGenerationContext(model, "/proj", "proj", "workspace", "app", repository);
        context.setSettings(IntentSettings.scaffold(model));

        new BpmnIntentGenerator().generate(context);

        ArgumentCaptor<String> paths = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<byte[]> contents = ArgumentCaptor.forClass(byte[].class);
        verify(repository, atLeastOnce()).createResource(paths.capture(), contents.capture());
        for (int i = 0; i < paths.getAllValues()
                                 .size(); i++) {
            if (paths.getAllValues()
                     .get(i)
                     .endsWith("/Dispatch.bpmn")) {
                return new String(contents.getAllValues()
                                          .get(i),
                        StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("the process BPMN was not written; wrote " + paths.getAllValues());
    }
}
