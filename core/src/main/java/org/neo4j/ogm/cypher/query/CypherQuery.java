/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.neo4j.ogm.cypher.query;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.request.Statement;

/**
 * Simple encapsulation of a Cypher query and its parameters and other optional parts (paging/sort).
 *
 * Note, this object will be transformed directly to JSON so don't add anything here that is
 * not part of the HTTP Transactional endpoint syntax
 *
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class CypherQuery implements Statement {

    private String statement;

    private int withIndex;

    private Map<String, Object> parameters = new HashMap<>();

    private Pagination paging;
    private SortOrder sortOrder = new SortOrder();
    private Filters filters = new Filters();


    /**
     * Constructs a new {@link CypherQuery} based on the given Cypher query string and query parameters.
     *
     * @param cypher The parameterised Cypher query string
     * @param parameters The name-value pairs that satisfy the parameters in the given query
     */
    public CypherQuery(String cypher, Map<String, ?> parameters) {
        this.statement = cypher;
        this.parameters.putAll(parameters);
        parseStatement();
    }

    public String getStatement() {

        String stmt = statement.trim();
        String sorting = sortOrder().toString();
        String pagination = paging == null ? "" : page().toString();

        // these transformations are entirely dependent on the form of our base queries and
        // binding the sorting properties to the default query variables is a terrible hack. All this
        // needs refactoring ASAP.
        if (sorting.length() > 0 || pagination.length() > 0) {

            if (withIndex > -1) {
                int nextClauseIndex = stmt.indexOf(" MATCH", withIndex);
                String withClause = stmt.substring(withIndex, nextClauseIndex);
                String newWithClause = withClause;
                if (stmt.contains(")-[r")) {
                    sorting = sorting.replace("$", "r");
                    if (!withClause.contains(",r")) {
                        newWithClause = newWithClause + ",r";
                    }
                } else {
                    sorting = sorting.replace("$", "n");
                }
                stmt = stmt.replace(withClause, newWithClause + sorting + pagination);
            } else {
                if (stmt.startsWith("MATCH p=(")) {
                    String withClause = "WITH p";
                    if (stmt.contains(")-[r")) {
                        withClause = withClause + ",r";
                        sorting = sorting.replace("$", "r");
                    } else {
                        sorting = sorting.replace("$", "n");
                    }
                    stmt = stmt.replace("RETURN ", withClause + sorting + pagination + " RETURN ");
                } else {
                    sorting = sorting.replace("$", "n");
                    stmt = stmt.replace("RETURN ", "WITH n" + sorting + pagination + " RETURN ");
                }
            }
        }

        return stmt;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String[] getResultDataContents() {
        return new String[0];
    }

    @Override
    public boolean isIncludeStats() {
        return false;
    }

    public Pagination page() {
        return paging;
    }

    public SortOrder sortOrder() {
        return sortOrder;
    }

    protected void addPaging(Pagination page) {
        this.paging = page;
    }

    public void addSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void addFilters(Filters filters) {
        this.filters = filters;
    }

    private void parseStatement() {
        this.withIndex = statement.indexOf("WITH n");
    }
}
