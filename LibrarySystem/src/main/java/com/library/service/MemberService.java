package com.library.service;

import com.library.dao.IMemberDAO;
import com.library.dao.MemberDAO;
import com.library.exception.DatabaseException;
import com.library.model.Member;

import java.util.List;
import java.util.regex.Pattern;

public class MemberService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final IMemberDAO memberDAO;

    public MemberService() {
        this(new MemberDAO());
    }

    public MemberService(IMemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    public void addMember(Member member) {
        validateMember(member);

        // Business rule: email must be unique
        boolean duplicate = memberDAO.getAllMembers().stream()
                .anyMatch(m -> m.getEmail().equalsIgnoreCase(member.getEmail().trim()));
        if (duplicate) {
            throw new IllegalArgumentException("A member with email " + member.getEmail().trim() + " already exists.");
        }

        member.setEmail(member.getEmail().trim());
        member.setName(member.getName().trim());
        if (member.getContact() != null) {
            member.setContact(member.getContact().trim());
        }

        memberDAO.addMember(member);
    }

    public Member getMemberById(int memberId) {
        return memberDAO.getMemberById(memberId);
    }

    public List<Member> getAllMembers() {
        return memberDAO.getAllMembers();
    }

    public void updateMember(Member member) {
        validateMember(member);

        Member current = memberDAO.getMemberById(member.getId());
        if (current == null) {
            throw new IllegalArgumentException("Member no longer exists (ID: " + member.getId() + ").");
        }

        // Business rule: check duplicate email against other members
        boolean duplicate = memberDAO.getAllMembers().stream()
                .anyMatch(m -> m.getId() != member.getId() && m.getEmail().equalsIgnoreCase(member.getEmail().trim()));
        if (duplicate) {
            throw new IllegalArgumentException("Another member with email " + member.getEmail().trim() + " already exists.");
        }

        member.setEmail(member.getEmail().trim());
        member.setName(member.getName().trim());
        if (member.getContact() != null) {
            member.setContact(member.getContact().trim());
        }

        memberDAO.updateMember(member);
    }

    public void deleteMember(int memberId) {
        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found.");
        }

        try {
            memberDAO.deleteMember(memberId);
        } catch (DatabaseException e) {
            if (e.getErrorCode() == 1451 || e.getMessage().contains("borrowing") || e.getMessage().contains("related")) {
                throw new IllegalStateException(
                        "Cannot remove member \"" + member.getName() +
                                "\" because borrowing records exist for this member. You can deactivate their membership instead.");
            }
            throw e;
        }
    }

    // ── Validation ──────────────────────────────────────────
    private void validateMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member details cannot be empty.");
        }
        if (member.getName() == null || member.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (member.getEmail() == null || !EMAIL_PATTERN.matcher(member.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address (e.g., user@example.com).");
        }
    }
}