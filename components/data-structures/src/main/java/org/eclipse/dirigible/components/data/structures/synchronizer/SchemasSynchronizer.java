/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer;

import static java.text.MessageFormat.format;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.ArtefactState;
import org.eclipse.dirigible.components.base.artefact.topology.TopologicalDepleter;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Schema;
import org.eclipse.dirigible.components.data.structures.domain.SchemaLifecycle;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintForeignKey;
import org.eclipse.dirigible.components.data.structures.domain.View;
import org.eclipse.dirigible.components.data.structures.service.SchemaService;
import org.eclipse.dirigible.components.data.structures.synchronizer.schema.SchemaCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.schema.SchemaDropProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The Class SchemasSynchronizer.
 *
 * @param <A> the generic type
 */
@Component
@Order(210)
public class SchemasSynchronizer<A extends Artefact> implements Synchronizer<Schema> {
	
	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(SchemasSynchronizer.class);
	
	/** The Constant FILE_EXTENSION_SCHEMA. */
	private static final String FILE_EXTENSION_SCHEMA = ".schema";
	
	/** The schema service. */
	private SchemaService schemaService;
	
	/** The datasources manager. */
	private DataSourcesManager datasourcesManager;
	
	/** The synchronization callback. */
	private SynchronizerCallback callback;
	
	/**
	 * Instantiates a new schema synchronizer.
	 *
	 * @param schemaService the schema service
	 * @param datasourcesManager the datasources manager
	 */
	@Autowired
	public SchemasSynchronizer(SchemaService schemaService, DataSourcesManager datasourcesManager) {
		this.schemaService = schemaService;
		this.datasourcesManager = datasourcesManager;
	}
	
	/**
	 * Gets the service.
	 *
	 * @return the service
	 */
	@Override
	public ArtefactService<Schema> getService() {
		return schemaService;
	}

	/**
	 * Checks if is accepted.
	 *
	 * @param file the file
	 * @param attrs the attrs
	 * @return true, if is accepted
	 */
	@Override
	public boolean isAccepted(Path file, BasicFileAttributes attrs) {
		return file.toString().endsWith(getFileExtension());
	}

	/**
	 * Checks if is accepted.
	 *
	 * @param type the type
	 * @return true, if is accepted
	 */
	@Override
	public boolean isAccepted(String type) {
		return Schema.ARTEFACT_TYPE.equals(type);
	}

	/**
	 * Load.
	 *
	 * @param location the location
	 * @param content the content
	 * @return the list
	 */
	@Override
	public List<Schema> load(String location, byte[] content) {
		Schema schema = parseSchema(location, new String(content, StandardCharsets.UTF_8));
		Configuration.configureObject(schema);
		schema.setLocation(location);
		if (schema.getName() == null) {
			schema.setName("");
		}
		schema.setType(Schema.ARTEFACT_TYPE);
		schema.updateKey();
		
		try {
			Schema maybe = getService().findByKey(schema.getKey());
			if (maybe != null) {
				schema.setId(maybe.getId());
			}
			getService().save(schema);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {logger.error(e.getMessage(), e);}
			if (logger.isErrorEnabled()) {logger.error("schema: {}", schema);}
			if (logger.isErrorEnabled()) {logger.error("content: {}", new String(content));}
		}
		return List.of(schema);
	}

	/**
	 * Prepare.
	 *
	 * @param wrappers the wrappers
	 * @param depleter the depleter
	 */
	@Override
	public void prepare(List<TopologyWrapper<? extends Artefact>> wrappers, TopologicalDepleter<TopologyWrapper<? extends Artefact>> depleter) {
		// drop schemas in a reverse order
		try {
			List<TopologyWrapper<? extends Artefact>> results = depleter.deplete(wrappers, SchemaLifecycle.DROP.toString());
			callback.registerErrors(this, results, SchemaLifecycle.DROP.toString(), ArtefactState.FAILED_DELETE);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {logger.error(e.getMessage(), e);}
			callback.addError(e.getMessage());
		}
	}
	
	/**
	 * Process.
	 *
	 * @param wrappers the wrappers
	 * @param depleter the depleter
	 */
	@Override
	public void process(List<TopologyWrapper<? extends Artefact>> wrappers, TopologicalDepleter<TopologyWrapper<? extends Artefact>> depleter) {
		
		// process schemas
		try {
			List<TopologyWrapper<? extends Artefact>> results = depleter.deplete(wrappers, SchemaLifecycle.CREATE.toString());
			callback.registerErrors(this, results, SchemaLifecycle.CREATE.toString(), ArtefactState.FAILED_CREATE);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {logger.error(e.getMessage(), e);}
			callback.addError(e.getMessage());
		}
		
	}

