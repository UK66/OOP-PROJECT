package com.library.service;

import com.library.dao.IMemberDAO;
import com.library.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MemberService Validation & Business Rule Tests")
class MemberServiceTest {

    private InMemoryMemberDAO memberDAO;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberDAO = new InMemoryMemberDAO();
        memberService = new MemberService(memberDAO);
    }

    @Test
    @DisplayName("addMember: Successfully registers new member")
    void testAddMemberSuccess() {
        Member member = new Member(0, "Alice Brown", "alice@test.org", "9876543210");
        memberService.addMember(member);

        assertEquals(1, memberService.getAllMembers().size());
        assertEquals("Alice Brown", memberService.getMemberById(1).getName());
    }

    @Test
    @DisplayName("addMember: Empty name or invalid email throws IllegalArgumentException")
    void testValidationNameAndEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                memberService.addMember(new Member(0, "", "alice@test.org", "123")));

        assertThrows(IllegalArgumentException.class, () ->
                memberService.addMember(new Member(0, "Alice", "not-an-email", "123")));

        assertThrows(IllegalArgumentException.class, () ->
                memberService.addMember(new Member(0, "Alice", "alice@domain", "123")));
    }

    @Test
    @DisplayName("addMember: Duplicate email on registration throws IllegalArgumentException")
    void testDuplicateEmailOnAdd() {
        memberService.addMember(new Member(0, "Alice Brown", "alice@test.org", "123"));

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                memberService.addMember(new Member(0, "Bob Smith", "alice@test.org", "456")));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("updateMember: Changing email to an existing member's email throws IllegalArgumentException")
    void testDuplicateEmailOnUpdate() {
        memberService.addMember(new Member(0, "Alice Brown", "alice@test.org", "123"));
        memberService.addMember(new Member(0, "Bob Smith", "bob@test.org", "456"));

        Member bob = memberService.getMemberById(2);
        bob.setEmail("alice@test.org");

        Exception ex = assertThrows(IllegalArgumentException.class, () -> memberService.updateMember(bob));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("updateMember: Saving same member with unchanged email succeeds")
    void testSameEmailOnUpdate() {
        memberService.addMember(new Member(0, "Alice Brown", "alice@test.org", "123"));

        Member alice = memberService.getMemberById(1);
        alice.setName("Alice B.");
        assertDoesNotThrow(() -> memberService.updateMember(alice));
        assertEquals("Alice B.", memberService.getMemberById(1).getName());
    }

    // ── In-Memory Stub ────────────────────────────────────────
    static class InMemoryMemberDAO implements IMemberDAO {
        private final List<Member> members = new ArrayList<>();
        private int idCounter = 1;

        @Override
        public void addMember(Member member) {
            member.setId(idCounter++);
            member.setMembershipDate(LocalDate.now());
            members.add(member);
        }

        @Override
        public Member getMemberById(int memberId) {
            return members.stream().filter(m -> m.getId() == memberId).findFirst().orElse(null);
        }

        @Override
        public List<Member> getAllMembers() {
            return new ArrayList<>(members);
        }

        @Override
        public void updateMember(Member member) {
            Member existing = getMemberById(member.getId());
            if (existing != null) {
                members.remove(existing);
                members.add(member);
            }
        }

        @Override
        public void deleteMember(int memberId) {
            members.removeIf(m -> m.getId() == memberId);
        }
    }
}
