package com.sfgroup81.tams.ui.mo;

import com.sfgroup81.tams.bootstrap.DataBootstrap;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.InternalReferralCsvRepository;
import com.sfgroup81.tams.repository.InterviewInvitationCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.SemesterCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.ApplicationReviewService;
import com.sfgroup81.tams.service.AuditLogService;
import com.sfgroup81.tams.service.CandidateInsightService;
import com.sfgroup81.tams.service.CandidateScreeningService;
import com.sfgroup81.tams.service.InterviewService;
import com.sfgroup81.tams.service.NotificationService;
import com.sfgroup81.tams.service.SecurityUtil;
import com.sfgroup81.tams.service.SemesterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MOCandidateManagementPanelTest {
    @TempDir
    Path tempDir;

    @Test
    void statusButtonsShouldRemainVisibleInCandidateManagementLayout() throws Exception {
        DataBootstrap.initialize(tempDir);
        UserCsvRepository userRepository = new UserCsvRepository(tempDir);
        User mo = userRepository.saveNewUser("MO Layout", "90020", "mo.layout@example.com",
                SecurityUtil.sha256("password123"), UserRole.MO);
        PositionCsvRepository positionRepository = new PositionCsvRepository(tempDir);
        TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository(tempDir);
        ApplicationStatusHistoryCsvRepository historyRepository = new ApplicationStatusHistoryCsvRepository(tempDir);
        InterviewInvitationCsvRepository interviewRepository = new InterviewInvitationCsvRepository(tempDir);
        AuditLogService auditLogService = AuditLogService.noop();
        NotificationService notificationService = NotificationService.noop();
        SemesterService semesterService = new SemesterService(new SemesterCsvRepository(tempDir), positionRepository, auditLogService);
        ApplicationReviewService reviewService = new ApplicationReviewService(
                applicationRepository,
                historyRepository,
                positionRepository,
                auditLogService,
                notificationService
        );
        InterviewService interviewService = new InterviewService(
                applicationRepository,
                historyRepository,
                interviewRepository,
                positionRepository,
                semesterService,
                auditLogService,
                notificationService
        );
        CandidateInsightService insightService = new CandidateInsightService(
                applicationRepository,
                userRepository,
                new ApplicantProfileCsvRepository(tempDir),
                positionRepository,
                new InternalReferralCsvRepository(tempDir),
                new TAFeedbackCsvRepository(tempDir),
                new ResumeFileCsvRepository(tempDir),
                auditLogService
        );
        CandidateScreeningService screeningService = new CandidateScreeningService(insightService, auditLogService);

        AtomicReference<MOCandidateManagementPanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> panelRef.set(new MOCandidateManagementPanel(
                mo,
                positionRepository,
                historyRepository,
                reviewService,
                interviewService,
                insightService,
                screeningService,
                auditLogService,
                semesterService,
                () -> {
                }
        )));

        assertButtonsVisible(panelRef.get(), 1080, 760);
        assertButtonsVisible(panelRef.get(), 900, 640);
        assertCoreAreasVisible(panelRef.get(), 1080, 760);
        assertCoreAreasVisible(panelRef.get(), 900, 640);
    }

    private void assertButtonsVisible(MOCandidateManagementPanel panel, int width, int height) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.setBounds(0, 0, width, height);
            layoutTree(panel);
            for (String text : List.of("Pending", "Hired", "Rejected")) {
                JButton button = findButton(panel, text);
                assertNotNull(button, text + " button should exist");
                Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), panel);
                assertTrue(bounds.y >= 0, text + " button should not be clipped above the panel");
                assertTrue(bounds.y + bounds.height <= panel.getHeight(), text + " button should not be clipped below the panel");
            }
        });
    }

    private void assertCoreAreasVisible(MOCandidateManagementPanel panel, int width, int height) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.setBounds(0, 0, width, height);
            layoutTree(panel);

            JTable table = findComponent(panel, JTable.class);
            assertNotNull(table, "candidate table should exist");
            JScrollPane tableScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            assertNotNull(tableScrollPane, "candidate table should be scrollable");
            Rectangle tableBounds = SwingUtilities.convertRectangle(tableScrollPane.getParent(), tableScrollPane.getBounds(), panel);
            assertTrue(tableBounds.width > 240, "candidate table should have usable width");
            assertTrue(tableBounds.height > 80, "candidate table should have usable height");

            for (String text : List.of("Export Ranked CSV", "Export Basic CSV", "View Resume", "Download Resumes")) {
                JButton button = findButton(panel, text);
                assertNotNull(button, text + " button should exist");
                Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), panel);
                assertTrue(bounds.y >= 0 && bounds.y + bounds.height <= panel.getHeight(), text + " button should stay visible");
            }

            JLabel comparisonTitle = findLabel(panel, "Candidate comparison");
            assertNotNull(comparisonTitle, "comparison section title should exist");
            Rectangle comparisonBounds = SwingUtilities.convertRectangle(comparisonTitle.getParent(), comparisonTitle.getBounds(), panel);
            assertTrue(comparisonBounds.y >= 0 && comparisonBounds.y + comparisonBounds.height <= panel.getHeight(),
                    "comparison section should stay visible");
        });
    }

    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static JButton findButton(JPanel panel, String text) {
        return findButton((Component) panel, text);
    }

    private static JButton findButton(Component component, String text) {
        if (component instanceof JButton button && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JLabel findLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel found = findLabel(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> T findComponent(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findComponent(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
