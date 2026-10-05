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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.FieldIntent;
import org.eclipse.dirigible.components.intent.model.RelationIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ProcessIntent;
import org.eclipse.dirigible.components.intent.model.StepIntent;

/**
 * The clear-D counterpart of {@link ProcessResolverSupport} for a decision that branches on the
 * trigger entity's <b>own</b> field (e.g. {@code amount > 10000}). Because the process context
 * holds only the entity id, the field's value is not in the context; this generates a <b>field
 * loader</b>: a {@code JavaDelegate} inserted just before the decision that loads the owner
 * (trigger entity) by its id and publishes the referenced own fields as process variables, so the
 * gateway's rewritten condition ({@code Amount > 10000}) can evaluate.
 * <p>
 * Only <b>bare</b> identifiers that name a field of the trigger entity are loaded - a
 * {@code relation.field} token (e.g. {@code customer.creditLimit}) is left to
 * {@link ProcessResolverSupport}, and the form-set {@code action} (not an entity field) is ignored.
 * One loader per decision that needs one, carrying just that decision's referenced fields.
 */
public final class ProcessFieldLoadSupport {

    /** A single identifier; adjacency to a {@code .} (a relation.field path) is checked separately. */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private ProcessFieldLoadSupport() {}

    /**
     * One field loader to generate (one per decision that references the trigger entity's own fields).
     *
     * @param process the owning process
     * @param beforeStep the decision step before which the loader is inserted
     * @param handler the generated handler class simple name (e.g. {@code LoadOrderApprovalBigOrder})
     * @param ownerEntity the trigger entity loaded by id (e.g. {@code Order})
     * @param ownerPerspective the trigger entity's resolved perspective (its gen data subfolder)
     * @param ownerKeyProperty the process variable holding the entity's PK (e.g. {@code Id})
     * @param ownerKeyAccessor the {@link Number} accessor matching the PK type
     * @param fields the PascalCase own fields published as process variables for the decision
     */
    public record FieldLoad(String process, String beforeStep, String handler, String ownerEntity, String ownerPerspective,
            String ownerKeyProperty, String ownerKeyAccessor, List<String> fields) {
    }

    /** Every field loader across every process in the model. */
    public static List<FieldLoad> fieldLoads(IntentModel model) {
        List<FieldLoad> loads = new ArrayList<>();
        Map<String, EntityIntent> byName = IntentEntities.byName(model);
        Map<String, String> compositionParents = IntentEntities.compositionParents(model);
        for (ProcessIntent process : model.getProcesses()) {
            String triggerEntity = TriggerSupport.triggerEntity(process);
            EntityIntent owner = triggerEntity == null ? null : byName.get(triggerEntity);
            if (owner == null) {
                continue; // no trigger entity -> no own-field context to load
            }
            Set<String> ownFieldNames = ownNames(owner);
            for (StepIntent step : process.getSteps()) {
                if (!"decision".equals(step.getKind()) || step.getName() == null) {
                    continue;
                }
                String condition = stringArg(step, "if");
                if (condition == null) {
                    continue;
                }
                List<String> fields = referencedOwnFields(condition, ownFieldNames);
                if (fields.isEmpty()) {
                    continue; // a relation.field / action-only decision needs no own-field load
                }
                loads.add(new FieldLoad(process.getName(), step.getName(),
                        "Load" + IntentNaming.pascalCase(process.getName()) + IntentNaming.pascalCase(step.getName()), triggerEntity,
                        IntentEntities.resolvePerspective(triggerEntity, compositionParents, model), IntentEntities.keyFieldName(owner),
                        idAccessor(IntentEntities.primaryKeyOf(owner)), fields));
            }
        }
        return loads;
    }

    /**
     * The PascalCase own fields a condition references: a bare identifier (not adjacent to a {@code .},
     * which would make it part of a {@code relation.field} path) that names a field of the entity.
     */
    private static List<String> referencedOwnFields(String condition, Set<String> ownFieldNames) {
        Set<String> fields = new LinkedHashSet<>();
        Matcher matcher = IDENTIFIER.matcher(condition);
        while (matcher.find()) {
            boolean dotBefore = matcher.start() > 0 && condition.charAt(matcher.start() - 1) == '.';
            boolean dotAfter = matcher.end() < condition.length() && condition.charAt(matcher.end()) == '.';
            if (dotBefore || dotAfter) {
                continue; // part of a relation.field path - ProcessResolverSupport handles it
            }
            if (ownFieldNames.contains(matcher.group())) {
                fields.add(IntentNaming.pascalCase(matcher.group()));
            }
        }
        return new ArrayList<>(fields);
    }

    /**
     * The identifiers of the trigger entity a decision may branch on: its own fields, and - since issue
     * #7648 - its TO-ONE relations, whose foreign key the row carries exactly as it carries a field.
     *
     * <p>
     * A relation was not loadable before, so {@code if: "SentMethod == 1"} got no loader at all: the
     * identifier resolved only if a task form happened to post it as a variable, and a completion
     * through the Inbox API carries none - Flowable then failed the expression on the unknown property.
     * The FK is a number on the entity like any other column, so loading it needs nothing the field
     * path does not already do; what it needed was to be looked for.
     *
     * @param owner the trigger entity
     * @return the authored names a decision may name, fields and to-one relations alike
     */
    private static Set<String> ownNames(EntityIntent owner) {
        Set<String> names = new LinkedHashSet<>();
        for (FieldIntent field : owner.getFields()) {
            if (field.getName() != null && !field.getName()
                                                 .isBlank()) {
                names.add(field.getName());
            }
        }
        if (owner.getRelations() != null) {
            for (RelationIntent relation : owner.getRelations()) {
                boolean toOne = "manyToOne".equals(relation.getKind()) || "oneToOne".equals(relation.getKind());
                if (toOne && relation.getName() != null && !relation.getName()
                                                                    .isBlank()) {
                    names.add(relation.getName());
                }
            }
        }
        return names;
    }

    private static String idAccessor(FieldIntent pk) {
        String type = pk == null || pk.getType() == null ? "integer" : pk.getType();
        return "long".equals(type) ? "longValue" : "intValue";
    }

    private static String stringArg(StepIntent step, String key) {
        Map<String, Object> args = step.getArgs();
        if (args == null) {
            return null;
        }
        Object value = args.get(key);
        return value == null ? null : value.toString();
    }
}
