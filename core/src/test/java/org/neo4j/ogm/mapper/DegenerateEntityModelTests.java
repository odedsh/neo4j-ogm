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

package org.neo4j.ogm.mapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.domain.filesystem.Document;
import org.neo4j.ogm.domain.filesystem.Folder;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.GraphTestUtils;
import org.neo4j.ogm.testutil.IntegrationTestRule;

/**
 * These tests are to establish the behaviour of degenerate entity models
 *
 * An entity model is considered degenerate if a relationship that should
 * exist between two entities is only established on one of them.
 *
 * For example if a parent object maintains a list of child objects
 * but a child object maintains a null (or incorrect) reference to its parent
 * the entity model is degenerate.
 *
 * The OGM is designed to cope with such models.
 *
 * @author Vince Bickers
 *
 */
public class DegenerateEntityModelTests {

    private Folder f;
    private Document a;
    private Document b;

    @Rule
    public IntegrationTestRule testServer = new IntegrationTestRule(Components.driver());

    private static final SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.domain.filesystem");

    private Session session;

    @Before
    public void init() throws IOException {
        session = sessionFactory.openSession(testServer.driver());
        session.purgeDatabase();
        ExecutionEngine executionEngine = new ExecutionEngine(getDatabase());;
        ExecutionResult executionResult = executionEngine.execute(
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) " +
                        "CREATE (f)-[:CONTAINS]->(a) " +
                        "CREATE (f)-[:CONTAINS]->(b) " +
                        "RETURN id(f) AS fid, id(a) AS aid, id(b) AS bid");


        Map<String, Object> resultSet = executionResult.iterator().next();

        a = session.load(Document.class, (Long) resultSet.get("aid"));

        b = session.load(Document.class, (Long) resultSet.get("bid"));

        f = session.load(Folder.class, (Long) resultSet.get("fid"));

        f.getDocuments().add(a);
        f.getDocuments().add(b);

        a.setFolder(f);
        b.setFolder(f);
    }

    private GraphDatabaseService getDatabase() {
        return testServer.getGraphDatabaseService();
    }

    @Test
    public void testSaveDegenerateDocument() {

        // set a's f to a new f, but don't remove from the current f's list of documents
        a.setFolder(null);
        session.save(a);

        GraphTestUtils.assertSameGraph(getDatabase(),
                "CREATE (f:Folder {name : 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) " +
                        "CREATE (f)-[:CONTAINS]->(b)");
    }

    @Test
    public void testSaveDegenerateFolder() {

        // remove f's documents, but don't clear the documents' f reference
        f.setDocuments(new HashSet<Document>());

        session.save(f);

        GraphTestUtils.assertSameGraph(getDatabase(),
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) ");
    }


    @Test
    public void testSaveDegenerateDocumentClone() {

        Document clone = new Document();
        clone.setId(a.getId());
        clone.setName(a.getName());
        clone.setFolder(null);

        session.save(clone);

        GraphTestUtils.assertSameGraph(getDatabase(),
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a'} ) " +
                        "CREATE (b:Document { name: 'b'} ) " +
                        "CREATE (f)-[:CONTAINS]->(b)");
    }

    @Test
    public void testSaveDegenerateFolderClone() {

        Folder clone = new Folder();
        clone.setId(f.getId());
        clone.setName(f.getName());
        clone.setDocuments(new HashSet<Document>());

        session.save(clone);

        GraphTestUtils.assertSameGraph(getDatabase(),
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) ");
    }

    @Test
    public void testSaveChangedDocument() {

        // set a's f to a new f, but don't remove from the current f's list of documents
        a.setFolder(new Folder());
        a.getFolder().setName("g");

        session.save(a);

        GraphTestUtils.assertSameGraph(getDatabase(),
                "CREATE (f:Folder { name: 'f' } )" +
                        "CREATE (g:Folder { name: 'g' } ) " +
                        "CREATE (a:Document { name: 'a' }) " +
                        "CREATE (b:Document { name: 'b' }) " +
                        "CREATE (f)-[:CONTAINS]->(b) " +
                        "CREATE (g)-[:CONTAINS]->(a) ");

    }

    @Test
    public void testSaveChangedFolder() {

        Document c = new Document();
        c.setName("c");

        f.getDocuments().add(c);
        f.getDocuments().remove(a);

        session.save(f);

        GraphTestUtils.assertSameGraph(getDatabase(),
                "CREATE (f:Folder { name: 'f' })" +
                        "CREATE (a:Document { name: 'a' } ) " +
                        "CREATE (b:Document { name: 'b' } ) " +
                        "CREATE (c:Document { name: 'c' } ) " +
                        "CREATE (f)-[:CONTAINS]->(b) " +
                        "CREATE (f)-[:CONTAINS]->(c) ");

    }
}
