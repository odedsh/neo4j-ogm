package org.neo4j.ogm.domain.election;

/**
 * @author vince
 */
public abstract class Entity {

    private Long id;
    private String name;

    public Entity() {
    }

    public Entity(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
