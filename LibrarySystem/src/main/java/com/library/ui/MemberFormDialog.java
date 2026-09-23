package com.library.ui;

import com.library.model.Member;

import javax.swing.*;
import java.awt.*;

public class MemberFormDialog extends JDialog {

    private JTextField nameField;
    private JTextField emailField;
    private JTextField contactField;
    private JCheckBox activeCheckBox;

    private final Member existingMember; // null when adding
    private Member resultMember;         // set only if user confirms with valid input

    public MemberFormDialog(Frame owner, Member existingMember) {
        super(owner, existingMember == null ? "Add Member" : "Edit Member", true);
        this.existingMember = existingMember;

        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        if (existingMember != null) {
            populateFields(existingMember);
        }

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JComponent buildFormPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));

        nameField = new JTextField();
        emailField = new JTextField();
        contactField = new JTextField();
        activeCheckBox = new JCheckBox();
        activeCheckBox.setSelected(true); // default for new members

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Contact:"));
        panel.add(contactField);
        panel.add(new JLabel("Active:"));
        panel.add(activeCheckBox);

        panel.setPreferredSize(new Dimension(320, 140));
        return panel;
    }

    private JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());

        panel.add(saveBtn);
        panel.add(cancelBtn);
        return panel;
    }

    private void populateFields(Member member) {
        nameField.setText(member.getName());
        emailField.setText(member.getEmail());
        contactField.setText(member.getContact());
        activeCheckBox.setSelected(member.isActive());
    }

    private void onSave() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String contact = contactField.getText().trim();
        boolean active = activeCheckBox.isSelected();

        // Field-level presence checks only; email FORMAT validation and
        // duplicate-email checks live in MemberService, not duplicated here.
        if (name.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Name and Email are required.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (existingMember == null) {
            // Adding: id=0 placeholder (DB assigns real id), joins today, active by default
            resultMember = new Member(0, name, email, contact);
            resultMember.setActive(active);
        } else {
            // Editing: preserve id and original membershipDate
            resultMember = existingMember;
            resultMember.setName(name);
            resultMember.setEmail(email);
            resultMember.setContact(contact);
            resultMember.setActive(active);
        }

        dispose();
    }

    /** Returns the member to save, or null if the user cancelled. */
    public Member getResultMember() {
        return resultMember;
    }
}