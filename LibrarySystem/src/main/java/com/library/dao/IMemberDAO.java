package com.library.dao;

import com.library.model.Member;
import java.util.List;

public interface IMemberDAO {
    void addMember(Member member);

    Member getMemberById(int memberId);

    List<Member> getAllMembers();

    void updateMember(Member member);

    void deleteMember(int memberId);
}