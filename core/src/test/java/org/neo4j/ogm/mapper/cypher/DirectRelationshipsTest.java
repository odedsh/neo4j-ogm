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
package org.neo4j.ogm.mapper.cypher;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.compiler.Compiler;
import org.neo4j.ogm.domain.filesystem.Document;
import org.neo4j.ogm.domain.filesystem.Folder;
import org.neo4j.ogm.mapper.EntityGraphMapper;
import org.neo4j.ogm.mapper.EntityMapper;
import org.neo4j.ogm.mapper.MappedRelationship;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.request.Statement;
import org.neo4j.ogm.session.request.RowStatementFactory;

/**
 * This test suite contains tests of the cypher compiler output regarding
 * manipulation of direct relationships, i.e. relationships that are not
 * mediated in the entity model using RelationshipEntity instances.
 *
 * @see DATAGRAPH-588 - Relationship deletion problems
 *
 * @author Vince Bickers
 */
public class DirectRelationshipsTest {

    private EntityMapper mapper;
    private static MetaData mappingMetadata;
    private static MappingContext mappingContext;

    @BeforeClass
    public static void setUpTestDatabase() {
        mappingMetadata = new MetaData("org.neo4j.ogm.domain.filesystem");
        mappingContext = new MappingContext(mappingMetadata);
    }

    @Before
    public void setUpMapper() {
        this.mapper = new EntityGraphMapper(mappingMetadata, mappingContext);
    }

    @After
    public void tidyUp() {
        mappingContext.clear();
    }


