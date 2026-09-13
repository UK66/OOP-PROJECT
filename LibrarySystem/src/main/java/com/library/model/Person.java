package com.library.model;

/**
 * Abstract base class representing a Person.
 * Demonstrates: Abstraction, Inheritance (base for Member).
 */
public abstract class Person {

    private int id;
    private String name;
    private String contact;

    // ── Constructors ───────────────────────────────────────
    protected Person() {}

    protected Person(int id, String name, String contact) {
        this.id      = id;
        this.name    = name;
        this.contact = contact;
    }

    // ── Abstract method (Polymorphism hook) ────────────────
    /**
     * Returns a display label used in UI components.
     * Each subclass provides its own meaningful representation.
     */
    public abstract String getDisplayInfo();

    // ── Getters & Setters (Encapsulation) ──────────────────
    public int getId()                 { return id; }
    public void setId(int id)          { this.id = id; }

    public String getName()            { return name; }
    public void setName(String name)   { this.name = name; }

    public String getContact()               { return contact; }
    public void setContact(String contact)   { this.contact = contact; }

    @Override
    public String toString() {
        return getDisplayInfo();
    }
}
