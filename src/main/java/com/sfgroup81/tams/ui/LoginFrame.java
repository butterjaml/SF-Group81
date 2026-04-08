package com.sfgroup81.tams.ui;

import com.sfgroup81.tams.model.UserRole;
import com.sfgroup81.tams.model.User;
import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.repository.ApplicantProfileCsvRepository;
import com.sfgroup81.tams.repository.ApplicationPreferenceCsvRepository;
import com.sfgroup81.tams.repository.ApplicationStatusHistoryCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkApplicationCsvRepository;
import com.sfgroup81.tams.repository.CasualWorkPostingCsvRepository;
import com.sfgroup81.tams.repository.InterviewInvitationCsvRepository;
import com.sfgroup81.tams.repository.InternalReferralCsvRepository;
import com.sfgroup81.tams.repository.PositionCsvRepository;
import com.sfgroup81.tams.repository.ResumeFileCsvRepository;
import com.sfgroup81.tams.repository.TAApplicationCsvRepository;
import com.sfgroup81.tams.repository.TAFeedbackCsvRepository;
import com.sfgroup81.tams.repository.UserCsvRepository;
import com.sfgroup81.tams.service.CandidateInsightService;
import com.sfgroup81.tams.service.CasualWorkService;
import com.sfgroup81.tams.service.InterviewService;
import com.sfgroup81.tams.service.ApplicationReviewService;
import com.sfgroup81.tams.service.ApplicationStatusService;
import com.sfgroup81.tams.service.EnrollmentService;
import com.sfgroup81.tams.service.PositionService;
import com.sfgroup81.tams.service.ResumeUploadService;
import com.sfgroup81.tams.service.SessionContext;
import com.sfgroup81.tams.service.TAFeedbackService;
import com.sfgroup81.tams.ui.admin.AdminCasualWorkPanel;
import com.sfgroup81.tams.ui.admin.AdminDashboardPanel;
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
    private final TAApplicationCsvRepository applicationRepository = new TAApplicationCsvRepository();
    private final ApplicationStatusHistoryCsvRepository historyRepository = new ApplicationStatusHistoryCsvRepository();
    private final InterviewInvitationCsvRepository interviewRepository = new InterviewInvitationCsvRepository();
    private final CasualWorkPostingCsvRepository casualWorkPostingRepository = new CasualWorkPostingCsvRepository();
    private final CasualWorkApplicationCsvRepository casualWorkApplicationRepository = new CasualWorkApplicationCsvRepository();
    private final InternalReferralCsvRepository internalReferralRepository = new InternalReferralCsvRepository();
    private final TAFeedbackCsvRepository feedbackRepository = new TAFeedbackCsvRepository();
    private final PositionService positionService = new PositionService(positionRepository);
    private final ResumeUploadService resumeUploadService = new ResumeUploadService(java.nio.file.Path.of("data"), resumeRepository, userRepository);
    private final EnrollmentService enrollmentService = new EnrollmentService(
            userRepository,
            positionRepository,
            profileRepository,
            resumeUploadService,
            applicationRepository,
            historyRepository,
            preferenceRepository
    );
    private final ApplicationStatusService applicationStatusService = new ApplicationStatusService(
            applicationRepository,
            historyRepository,
            positionRepository
    );
    private final ApplicationReviewService reviewService = new ApplicationReviewService(
            applicationRepository,
            historyRepository,
            positionRepository
    );
    private final InterviewService interviewService = new InterviewService(
            applicationRepository,
            historyRepository,
            interviewRepository
    );
    private final CasualWorkService casualWorkService = new CasualWorkService(
            casualWorkPostingRepository,
            casualWorkApplicationRepository,
            userRepository
    );
    private final CandidateInsightService candidateInsightService = new CandidateInsightService(
            applicationRepository,
            userRepository,
            profileRepository,
            positionRepository,
            internalReferralRepository,
            feedbackRepository
    );
    private final TAFeedbackService feedbackService = new TAFeedbackService(
            feedbackRepository,
            applicationRepository,
            positionRepository,
            userRepository
    );

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
                    currentUser.taCategory() == TACategory.NON_MODULAR,
                    () -> showTAEnrollment(null),
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
                    () -> showPlannedMessage("User management is planned for Sprint3."),
                    this::showAdminCasualWork,
                    () -> showPlannedMessage("Audit log is planned for Sprint3."),
                    this::logout
            ));
        }
        refreshFrame();
    }

    private void showTAJobs() {
        setContentPane(new TAJobBrowserPanel(positionService.listOpenPublishedPositions(), this::showRoleHome, this::showTAJobDetail, () -> showTAEnrollment(null)));
        refreshFrame();
    }

    private void showTAJobDetail(String positionId) {
        setContentPane(new TAJobDetailPanel(positionService.getById(positionId), this::showTAJobs, () -> showTAEnrollment(positionId)));
        refreshFrame();
    }

    private void showTAEnrollment(String preselectedPositionId) {
        setContentPane(new TAEnrollmentPanel(
                currentUser,
                positionService.listOpenPublishedPositions(),
                profileRepository,
                applicationRepository,
                resumeRepository,
                enrollmentService,
                this::showRoleHome,
                this::showTAStatus,
                preselectedPositionId
        ));
        refreshFrame();
    }

    private void showTAStatus() {
        setContentPane(new TAApplicationStatusPanel(currentUser, applicationStatusService, this::showRoleHome));
        refreshFrame();
    }

    private void showTAInterviews() {
        setContentPane(new TAInterviewManagementPanel(currentUser, interviewService, this::showRoleHome));
        refreshFrame();
    }

    private void showTACasualWork() {
        if (currentUser == null || currentUser.taCategory() != TACategory.NON_MODULAR) {
            showPlannedMessage("Casual work is available only to non-modular TAs.");
            return;
        }
        setContentPane(new CasualWorkBrowserPanel(currentUser, casualWorkService, this::showRoleHome));
        refreshFrame();
    }

    private void showMOPositions() {
        setContentPane(new PositionManagePanel(positionService, this::showRoleHome));
        refreshFrame();
    }

    private void showMOCandidates() {
        setContentPane(new MOCandidateManagementPanel(
                currentUser,
                positionRepository,
                applicationRepository,
                historyRepository,
                profileRepository,
                userRepository,
                reviewService,
                interviewService,
                candidateInsightService,
                this::showRoleHome
        ));
        refreshFrame();
    }

    private void showMOFeedback() {
        setContentPane(new TAFeedbackPanel(currentUser, feedbackService, this::showRoleHome));
        refreshFrame();
    }

    private void showAdminCasualWork() {
        setContentPane(new AdminCasualWorkPanel(currentUser, casualWorkService, userRepository, this::showRoleHome));
        refreshFrame();
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