    @Test
    public void shouldSaveNewFolderDocumentPair() {

        Folder folder = new Folder();
        Document document = new Document();

        folder.getDocuments().add(document);
        document.setFolder(folder);

        Compiler compiler = mapper.map(folder).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        List<String> createNodeStatements = cypherStatements(compiler.createNodesStatements());
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));

        List<String> createRelStatements = cypherStatements(compiler.createRelationshipsStatements());
        assertEquals(1, createRelStatements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId", createRelStatements.get(0));

        compiler = mapper.map(document).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());
        createNodeStatements = cypherStatements(compiler.createNodesStatements());
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));

        createRelStatements = cypherStatements(compiler.createRelationshipsStatements());
        assertEquals(1, createRelStatements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId", createRelStatements.get(0));


    }

    @Test
    public void shouldSaveNewFolderWithTwoDocuments() {

        Folder folder = new Folder();
        Document doc1 = new Document();
        Document doc2 = new Document();

        folder.getDocuments().add(doc1);
        folder.getDocuments().add(doc2);

        doc1.setFolder(folder);
        doc2.setFolder(folder);

        //save folder
        Compiler compiler = mapper.map(folder).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        List<Statement> statements = compiler.createNodesStatements();
        List<String> createNodeStatements = cypherStatements(statements);
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        for (Statement statement : statements) {
            List rows = (List) statement.getParameters().get("rows");
            if (statement.getStatement().contains("Folder")) {
                assertEquals(1, rows.size());
            }
            if (statement.getStatement().contains("Document")) {
                assertEquals(2, rows.size());
            }
        }

        statements = compiler.createRelationshipsStatements();
        List<String> createRelStatements = cypherStatements(statements);
        assertEquals(1, createRelStatements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId", createRelStatements.get(0));
        List rows = (List) statements.get(0).getParameters().get("rows");
        assertEquals(2, rows.size());

        //Save doc1
        compiler = mapper.map(doc1).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.createNodesStatements();
        createNodeStatements = cypherStatements(statements);
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        for (Statement statement : statements) {
            rows = (List) statement.getParameters().get("rows");
            if (statement.getStatement().contains("Folder")) {
                assertEquals(1, rows.size());
            }
            if (statement.getStatement().contains("Document")) {
                assertEquals(2, rows.size());
            }
        }

        //Save doc2
        statements = compiler.createRelationshipsStatements();
        createRelStatements = cypherStatements(statements);
        assertEquals(1, createRelStatements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId", createRelStatements.get(0));
        rows = (List) statements.get(0).getParameters().get("rows");
        assertEquals(2, rows.size());

        compiler = mapper.map(doc2).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.createNodesStatements();
        createNodeStatements = cypherStatements(statements);
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        for (Statement statement : statements) {
            rows = (List) statement.getParameters().get("rows");
            if (statement.getStatement().contains("Folder")) {
                assertEquals(1, rows.size());
            }
            if (statement.getStatement().contains("Document")) {
                assertEquals(2, rows.size());
            }
        }

        statements = compiler.createRelationshipsStatements();
        createRelStatements = cypherStatements(statements);
        assertEquals(1, createRelStatements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId", createRelStatements.get(0));
        rows = (List) statements.get(0).getParameters().get("rows");
        assertEquals(2, rows.size());
    }


    @Test
    public void shouldNotBeAbleToCreateDuplicateRelationship() {

        Folder folder = new Folder();
        Document document = new Document();

        document.setFolder(folder);

        // we try to store two identical references to the document object. Although this
        // is supported by the graph, it isn't currently supported by the OGM,
        // therefore we expect only one relationship to be persisted

        folder.getDocuments().add(document);
        folder.getDocuments().add(document);

        assertEquals(2, folder.getDocuments().size());

        //save folder
        Compiler compiler = mapper.map(folder).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        List<Statement> statements = compiler.createNodesStatements();
        List<String> createNodeStatements = cypherStatements(statements);
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        for (Statement statement : statements) {
            List rows = (List) statement.getParameters().get("rows");
            assertEquals(1, rows.size());
        }

        statements = compiler.createRelationshipsStatements();
        List<String> createRelStatements = cypherStatements(statements);
        assertEquals(1, createRelStatements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId", createRelStatements.get(0));
        List rows = (List) statements.get(0).getParameters().get("rows");
        assertEquals(1, rows.size());

        //save document
        compiler = mapper.map(document).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.createNodesStatements();
        createNodeStatements = cypherStatements(statements);
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        for (Statement statement : statements) {
            rows = (List) statement.getParameters().get("rows");
            assertEquals(1, rows.size());
        }

        statements = compiler.createRelationshipsStatements();
        createRelStatements = cypherStatements(statements);
        assertEquals(1, createRelStatements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId", createRelStatements.get(0));
        rows = (List) statements.get(0).getParameters().get("rows");
        assertEquals(1, rows.size());
    }

    @Test
    public void shouldBeAbleToCreateDifferentRelationshipsToTheSameDocument() {

        Folder folder = new Folder();
        Document document = new Document();

        document.setFolder(folder);

        folder.getDocuments().add(document);
        folder.getArchived().add(document);

        //save folder
        Compiler compiler = mapper.map(folder).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        List<Statement> statements = compiler.createNodesStatements();
        List<String> createNodeStatements = cypherStatements(statements);
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        for (Statement statement : statements) {
            List rows = (List) statement.getParameters().get("rows");
            assertEquals(1, rows.size());
        }

        statements = compiler.createRelationshipsStatements();
        List<String> createRelStatements = cypherStatements(statements);
        assertEquals(2, createRelStatements.size());
        assertTrue(createRelStatements.contains("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId"));
        assertTrue(createRelStatements.contains("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`ARCHIVED`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId"));
        boolean archivedType=false, containsType=false;
        for (Statement statement : statements) {
            if (statement.getStatement().contains("ARCHIVED")) {
                archivedType = true;
            }
            if (statement.getStatement().contains("CONTAINS")) {
                containsType = true;
            }
        }
        assertTrue(archivedType);
        assertTrue(containsType);

        //save document
        compiler = mapper.map(document).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.createNodesStatements();
        createNodeStatements = cypherStatements(statements);
        assertEquals(2, createNodeStatements.size());
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Folder`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));
        assertTrue(createNodeStatements.contains("UNWIND {rows} as row CREATE (n:`Document`) SET n=row.props RETURN row.nodeRef as nodeRef, ID(n) as nodeId"));

        statements = compiler.createRelationshipsStatements();
        createRelStatements = cypherStatements(statements);
        assertEquals(2, createRelStatements.size());
        assertTrue(createRelStatements.contains("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`CONTAINS`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId"));
        assertTrue(createRelStatements.contains("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`ARCHIVED`]->(endNode) RETURN row.relRef as relRefId, ID(rel) as relId"));
        archivedType=false;
        containsType=false;
        for (Statement statement : statements) {
            if (statement.getStatement().contains("ARCHIVED")) {
                archivedType = true;
            }
            if (statement.getStatement().contains("CONTAINS")) {
                containsType = true;
            }
        }
        assertTrue(archivedType);
        assertTrue(containsType);
    }


    @Test
    public void shouldBeAbleToRemoveTheOnlyRegisteredRelationship() {

        Folder folder = new Folder();
        Document document = new Document();

        folder.getDocuments().add(document);
        document.setFolder(folder);

        folder.setId(0L);
        document.setId(1L);

        mappingContext.registerNodeEntity(folder, folder.getId());
        mappingContext.registerNodeEntity(document, document.getId());
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", document.getId(), Folder.class, Document.class));

        mappingContext.remember(document);
        mappingContext.remember(folder);

        document.setFolder(null);
        folder.getDocuments().clear();

        Compiler compiler = mapper.map(folder).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        List<Statement> statements = compiler.deleteRelationshipStatements();
        assertEquals(1, statements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MATCH (startNode)-[rel:`CONTAINS`]->(endNode) DELETE rel", statements.get(0).getStatement());

        // we need to re-establish the relationship in the mapping context for this expectation, otherwise
        // the previous save will have de-registered the relationship.
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", document.getId(), Folder.class, Document.class));

        compiler = mapper.map(document).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.deleteRelationshipStatements();
        assertEquals(1, statements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MATCH (startNode)-[rel:`CONTAINS`]->(endNode) DELETE rel", statements.get(0).getStatement());
    }

    @Test
    public void shouldBeAbleToRemoveAnyRegisteredRelationship() {

        // given
        Folder folder = new Folder();
        Document doc1 = new Document();
        Document doc2 = new Document();

        folder.getDocuments().add(doc1);
        folder.getDocuments().add(doc2);
        doc1.setFolder(folder);
        doc2.setFolder(folder);

        folder.setId(0L);
        doc1.setId(1L);
        doc2.setId(2L);

        mappingContext.registerNodeEntity(folder, folder.getId());
        mappingContext.registerNodeEntity(doc1, doc1.getId());
        mappingContext.registerNodeEntity(doc2, doc2.getId());
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc1.getId(), Folder.class, Document.class));
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc2.getId(), Folder.class, Document.class));

        mappingContext.remember(doc1);
        mappingContext.remember(doc2);
        mappingContext.remember(folder);

        // when
        doc2.setFolder(null);
        folder.getDocuments().remove(doc2);


        // then
        assertEquals(1, folder.getDocuments().size());

        Compiler compiler = mapper.map(folder).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        List<Statement> statements = compiler.deleteRelationshipStatements();
        assertEquals(1, statements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MATCH (startNode)-[rel:`CONTAINS`]->(endNode) DELETE rel", statements.get(0).getStatement());

        // we need to re-establish the relationship in the mapping context for this expectation, otherwise
        // the previous save will have de-registered the relationship.
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc2.getId(), Folder.class, Document.class));
        compiler = mapper.map(doc1).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.deleteRelationshipStatements();
        assertEquals(1, statements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MATCH (startNode)-[rel:`CONTAINS`]->(endNode) DELETE rel", statements.get(0).getStatement());

        // we need to re-establish the relationship in the mapping context for this expectation, otherwise
        // the previous save will have de-registered the relationship.
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc2.getId(), Folder.class, Document.class));
        compiler = mapper.map(doc2).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.deleteRelationshipStatements();
        assertEquals(1, statements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MATCH (startNode)-[rel:`CONTAINS`]->(endNode) DELETE rel", statements.get(0).getStatement());
    }

    @Test
    public void shouldBeAbleToRemoveContainedRelationshipOnly() {

        // given
        Folder folder = new Folder();
        Document doc1 = new Document();

        folder.getDocuments().add(doc1);
        folder.getArchived().add(doc1);
        doc1.setFolder(folder);

        folder.setId(0L);
        doc1.setId(1L);

        mappingContext.registerNodeEntity(folder, folder.getId());
        mappingContext.registerNodeEntity(doc1, doc1.getId());
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "CONTAINS", doc1.getId(), Folder.class, Document.class));
        mappingContext.registerRelationship(new MappedRelationship(folder.getId(), "ARCHIVED", doc1.getId(), Folder.class, Document.class));

        mappingContext.remember(doc1);
        mappingContext.remember(folder);

        // when
        folder.getDocuments().remove(doc1);
        doc1.setFolder(null);
        // then
        assertEquals(0, folder.getDocuments().size());
        assertEquals(1, folder.getArchived().size());

        Compiler compiler = mapper.map(folder).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        List<Statement> statements = compiler.deleteRelationshipStatements();
        assertEquals(1, statements.size());
        assertEquals("UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MATCH (startNode)-[rel:`CONTAINS`]->(endNode) DELETE rel", statements.get(0).getStatement());

        //There are no more changes to the graph
        compiler = mapper.map(doc1).getCompiler();
        compiler.useStatementFactory(new RowStatementFactory());

        statements = compiler.deleteRelationshipStatements();
        assertEquals(0, statements.size());
    }

    private List<String> cypherStatements(List<Statement> statements) {
        List<String> cypher = new ArrayList<>(statements.size());
        for(Statement statement : statements) {
            cypher.add(statement.getStatement());
        }
        return cypher;
    }
}
