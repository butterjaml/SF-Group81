package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.AuditLogCsvRepository;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkApplicationCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkPostingCsvRepository;
import com.sfgroup81.tams.repository.EnrollmentProfileSnapshotCsvRepository;
import com.sfgroup81.tams.repository.InterviewInvitationCsvRepository;
import com.sfgroup81.tams.repository.InternalReferralCsvRepository;
import com.sfgroup81.tams.repository.NotificationCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.SemesterCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.CandidateInsightService;
import com.sfgroup81.tams.service.CandidateScreeningService;
import com.sfgroup81.tams.service.CasualWorkService;
import com.sfgroup81.tams.service.InterviewService;
import com.sfgroup81.tams.service.ApplicationReviewService;
import com.sfgroup81.tams.service.ApplicationStatusService;
import com.sfgroup81.tams.service.AuditLogService;
import com.sfgroup81.tams.service.EnrollmentAutofillService;
import com.sfgroup81.tams.service.EnrollmentService;
import com.sfgroup81.tams.service.NotificationService;
import com.sfgroup81.tams.service.PositionService;
import com.sfgroup81.tams.service.ResumeUploadService;
import com.sfgroup81.tams.service.SemesterService;
import com.sfgroup81.tams.service.SessionContext;
import com.sfgroup81.tams.service.TAFeedbackService;
import com.sfgroup81.tams.service.UserManagementService;
import com.sfgroup81.tams.ui.admin.AdminAuditLogPanel;
import com.sfgroup81.tams.ui.admin.AdminCasualWorkPanel;
import com.sfgroup81.tams.ui.admin.AdminDashboardPanel;
import com.sfgroup81.tams.ui.admin.AdminSemesterManagementPanel;
import com.sfgroup81.tams.ui.admin.AdminUserManagementPanel;
import com.sfgroup81.tams.ui.auth.AuthLandingPanel;
import com.sfgroup81.tams.ui.mo.MODashboardPanel;
import com.sfgroup81.tams.ui.mo.MOCandidateManagementPanel;
import com.sfgroup81.tams.ui.mo.PositionManagePanel;
import com.sfgroup81.tams.ui.mo.TAFeedbackPanel;
import com.sfgroup81.tams.ui.ta.TAApplicationStatusPanel;
import com.sfgroup81.tams.ui.ta.TADashboardPanel;
import com.sfgroup81.tams.ui.ta.CasualWorkBrowserPanel;
import com.sfgroup81.tams.ui.ta.TAEnrollmentPanel;
import com.sfgroup81.tams.ui.ta.TAInterviewManagementPanel;
import com.sfgroup81.tams.ui.ta.TAJobBrowserPanel;
import com.sfgroup81.tams.ui.ta.TAJobDetailPanel;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class LoginFrame extends JFrame {
    private User currentUser;
    private final UserCsvRepository userRepository = new UserCsvRepository();
    private final PositionCsvRepository positionRepository = new PositionCsvRepository();
    private final ApplicantProfileCsvRepository profileRepository = new ApplicantProfileCsvRepository();
    private final ApplicationPreferenceCsvRepository preferenceRepository = new ApplicationPreferenceCsvRepository();
    private final ResumeFileCsvRepository resumeRepository = new ResumeFileCsvRepository();
    private final EnrollmentProfileSnapshotCsvRepository enrollmentSnapshotRepository = new EnrollmentProfileSnapshotCsvRepository();
    private final TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository();
    private final ApplicationStatusHistoryCsvRepository historyRepository = new ApplicationStatusHistoryCsvRepository();
    private final InterviewInvitationCsvRepository interviewRepository = new InterviewInvitationCsvRepository();
    private final CasualWorkPostingCsvRepository casualWorkPostingRepository = new CasualWorkPostingCsvRepository();
    private final CasualWorkApplicationCsvRepository casualWorkApplicationRepository = new CasualWorkApplicationCsvRepository();
    private final InternalReferralCsvRepository internalReferralRepository = new InternalReferralCsvRepository();
    private final TAFeedbackCsvRepository feedbackRepository = new TAFeedbackCsvRepository();
    private final AuditLogCsvRepository auditLogRepository = new AuditLogCsvRepository();
    private final NotificationCsvRepository notificationRepository = new NotificationCsvRepository();
    private final SemesterCsvRepository semesterRepository = new SemesterCsvRepository();
    private final AuditLogService auditLogService = new AuditLogService(auditLogRepository, userRepository);
    private final NotificationService notificationService = new NotificationService(notificationRepository);
    private final SemesterService semesterService = new SemesterService(semesterRepository, positionRepository, auditLogService);
    private final PositionService positionService = new PositionService(positionRepository, semesterService, auditLogService);
    private final ResumeUploadService resumeUploadService = new ResumeUploadService(java.nio.file.Path.of("data"), resumeRepository, userRepository);
    private final EnrollmentAutofillService enrollmentAutofillService = new EnrollmentAutofillService(
            profileRepository,
            applicationRepository,
            resumeRepository,
            enrollmentSnapshotRepository
    );
    private final EnrollmentService enrollmentService = new EnrollmentService(
            userRepository,
            positionRepository,
            profileRepository,
            resumeUploadService,
            applicationRepository,
            historyRepository,
            preferenceRepository,
            enrollmentSnapshotRepository,
            auditLogService
    );
    private final ApplicationStatusService applicationStatusService = new ApplicationStatusService(
            applicationRepository,
            historyRepository,
            positionRepository,
            semesterService,
            auditLogService
    );
    private final ApplicationReviewService reviewService = new ApplicationReviewService(
            applicationRepository,
            historyRepository,
            positionRepository,
            auditLogService,
            notificationService
    );
    private final InterviewService interviewService = new InterviewService(
            applicationRepository,
            historyRepository,
            interviewRepository,
            semesterService,
            auditLogService,
            notificationService
    );
    private final CasualWorkService casualWorkService = new CasualWorkService(
            casualWorkPostingRepository,
            casualWorkApplicationRepository,
            userRepository,
            applicationRepository,
            positionRepository,
            auditLogService
    );
    private final CandidateInsightService candidateInsightService = new CandidateInsightService(
            applicationRepository,
            userRepository,
            profileRepository,
            positionRepository,
            internalReferralRepository,
            feedbackRepository,
            resumeRepository,
            auditLogService
    );
    private final CandidateScreeningService candidateScreeningService = new CandidateScreeningService(candidateInsightService, auditLogService);
    private final TAFeedbackService feedbackService = new TAFeedbackService(
            feedbackRepository,
            applicationRepository,
            positionRepository,
            userRepository,
            auditLogService
    );
    private final UserManagementService userManagementService = new UserManagementService(userRepository, auditLogService);

    public LoginFrame() {
        setTitle("TA Management System");
        setSize(1080, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        showAuthLanding();
    }

    private void onLoginSuccess(User user) {
        this.currentUser = user;
        showRoleHome();
    }

    private void showAuthLanding() {
        setContentPane(new AuthLandingPanel(this::onLoginSuccess));
        refreshFrame();
    }

    private void showRoleHome() {
        if (currentUser == null) {
            showAuthLanding();
            return;
        }
        if (currentUser.role() == UserRole.TA) {
            setContentPane(new TADashboardPanel(
                    this::showTAJobs,
                    this::showTAStatus,
                    this::showTAInterviews,
                    this::showTACasualWork,
                    casualWorkService.canApplyCasualWork(currentUser.userId()),
                    () -> showTAEnrollment(null),
                    notificationService.listForUser(currentUser.userId()),
                    this::markAllNotificationsAsRead,
                    this::openNotificationTarget,
                    this::markNotificationAsRead,
                    this::logout
            ));
        } else if (currentUser.role() == UserRole.MO) {
            setContentPane(new MODashboardPanel(
                    this::showMOPositions,
                    this::showMOCandidates,
                    this::showMOFeedback,
                    feedbackService.listPendingAssignments(currentUser.userId()).size(),
                    this::logout
            ));
        } else {
            setContentPane(new AdminDashboardPanel(
                    this::showAdminUserManagement,
                    this::showAdminCasualWork,
                    this::showAdminSemesterArchive,
                    this::showAdminAuditLog,
                    this::logout
            ));
        }
        refreshFrame();
    }

    private void showTAJobs() {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened TA job browsing page");
        setContentPane(new TAJobBrowserPanel(positionService.listOpenPublishedPositions(), this::showRoleHome, this::showTAJobDetail, () -> showTAEnrollment(null)));
        refreshFrame();
    }

    private void showTAJobDetail(String positionId) {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Viewed TA job details for " + positionId);
        setContentPane(new TAJobDetailPanel(positionService.getById(positionId), this::showTAJobs, () -> showTAEnrollment(positionId)));
        refreshFrame();
    }

    private void showTAEnrollment(String preselectedPositionId) {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened TA one-stop registration");
        setContentPane(new TAEnrollmentPanel(
                currentUser,
                positionService.listOpenPublishedPositions(),
                profileRepository,
                applicationRepository,
                resumeRepository,
                enrollmentAutofillService,
                enrollmentService,
                this::showRoleHome,
                this::showTAStatus,
                preselectedPositionId
        ));
        refreshFrame();
    }

    private void showTAStatus() {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened TA application status page");
        setContentPane(new TAApplicationStatusPanel(currentUser, applicationStatusService, this::showRoleHome));
        refreshFrame();
    }

    private void showTAInterviews() {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened TA interview management");
        setContentPane(new TAInterviewManagementPanel(currentUser, interviewService, this::showRoleHome));
        refreshFrame();
    }

    private void showTACasualWork() {
        if (currentUser == null || !casualWorkService.canApplyCasualWork(currentUser.userId())) {
            showPlannedMessage("Casual work is available only to TAs hired for the current semester.");
            return;
        }
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened TA casual work browser");
        setContentPane(new CasualWorkBrowserPanel(currentUser, casualWorkService, this::showRoleHome));
        refreshFrame();
    }

    private void showMOPositions() {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened MO position management");
        setContentPane(new PositionManagePanel(positionService, this::showRoleHome));
        refreshFrame();
    }

    private void showMOCandidates() {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened MO candidate management");
        setContentPane(new MOCandidateManagementPanel(
                currentUser,
                positionRepository,
                historyRepository,
                reviewService,
                interviewService,
                candidateInsightService,
                candidateScreeningService,
                auditLogService,
                semesterService,
                this::showRoleHome
        ));
        refreshFrame();
    }

    private void showMOFeedback() {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened MO TA feedback panel");
        setContentPane(new TAFeedbackPanel(currentUser, feedbackService, this::showRoleHome));
        refreshFrame();
    }

    private void showAdminCasualWork() {
        auditLogService.record("DATA_ACCESS", currentUser.userId(), "Opened admin casual work management");
        setContentPane(new AdminCasualWorkPanel(currentUser, casualWorkService, userRepository, this::showRoleHome));
        refreshFrame();
    }

    private void showAdminUserManagement() {
        setContentPane(new AdminUserManagementPanel(currentUser, userManagementService, auditLogService, this::showRoleHome));
        refreshFrame();
    }

    private void showAdminSemesterArchive() {
        setContentPane(new AdminSemesterManagementPanel(
                currentUser,
                semesterService,
                positionRepository.findAll(),
                applicationRepository.findAll(),
                profileRepository.findAll(),
                this::showRoleHome
        ));
        refreshFrame();
    }

    private void showAdminAuditLog() {
        setContentPane(new AdminAuditLogPanel(currentUser, auditLogService, this::showRoleHome));
        refreshFrame();
    }

    private void openNotificationTarget(com.sfgroup81.tams.model.NotificationEntry notification) {
        if (notification == null) {
            return;
        }
        notificationService.markAsRead(notification.notificationId(), currentUser.userId());
        if ("TA_INTERVIEWS".equalsIgnoreCase(notification.relatedPage())) {
            showTAInterviews();
            return;
        }
        showTAStatus();
    }

    private void markNotificationAsRead(com.sfgroup81.tams.model.NotificationEntry notification) {
        if (notification == null) {
            return;
        }
        notificationService.markAsRead(notification.notificationId(), currentUser.userId());
        showRoleHome();
    }

    private void markAllNotificationsAsRead() {
        if (currentUser == null) {
            return;
        }
        notificationService.markAllAsRead(currentUser.userId());
        showRoleHome();
    }

    private void logout() {
        currentUser = null;
        SessionContext.clear();
        showAuthLanding();
    }

    private void refreshFrame() {
        revalidate();
        repaint();
    }

    private void showPlannedMessage(String message) {
        javax.swing.JOptionPane.showMessageDialog(this, message);
    }
}
