/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import org.hibernate.query.Query;

/**
 * The Class ObjectStore.
 */
@Component
public class DataStore {

    /** The session factory. */
    private SessionFactory sessionFactory;

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    /** The data source. */
    private DataSource dataSource;

    /** The mappings. */
    private final Map<String, String> mappings = new HashMap<>();

    /**
     * Instantiates a new object store.
     *
     * @param datasourcesManager the datasources manager
     */
    @Autowired
    public DataStore(DataSourcesManager datasourcesManager) {
        this.datasourcesManager = datasourcesManager;
    }

    /**
     * Gets the datasources manager.
     *
     * @return the datasources manager
     */
    public DataSourcesManager getDatasourcesManager() {
        return datasourcesManager;
    }

    /**
     * Gets the data source.
     *
     * @return the data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the data source.
     *
     * @param dataSource the new data source
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Adds the mapping.
     *
     * @param name the name
     * @param content the content
     */
    public void addMapping(String name, String content) {
        mappings.put(name, content);
    }

    /**
     * Removes the mapping.
     *
     * @param name the name
     */
    public void removeMapping(String name) {
        mappings.remove(name);
    }

    /**
     * Initialize.
     */
    public synchronized void initialize() {
        if (this.dataSource == null) {
            this.dataSource = datasourcesManager.getDefaultDataSource();
        }
        Configuration configuration = new Configuration().setProperty(Environment.SHOW_SQL, "true")
                                                         .setProperty("hibernate.hbm2ddl.auto", "update")
                                                         .setProperty("hibernate.current_session_context_class",
                                                                 "org.hibernate.context.internal.ThreadLocalSessionContext");

        mappings.forEach((k, v) -> addInputStreamToConfig(configuration, k, v));

        StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder();
        serviceRegistryBuilder.applySetting(Environment.DATASOURCE, this.dataSource);
        serviceRegistryBuilder.applySetting(Environment.JAKARTA_JTA_DATASOURCE, getDataSource());
        serviceRegistryBuilder.applySettings(configuration.getProperties());

        StandardServiceRegistry serviceRegistry = serviceRegistryBuilder.build();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    }

    /**
     * Adds the input stream to config.
     *
     * @param configuration the configuration
     * @param key the key
     * @param value the value
     */
    private void addInputStreamToConfig(Configuration configuration, String key, String value) {
        try (InputStream inputStream = IOUtils.toInputStream(EntityTransformer.fromEntity(value), StandardCharsets.UTF_8)) {
            configuration.addInputStream(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to add input stream to configuration for [" + key + "]: [" + value + "]", ex);
        }
    }

    /**
     * Save.
     *
     * @param type the type
     * @param json the json
     * @return the identifier
     */
    public Object save(String type, String json) {
        return save(type, json, getDataSource());
    }

    /**
     * Save.
     *
     * @param type the type
     * @param json the json
     * @param datasource the datasource
     * @return the identifier
     */
    public Object save(String type, String json, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Map object = JsonHelper.fromJson(json, Map.class);
            return save(type, object, datasource);
        }
    }

    /**
     * Save.
     *
     * @param type the type
     * @param object the object
     * @return the identifier
     */
    public Object save(String type, Map object) {
        return save(type, object, getDataSource());
    }