	/**
	 * Complete.
	 *
	 * @param wrapper the wrapper
	 * @param flow the flow
	 * @return true, if successful
	 */
	@Override
	public boolean complete(TopologyWrapper<Artefact> wrapper, String flow) {
		
		try (Connection connection = datasourcesManager.getDefaultDataSource().getConnection()) {
		
			Schema schema = null;
			if (wrapper.getArtefact() instanceof Schema) {
				schema = (Schema) wrapper.getArtefact();
			} else {
				throw new UnsupportedOperationException(String.format("Trying to process %s as Schema", wrapper.getArtefact().getClass()));
			}
			
			SchemaLifecycle flag = SchemaLifecycle.valueOf(flow);
			switch (flag) {
			case UPDATE:
				executeSchemaUpdate(connection, schema);
				break;
			case CREATE:
				try {
					executeSchemaCreate(connection, schema);
					callback.registerState(this, wrapper, ArtefactLifecycle.CREATED.toString(), ArtefactState.SUCCESSFUL_CREATE);
				} catch (Exception e) {
					if (logger.isErrorEnabled()) {logger.error(e.getMessage(), e);}
					callback.registerState(this, wrapper, ArtefactLifecycle.CREATED.toString(), ArtefactState.FAILED_CREATE);
				}
				break;
			case DROP:
				executeSchemaDrop(connection, schema);
				break;
			default:
				throw new UnsupportedOperationException(flow);
			}
			return true;
		} catch (SQLException e) {
			if (logger.isErrorEnabled()) {logger.error(e.getMessage(), e);}
			callback.addError(e.getMessage());
			return false;
		}
	}

	/**
	 * Cleanup.
	 *
	 * @param schema the schema
	 */
	@Override
	public void cleanup(Schema schema) {
		try {
			getService().delete(schema);
			callback.registerState(this, schema, ArtefactLifecycle.DELETED.toString(), ArtefactState.SUCCESSFUL_DELETE);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {logger.error(e.getMessage(), e);}
			callback.addError(e.getMessage());
			callback.registerState(this, schema, ArtefactLifecycle.DELETED.toString(), ArtefactState.FAILED_DELETE);
		}
	}
	
