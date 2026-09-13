package com.library.dao;

import com.library.DBConnection;
import com.library.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO implements IMemberDAO {

    @Override
    public void addMember(Member member) {
        String sql = "INSERT INTO members (name, email, contact, membership_date, is_active) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getContact());
            ps.setDate(4, Date.valueOf(member.getMembershipDate()));
            ps.setBoolean(5, member.isActive());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setId(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add member: " + e.getMessage(), e);
        }
    }

    @Override
    public Member getMemberById(int memberId) {
        String sql = "SELECT * FROM members WHERE member_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch member " + memberId + ": " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    public List<Member> getAllMembers() {
        String sql = "SELECT * FROM members ORDER BY name";
        List<Member> members = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                members.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch members: " + e.getMessage(), e);
        }

        return members;
    }

    @Override
    public void updateMember(Member member) {
        String sql = "UPDATE members SET name = ?, email = ?, contact = ?, is_active = ? WHERE member_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getContact());
            ps.setBoolean(4, member.isActive());
            ps.setInt(5, member.getId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("No member found with id " + member.getId() + " to update.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update member: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteMember(int memberId) {
        String sql = "DELETE FROM members WHERE member_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("No member found with id " + memberId + " to delete.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete member " + memberId + ": " + e.getMessage(), e);
        }
    }

    // ── Row mapping helper ──────────────────────────────────
    private Member mapRow(ResultSet rs) throws SQLException {
        return new Member(
                rs.getInt("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("contact"),
                rs.getDate("membership_date").toLocalDate(),
                rs.getBoolean("is_active"));
    }
}