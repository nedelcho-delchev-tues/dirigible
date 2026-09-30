/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.engine.template.velocity.VelocityGenerationEngine;
import org.junit.jupiter.api.Test;

/**
 * Renders the three generated REST controllers through the platform's
 * {@link VelocityGenerationEngine} to cover the create-side system-owned column rule (dirigible
 * #7549) - the mirror of the {@code preservedOnUpdate} set {@code update()} already honours.
 *
 * <p>
 * The rule lives in the CONTROLLER rather than the repository, and that placement is the whole
 * design: it is a rule about what a USER may hand in, like {@code immutableWhen:},
 * {@code visibleTo:} and the workflow-owned status - not a business invariant like {@code checks:}
 * or {@code lifecycle:}. The generated system writers (a create-from, a posting, an arrival) create
 * rows through the repository and never through a controller, so the values the MODEL tells them to
 * assign survive - which is exactly the "trusted path" the issue's Ask assumes. Enforcing it in the
 * repository instead would null those too, and a posting's amend comparison would then hold a
 * derived expression against a stored null and rewrite itself on every redelivery.
 */
class SystemOwnedOnCreateControllerTemplateIT {

    private static final String BASE = "/META-INF/dirigible/template-application-rest-java/api/";

    private static final List<String> CONTROLLERS =
            List.of("EntityController.java.template", "EntityMyController.java.template", "EntityPartnerController.java.template");

    private final VelocityGenerationEngine velocityGenerationEngine = new VelocityGenerationEngine();

    @Test
    void everySurfaceDropsTheSystemOwnedColumnsOnCreate() throws Exception {
        for (String template : CONTROLLERS) {
            String rendered = render(template, context());

            assertTrue(rendered.contains("dropSystemOwnedOnCreate(entity);"), template + " must call the guard on create");
            assertTrue(rendered.contains("private void dropSystemOwnedOnCreate("), template + " must emit the guard");
            // An author's readOnly: column and an aggregate footer field are the issue's own cases.
            assertTrue(rendered.contains("entity.Solution = null;"), template + " must drop a readOnly column: " + rendered);
            assertTrue(rendered.contains("entity.Total = null;"), template + " must drop an aggregate footer field: " + rendered);
            // ...while an ordinary user column is untouched, or the create would store nothing.
            assertFalse(rendered.contains("entity.Title = null;"), template + " must not drop an ordinary column: " + rendered);
            // The primary key is never in the set, and the numbering/uuid placeholders keep their own
            // fill logic in the repository (#7548).
            assertFalse(rendered.contains("entity.Id = null;"), template + " must not drop the primary key");
            assertFalse(rendered.contains("entity.Number = null;"), template + " must not drop a number: placeholder");
            assertNoUnresolvedReferences(rendered);
        }
    }

    /**
     * An aggregate the repository RECOMPUTES on update is exempt from the preserved set, so it must be
     * exempt here too - the two halves are one rule and may not drift.
     */
    @Test
    void anAggregateRecomputedOnUpdateIsNotDropped() throws Exception {
        Map<String, Object> context = context();
        Map<String, Object> balance = property("Balance", "DECIMAL", "java.math.BigDecimal");
        balance.put("aggregate", Boolean.TRUE);
        balance.put("calculatedPropertyExpressionUpdate", "Total - Paid");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> properties = new ArrayList<>((List<Map<String, Object>>) context.get("properties"));
        properties.add(balance);
        context.put("properties", properties);

        String rendered = render("EntityController.java.template", context);
        assertFalse(rendered.contains("entity.Balance = null;"), "a recomputed aggregate must keep the update-side exemption");
    }

    /** An entity with no system-owned column emits none of this - a model that needs it pays for it. */
    @Test
    void anEntityWithoutSystemOwnedColumnsEmitsNoGuard() throws Exception {
        Map<String, Object> context = context();
        context.put("properties", List.of(primaryKey(), property("Title", "VARCHAR", "String")));

        String rendered = render("EntityController.java.template", context);
        assertFalse(rendered.contains("dropSystemOwnedOnCreate"), "nothing to drop must emit no guard: " + rendered);
    }

    private String render(String templateName, Map<String, Object> parameters) throws Exception {
        String location = BASE + templateName;
        String template;
        try (InputStream in = getClass().getResourceAsStream(location)) {
            assertNotNull(in, "template resource not found on classpath: " + location);
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] out = velocityGenerationEngine.generate(parameters, location, template.getBytes(StandardCharsets.UTF_8));
        return new String(out, StandardCharsets.UTF_8);
    }

    /** Asserts the emitted guard resolved every reference - an unresolved one renders literally. */
    private static void assertNoUnresolvedReferences(String rendered) {
        for (String line : rendered.split("\n")) {
            if (line.contains("dropSystemOwnedOnCreate") || line.contains("= null;")) {
                assertFalse(line.contains("${"), "an unresolved template reference survived into the guard: " + line);
            }
        }
    }

    private static Map<String, Object> context() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Ticket");
        parameters.put("projectName", "services");
        parameters.put("perspectiveName", "Tickets");
        parameters.put("javaGenFolderName", "services");
        parameters.put("javaPerspectiveName", "tickets");
        parameters.put("properties", List.of(primaryKey(), title(), solution(), total(), number()));
        parameters.put("sensitiveProperties", new ArrayList<>());
        parameters.put("personalProperty", "Person");
        parameters.put("personalFkJavaClass", "Integer");
        parameters.put("personalIdentityProperty", "Email");
        parameters.put("personalIdentityLabel", "Name");
        parameters.put("personalIdentityRepositoryClass", "gen.services.data.people.PersonRepository");
        parameters.put("partnerProperty", "Customer");
        parameters.put("partnerFkJavaClass", "Integer");
        parameters.put("partnerIdentityProperty", "Email");
        parameters.put("partnerIdentityLabel", "Name");
        parameters.put("partnerIdentityRepositoryClass", "gen.services.data.people.CustomerRepository");
        return parameters;
    }

    private static Map<String, Object> primaryKey() {
        Map<String, Object> property = property("Id", "INTEGER", "Integer");
        property.put("dataPrimaryKey", Boolean.TRUE);
        return property;
    }

    /** An ordinary user-editable column. */
    private static Map<String, Object> title() {
        return property("Title", "VARCHAR", "String");
    }

    /** The issue's own case: an author's `readOnly:` column a POST could set. */
    private static Map<String, Object> solution() {
        Map<String, Object> property = property("Solution", "VARCHAR", "String");
        property.put("isReadOnlyProperty", Boolean.TRUE);
        return property;
    }

    /** An aggregate footer field, recomputed from the lines rather than handed in. */
    private static Map<String, Object> total() {
        Map<String, Object> property = property("Total", "DECIMAL", "java.math.BigDecimal");
        property.put("aggregate", Boolean.TRUE);
        return property;
    }

    /** A `number:` placeholder: read-only, but filled by the repository's own logic (#7548). */
    private static Map<String, Object> number() {
        Map<String, Object> property = property("Number", "VARCHAR", "String");
        property.put("isReadOnlyProperty", Boolean.TRUE);
        property.put("numberStampOnCreate", Boolean.TRUE);
        return property;
    }

    private static Map<String, Object> property(String name, String dataType, String javaClass) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("name", name);
        property.put("dataName", "TICKET_" + name.toUpperCase());
        property.put("dataType", dataType);
        property.put("dataTypeJavaClass", javaClass);
        property.put("dataPrimaryKey", Boolean.FALSE);
        return property;
    }
}
