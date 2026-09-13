package com.library.model;

import java.time.LocalDate;

/**
 * Represents a library member.
 * Demonstrates: Inheritance (extends Person), Encapsulation.
 */
public class Member extends Person {

    private String    email;
    private LocalDate membershipDate;
    private boolean   isActive;

    // ── Constructors ───────────────────────────────────────
    public Member() {
        super();
        this.membershipDate = LocalDate.now();
        this.isActive       = true;
    }

    public Member(int id, String name, String email, String contact) {
        super(id, name, contact);
        this.email          = email;
        this.membershipDate = LocalDate.now();
        this.isActive       = true;
    }

    public Member(int id, String name, String email, String contact,
                  LocalDate membershipDate, boolean isActive) {
        super(id, name, contact);
        this.email          = email;
        this.membershipDate = membershipDate;
        this.isActive       = isActive;
    }

    // ── Polymorphism — implements abstract method ───────────
    @Override
    public String getDisplayInfo() {
        return String.format("Member[%d] %s <%s>", getId(), getName(), email);
    }

    // ── Getters & Setters ──────────────────────────────────
    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public LocalDate getMembershipDate()                     { return membershipDate; }
    public void setMembershipDate(LocalDate membershipDate)  { this.membershipDate = membershipDate; }

    public boolean isActive()                    { return isActive; }
    public void setActive(boolean active)        { this.isActive = active; }
}