	/**
	 * Sets the callback.
	 *
	 * @param callback the new callback
	 */
	@Override
	public void setCallback(SynchronizerCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * Execute schema update.
	 *
	 * @param connection
	 *            the connection
	 * @param schemaModel
	 *            the schema model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeSchemaUpdate(Connection connection, Schema schemaModel) throws SQLException {
		if (logger.isInfoEnabled()) {logger.info("Processing Update Schema: " + schemaModel.getName());}
		if (SqlFactory.getNative(connection).exists(connection, schemaModel.getName())) {
			executeSchemaDrop(connection, schemaModel);
			executeSchemaCreate(connection, schemaModel);
		} else {
			executeSchemaCreate(connection, schemaModel);
		}
	}

	/**
	 * Execute schema create.
	 *
	 * @param connection
	 *            the connection
	 * @param schemaModel
	 *            the schema model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeSchemaCreate(Connection connection, Schema schemaModel) throws SQLException {
		SchemaCreateProcessor.execute(connection, schemaModel);
	}
	
	/**
	 * Execute schema drop.
	 *
	 * @param connection
	 *            the connection
	 * @param schemaModel
	 *            the schema model
	 * @throws SQLException
	 *             the SQL exception
	 */
	public void executeSchemaDrop(Connection connection, Schema schemaModel) throws SQLException {
		SchemaDropProcessor.execute(connection, schemaModel);
	}
	
	/**
	 * Gets the file extension.
	 *
	 * @return the file extension
	 */
	@Override
	public String getFileExtension() {
		return FILE_EXTENSION_SCHEMA;
	}

	/**
	 * Gets the artefact type.
	 *
	 * @return the artefact type
	 */
	@Override
	public String getArtefactType() {
		return Schema.ARTEFACT_TYPE;
	}
	
	/**
	 * Parses the schema.
	 *
	 * @param location the location
	 * @param content the content
	 * @return the schema
	 */
	public static Schema parseSchema(String location, String content) {
		Schema result = new Schema();
		
			JsonElement root = GsonHelper.parseJson(content);
			JsonArray structures = root.getAsJsonObject().get("schema").getAsJsonObject().get("structures").getAsJsonArray();
			for (int i=0; i<structures.size(); i++) {
				JsonObject structure = structures.get(i).getAsJsonObject();
				String type = structure.get("type").getAsString();
				if ("table".equalsIgnoreCase(type)) {
					Table table = new Table();
					setTableAttributes(location, result, structure, type, table);
					result.getTables().add(table);
				} else if ("view".equalsIgnoreCase(type)) {
					View view = new View();
					setViewAttributes(location, result, structure, type, view);
					result.getViews().add(view);
				} else if ("foreignKey".equalsIgnoreCase(type)) {
					// skip for now
				} else {
					throw new IllegalArgumentException(format("Unknown data structure type [{0}] loaded from schema [{1}]", type, location));
				}
			}
			for (int i=0; i<structures.size(); i++) {
				JsonObject structure = structures.get(i).getAsJsonObject();
				String type = structure.get("type").getAsString();
				if ("foreignKey".equals(type)) {
					TableConstraintForeignKey foreignKey = new TableConstraintForeignKey();
					foreignKey.setName(structure.get("name").getAsString());
					foreignKey.setColumns(structure.get("columns").getAsString().split(","));
					foreignKey.setReferencedTable(structure.get("referencedTable").getAsString());
					foreignKey.setReferencedColumns(structure.get("referencedColumns").getAsString().split(","));
					String tableName = structure.get("table").getAsString();
					for (Table table : result.getTables()) {
						if (table.getName().equals(tableName)) {
							// add the foreign key
							List<TableConstraintForeignKey> list = new ArrayList<TableConstraintForeignKey>(); 
							if (table.getConstraints().getForeignKeys() != null ) { 
								list.addAll(table.getConstraints().getForeignKeys());
							}
							list.add(foreignKey);
							table.getConstraints().getForeignKeys().addAll(list);
							// add the dependency for the topological sorting later
							table.addDependency(location, foreignKey.getReferencedTable(), "TABLE");
							break;
						}
					}
				}
			}
		
		
		return result;
	}
	
	/**
	 * Sets the table attributes.
	 *
	 * @param location the location
	 * @param result the result
	 * @param structure the structure
	 * @param type the type
	 * @param table the table
	 */
	private static void setTableAttributes(String location, Schema result, JsonObject structure,
			String type, Table table) {
		table.setLocation(location);
		table.setName(structure.get("name").getAsString());
		table.setKind(type);
		table.setType(Table.ARTEFACT_TYPE);
		table.updateKey();
		JsonElement columnElement = structure.get("columns");
		if (columnElement.isJsonObject()) {
			JsonObject column = columnElement.getAsJsonObject();
			TableColumn columnModel = new TableColumn();
			setColumnAttributes(column, columnModel);
			columnModel.setTable(table);
			table.getColumns().add(columnModel);
		} else if (columnElement.isJsonArray()) {
			JsonArray columns = columnElement.getAsJsonArray();
			for (int j=0; j<columns.size(); j++) {
				JsonObject column = columns.get(j).getAsJsonObject();
				TableColumn columnModel = new TableColumn();
				setColumnAttributes(column, columnModel);
				columnModel.setTable(table);
				table.getColumns().add(columnModel);
			}
		} else {
			throw new IllegalArgumentException(format("Error in parsing columns of table [{0}] in schema [{1}]", table.getName(), location));
		}
	}

	/**
	 * Sets the column attributes.
	 *
	 * @param column the column
	 * @param columnModel the column model
	 */
	private static void setColumnAttributes(JsonObject column, TableColumn columnModel) {
		columnModel.setName(column.get("name") != null && !column.get("name").isJsonNull() ? column.get("name").getAsString() : "unknown");
		columnModel.setType(column.get("type") != null && !column.get("type").isJsonNull()  ? column.get("type").getAsString() : "unknown");
		columnModel.setLength(column.get("length") != null && !column.get("length").isJsonNull()  ? column.get("length").getAsString() : null);
		columnModel.setPrimaryKey(column.get("primaryKey") != null && !column.get("primaryKey").isJsonNull()  ? column.get("primaryKey").getAsBoolean() : false);
		columnModel.setUnique(column.get("unique") != null && !column.get("unique").isJsonNull()  ? column.get("unique").getAsBoolean() : false);
		columnModel.setNullable(column.get("nullable") != null && !column.get("nullable").isJsonNull()  ? column.get("nullable").getAsBoolean() : false);
		columnModel.setDefaultValue(column.get("defaultValue") != null && !column.get("defaultValue").isJsonNull()  ? column.get("defaultValue").getAsString() : null);
		columnModel.setScale(column.get("scale") != null && !column.get("scale").isJsonNull() ? column.get("scale").getAsString() : null);
	}
	
	/**
	 * Sets the view attributes.
	 *
	 * @param location the location
	 * @param result the result
	 * @param structure the structure
	 * @param type the type
	 * @param view the view
	 */
	private static void setViewAttributes(String location, Schema result, JsonObject structure,
			String type, View view) {
		view.setLocation(location);
		view.setName(structure.get("name").getAsString());
		view.setKind(type);
		view.setType(View.ARTEFACT_TYPE);
		view.setQuery(structure.get("columns").getAsJsonArray().get(0).getAsJsonObject().get("query").getAsString());
		view.updateKey();
	}

}