    /**
     * Save.
     *
     * @param type the type
     * @param object the object
     * @param datasource the datasource
     * @return the identifier
     */
    public Object save(String type, Map object, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Object id = session.save(type, object);
            transaction.commit();
            return id;
        }
    }

    /**
     * Save or update.
     *
     * @param type the type
     * @param json the json
     */
    public void upsert(String type, String json) {
    	upsert(type, json, getDataSource());
    }

    /**
     * Save or update.
     *
     * @param type the type
     * @param json the json
     * @param datasource the datasource
     */
    public void upsert(String type, String json, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Map object = JsonHelper.fromJson(json, Map.class);
            upsert(type, object, datasource);
        }
    }

    /**
     * Save or update.
     *
     * @param type the type
     * @param object the object
     */
    public void upsert(String type, Map object) {
    	upsert(type, object, getDataSource());
    }

    /**
     * Save or update.
     *
     * @param type the type
     * @param object the object
     * @param datasource the datasource
     */
    public void upsert(String type, Map object, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(type, object);
            transaction.commit();
        }
    }

    /**
     * Update.
     *
     * @param type the type
     * @param json the json
     */
    public void update(String type, String json) {
        update(type, json, getDataSource());
    }

    /**
     * Update.
     *
     * @param type the type
     * @param json the json
     * @param datasource the datasource
     */
    public void update(String type, String json, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Map object = JsonHelper.fromJson(json, Map.class);
            update(type, object, datasource);
        }
    }

    /**
     * Update.
     *
     * @param type the type
     * @param object the object
     */
    public void update(String type, Map object) {
        update(type, object, getDataSource());
    }

    /**
     * Update.
     *
     * @param type the type
     * @param object the object
     * @param datasource the datasource
     */
    public void update(String type, Map object, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.update(type, object);
            transaction.commit();
        }
    }

    /**
     * Delete.
     *
     * @param type the type
     * @param id the id
     */
    public void delete(String type, Serializable id) {
        delete(type, id, getDataSource());
    }

    /**
     * Delete.
     *
     * @param type the type
     * @param id the id
     * @param datasource the datasource
     */
    public void delete(String type, Serializable id, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            Object object = get(type, id);
            session.delete(type, object);
            transaction.commit();
        }
    }

    /**
     * Gets the.
     *
     * @param type the type
     * @param id the id
     * @return the map
     */
    public Map get(String type, Serializable id) {
        return get(type, id, getDataSource());
    }

    /**
     * Gets the.
     *
     * @param type the type
     * @param id the id
     * @param datasource the datasource
     * @return the map
     */
    public Map get(String type, Serializable id, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            return (Map) session.get(type, id);
        }
    }

    /**
     * Contains.
     *
     * @param type the type
     * @param json the json
     * @return true, if successful
     */
    public boolean contains(String type, String json) {
        return contains(type, json, getDataSource());
    }

    /**
     * Contains.
     *
     * @param type the type
     * @param json the json
     * @param datasource the datasource
     * @return true, if successful
     */
    public boolean contains(String type, String json, DataSource datasource) {
        try (Session session = sessionFactory.openSession()) {
            Map object = JsonHelper.fromJson(json, Map.class);
            return session.contains(type, object);
        }
    }

    /**
     * List.
     *
     * @param type the type
     * @return the list
     */
    public List<Map> list(String type) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {

            Query<Map> query = session.createQuery("from " + type + " c", Map.class);
            return query.getResultList();
        }
    }

    /**
     * Count.
     *
     * @param type the type
     * @return the count
     */
    public long count(String type) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {

            Query<Map> query = session.createQuery("from " + type + " c", Map.class);
            return query.getResultCount();
        }
    }

    /**
     * List with filter.
     *
     * @param type the type
     * @param options the options
     * @return the list
     */
    public List<Map> list(String type, QueryOptions options) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {

            List<Map> matchingItems = DynamicQueryFilter.list(entityManager, type, options);

            return matchingItems;
        }
    }

    /**
     * List with filter.
     *
     * @param type the type
     * @param options the options
     * @return the list
     */
    public List<Map> list(String type, String options) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {
            if (options != null) {
                QueryOptions queryOptions = JsonHelper.fromJson(options, QueryOptions.class);
                return list(type, queryOptions);
            }
            return list(type);
        }
    }

    /**
     * Count with filter.
     *
     * @param type the type
     * @param options the options
     * @return the count
     */
    public long count(String type, QueryOptions options) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {

            long count = DynamicQueryFilter.count(entityManager, type, options);

            return count;
        }
    }

    /**
     * Count with filter.
     *
     * @param type the type
     * @param options the options
     * @return the count
     */
    public long count(String type, String options) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {
            if (options != null) {
                QueryOptions queryOptions = JsonHelper.fromJson(options, QueryOptions.class);
                return count(type, queryOptions);
            }
            return count(type);
        }
    }


    /**
     * Find by example.
     *
     * @param type the type
     * @param json the json
     * @return the list
     */
    public List<Map> findByExample(String type, String json) {
        return findByExample(type, json, 100, 0, getDataSource());
    }

    /**
     * Find by example.
     *
     * @param type the type
     * @param json the json
     * @param limit the limit
     * @param offset the offset
     * @return the list
     */
    public List<Map> findByExample(String type, String json, int limit, int offset) {
        return findByExample(type, json, limit, offset, getDataSource());
    }

    /**
     * Find by example.
     *
     * @param type the type
     * @param json the json
     * @param limit the limit
     * @param offset the offset
     * @param datasource the datasource
     * @return the list
     */
    public List<Map> findByExample(String type, String json, int limit, int offset, DataSource datasource) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {
            Map object = JsonHelper.fromJson(json, Map.class);

            List result = DynamicCriteriaFinder.findByExampleDynamic(entityManager, type, object, limit, offset);

            return result;
        }
    }

    /**
     * Query.
     *
     * @param query the query
     * @param limit the limit
     * @param offset the offset
     * @return the list
     */
    public List<Map> query(String query, int limit, int offset) {
        try (Session session = sessionFactory.openSession();
                EntityManager entityManager = session.getEntityManagerFactory()
                                                     .createEntityManager()) {
            Query<Map> queryObject = session.createQuery(query, Map.class);
            if (limit > 0) {
                queryObject.setMaxResults(limit);
            }
            if (offset >= 0) {
                queryObject.setFirstResult(offset);
            }
            return queryObject.getResultList();
        }
    }

    /**
     * Query.
     *
     * @param query the query
     * @return the list
     */
    public List<Map> queryNative(String query) {
        try (Session session = sessionFactory.openSession()) {
            return session.createNativeQuery(query, Map.class)
                          .list();
        }
    }

}


