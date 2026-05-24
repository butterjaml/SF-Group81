package com.sfgroup81.tams.ui.admin;

import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.service.AuditLogService;
import com.sfgroup81.tams.service.UserManagementService;
import com.sfgroup81.tams.service.UserUpsertRequest;
import com.sfgroup81.tams.ui.PrototypeUi;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class AdminUserManagementPanel extends JPanel {
    private final User currentUser;
    private final UserManagementService userManagementService;
    private final AuditLogService auditLogService;

    private final CardLayout tabLayout = new CardLayout();
    private final JPanel tabContainer = new JPanel(tabLayout);

    private final JTextField createNameField = new JTextField(22);
    private final JTextField createIdField = new JTextField(22);
    private final JTextField createEmailField = new JTextField(22);
    private final JPasswordField createPasswordField = new JPasswordField(22);
    private final JComboBox<UserRole> createRoleCombo = new JComboBox<>(UserRole.values());
    private final JComboBox<TACategory> createCategoryCombo = new JComboBox<>(new TACategory[]{TACategory.MODULAR, TACategory.NON_MODULAR});

    private final JTextField searchField = new JTextField(18);
    private final DefaultTableModel userTableModel = new DefaultTableModel(
            new Object[]{"User ID", "Name", "Email", "Role", "Category", "Status", "Workload", "Last Login"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable userTable = new JTable(userTableModel);
    private final JTextArea detailArea = new JTextArea();
    private final JComboBox<UserRole> editRoleCombo = new JComboBox<>(UserRole.values());
    private final JComboBox<TACategory> editCategoryCombo = new JComboBox<>(new TACategory[]{TACategory.MODULAR, TACategory.NON_MODULAR, TACategory.NONE});
    private final JPasswordField resetPasswordField = new JPasswordField(18);
    private final JButton toggleStatusButton = PrototypeUi.secondaryButton("Disable Account");

    private final JButton createTabButton = new JButton("Account creation form");
    private final JButton listTabButton = new JButton("List of created accounts");

    private List<User> displayedUsers = List.of();

    public AdminUserManagementPanel(User currentUser,
                                    UserManagementService userManagementService,
                                    AuditLogService auditLogService,
                                    Runnable onBack) {
        this.currentUser = currentUser;
        this.userManagementService = userManagementService;
        this.auditLogService = auditLogService;

        setLayout(new BorderLayout());
        setBackground(PrototypeUi.PANEL_BACKGROUND);
        add(PrototypeUi.createHeader("User management", onBack), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);

        auditLogService.record("USER_MANAGEMENT_VIEWED", currentUser.userId(), "Opened user management page");
        switchTab("create");
        refreshUserTable();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        tabContainer.setOpaque(false);
        tabContainer.add(buildCreateTab(), "create");
        tabContainer.add(buildListTab(), "list");
        content.add(tabContainer, BorderLayout.CENTER);
        content.add(buildBottomTabs(), BorderLayout.SOUTH);
        return content;
    }

    private JPanel buildCreateTab() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel formCard = PrototypeUi.createVerticalCard();
        formCard.setPreferredSize(new Dimension(540, 420));
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        addField(formCard, gbc, row++, "Full Name", createNameField);
        addField(formCard, gbc, row++, "Staff/Student ID", createIdField);
        addField(formCard, gbc, row++, "Email Address", createEmailField);
        addField(formCard, gbc, row++, "Temporary Password", createPasswordField);
        addField(formCard, gbc, row++, "Role", createRoleCombo);
        addField(formCard, gbc, row, "TA Category", createCategoryCombo);

        createRoleCombo.addActionListener(e -> refreshCreateCategoryState());
        refreshCreateCategoryState();

        JButton createButton = PrototypeUi.primaryButton("Create Account");
        createButton.addActionListener(e -> createUser());
        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(18, 8, 8, 8);
        formCard.add(createButton, gbc);

        wrapper.add(formCard);
        return wrapper;
    }

    private JPanel buildListTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setOpaque(false);

        JPanel filterBar = PrototypeUi.createCard();
        filterBar.setLayout(new BorderLayout(12, 12));
        JLabel hint = PrototypeUi.sectionTitle("Registered accounts");
        filterBar.add(hint, BorderLayout.WEST);
        JPanel searchPanel = new JPanel();
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search"));
        searchPanel.add(searchField);
        filterBar.add(searchPanel, BorderLayout.EAST);
        searchField.getDocument().addDocumentListener(refreshOnChange(this::refreshUserTable));
        panel.add(filterBar, BorderLayout.NORTH);

        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelectedUser();
            }
        });
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        JPanel right = PrototypeUi.createVerticalCard();
        right.setPreferredSize(new Dimension(340, 0));
        right.setLayout(new BorderLayout(8, 8));
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        right.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        right.add(buildEditControls(), BorderLayout.SOUTH);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildEditControls() {
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        addField(controls, gbc, row++, "Role", editRoleCombo);
        addField(controls, gbc, row++, "TA Category", editCategoryCombo);
        addField(controls, gbc, row++, "New Password", resetPasswordField);

        JButton saveRoleButton = PrototypeUi.primaryButton("Save Role");
        saveRoleButton.addActionListener(e -> saveRoleAndCategory());
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        controls.add(saveRoleButton, gbc);

        toggleStatusButton.addActionListener(e -> toggleStatus());
        gbc.gridy = row++;
        controls.add(toggleStatusButton, gbc);

        JButton resetPasswordButton = PrototypeUi.secondaryButton("Reset Password");
        resetPasswordButton.addActionListener(e -> resetPassword());
        gbc.gridy = row;
        controls.add(resetPasswordButton, gbc);

        editRoleCombo.addActionListener(e -> refreshEditCategoryState());
        return controls;
    }

    private JPanel buildBottomTabs() {
        JPanel tabs = new JPanel(new GridLayout(1, 2, 0, 0));
        tabs.setOpaque(false);
        styleTabButton(createTabButton, true);
        styleTabButton(listTabButton, false);
        createTabButton.addActionListener(e -> switchTab("create"));
        listTabButton.addActionListener(e -> switchTab("list"));
        tabs.add(createTabButton);
        tabs.add(listTabButton);
        return tabs;
    }

    private void styleTabButton(JButton button, boolean active) {
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        button.setBackground(active ? new Color(24, 118, 242) : new Color(226, 231, 239));
        button.setForeground(active ? Color.WHITE : new Color(102, 110, 126));
    }

    private void switchTab(String tab) {
        tabLayout.show(tabContainer, tab);
        boolean createActive = "create".equals(tab);
        styleTabButton(createTabButton, createActive);
        styleTabButton(listTabButton, !createActive);
        if (!createActive) {
            refreshUserTable();
        }
    }

    private void createUser() {
        try {
            userManagementService.createUser(new UserUpsertRequest(
                    createNameField.getText(),
                    createIdField.getText(),
                    createEmailField.getText(),
                    new String(createPasswordField.getPassword()),
                    (UserRole) createRoleCombo.getSelectedItem(),
                    (TACategory) createCategoryCombo.getSelectedItem()
            ), currentUser.userId());
            clearCreateForm();
            refreshUserTable();
            switchTab("list");
            JOptionPane.showMessageDialog(this, "User account created.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Create Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshUserTable() {
        displayedUsers = new ArrayList<>(userManagementService.listUsers(searchField.getText()));
        userTableModel.setRowCount(0);
        for (User user : displayedUsers) {
            // 如果是 TA 就计算工时，否则显示 "-"
            String workload = user.role() == UserRole.TA
                    ? userManagementService.calculateTAWorkload(user.userId()) + " hrs"
                    : "-";

            userTableModel.addRow(new Object[]{
                    user.userId(),
                    user.name(),
                    user.email(),
                    user.role(),
                    user.taCategory(),
                    user.status(),
                    workload, // 新增的 Workload 列数据
                    user.lastLoginAt().isBlank() ? "-" : user.lastLoginAt()
            });
        }
        if (!displayedUsers.isEmpty()) {
            userTable.setRowSelectionInterval(0, 0);
            renderSelectedUser();
        } else {
            detailArea.setText("No users match the current search.");
        }
    }

    private void renderSelectedUser() {
        User user = selectedUser();
        if (user == null) {
            detailArea.setText("");
            return;
        }

        String workloadStr = user.role() == UserRole.TA
                ? userManagementService.calculateTAWorkload(user.userId()) + " hrs"
                : "N/A";

        detailArea.setText("""
                User Details

                Name: %s
                User ID: %s
                Staff/Student ID: %s
                Email: %s
                Role: %s
                TA Category: %s
                Status: %s
                Current Workload: %s
                Last Login: %s
                """.formatted(
                user.name(),
                user.userId(),
                user.staffOrStudentId(),
                user.email(),
                user.role(),
                user.taCategory(),
                user.status(),
                workloadStr, // 插入计算出的工作量
                user.lastLoginAt().isBlank() ? "-" : user.lastLoginAt()
        ));

        editRoleCombo.setSelectedItem(user.role());
        editCategoryCombo.setSelectedItem(user.taCategory());
        toggleStatusButton.setText("ACTIVE".equalsIgnoreCase(user.status()) ? "Disable Account" : "Enable Account");
        refreshEditCategoryState();
    }

    private void saveRoleAndCategory() {
        User user = selectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        try {
            userManagementService.updateUserRoleAndCategory(
                    user.userId(),
                    (UserRole) editRoleCombo.getSelectedItem(),
                    (TACategory) editCategoryCombo.getSelectedItem(),
                    currentUser.userId()
            );
            refreshUserTable();
            JOptionPane.showMessageDialog(this, "User role updated.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleStatus() {
        User user = selectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        try {
            boolean active = !"ACTIVE".equalsIgnoreCase(user.status());
            userManagementService.updateAccountStatus(user.userId(), active, currentUser.userId());
            refreshUserTable();
            JOptionPane.showMessageDialog(this, "Account status updated.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Status Update Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetPassword() {
        User user = selectedUser();
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.");
            return;
        }
        try {
            userManagementService.resetPassword(user.userId(), new String(resetPasswordField.getPassword()), currentUser.userId());
            resetPasswordField.setText("");
            JOptionPane.showMessageDialog(this, "Password reset successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Password Reset Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearCreateForm() {
        createNameField.setText("");
        createIdField.setText("");
        createEmailField.setText("");
        createPasswordField.setText("");
        createRoleCombo.setSelectedItem(UserRole.TA);
        createCategoryCombo.setSelectedItem(TACategory.MODULAR);
        refreshCreateCategoryState();
    }

    private void refreshCreateCategoryState() {
        boolean taSelected = createRoleCombo.getSelectedItem() == UserRole.TA;
        createCategoryCombo.setEnabled(taSelected);
        if (!taSelected) {
            createCategoryCombo.setSelectedItem(TACategory.MODULAR);
        }
    }

    private void refreshEditCategoryState() {
        boolean taSelected = editRoleCombo.getSelectedItem() == UserRole.TA;
        editCategoryCombo.setEnabled(taSelected);
        if (!taSelected) {
            editCategoryCombo.setSelectedItem(TACategory.NONE);
        }
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
    }

    private User selectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= displayedUsers.size()) {
            return null;
        }
        return displayedUsers.get(row);
    }

    private DocumentListener refreshOnChange(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        };
    }
}
