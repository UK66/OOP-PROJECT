package com.library.service;

import com.library.dao.IMemberDAO;
import com.library.dao.MemberDAO;
import com.library.model.Member;

import java.util.List;
import java.util.regex.Pattern;

public class MemberService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final IMemberDAO memberDAO;

    public MemberService() {
        this.memberDAO = new MemberDAO();
    }

    public void addMember(Member member) {
        validateMember(member);

        // Business rule: email must be unique — checked here for a clean
        // error message rather than letting the DB's UNIQUE constraint throw.
        boolean duplicate = memberDAO.getAllMembers().stream()
            .anyMatch(m -> m.getEmail().equalsIgnoreCase(member.getEmail()));
        if (duplicate) {
            throw new IllegalArgumentException("A member with email " + member.getEmail() + " already exists.");
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
        memberDAO.updateMember(member);
    }

    public void deleteMember(int memberId) {
        memberDAO.deleteMember(memberId);
    }

    // ── Validation ──────────────────────────────────────────
    private void validateMember(Member member) {
        if (member.getName() == null || member.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (member.getEmail() == null || !EMAIL_PATTERN.matcher(member.getEmail()).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
    }
}