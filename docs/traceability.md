## Story Traceability Matrix

| Story | Branch | Acceptance Criteria | Verification | Status |
|---|---|---|---|---|
| #1 User Registration Pre-login | hh/auth-registration | Registration UI + user model + uniqueness checks (email/ID) + hashed password storage | `mvn -q -DskipTests compile` | Merged |
| #2 User Login & Role Identification | cj/login-role | Email+password login, users.csv authentication, session context, and role-based menu routing | `mvn -q -DskipTests compile` | Merged |
| #4 Course Selection | hr/course-selection | Course list from published positions, multi-select interaction, and selected-preference restore/save to `application_preferences.csv` | `mvn -q -DskipTests compile` | In Progress |
| #8 Job Posting & Management | zj/mo-job-posting | Create/edit/publish/draft, auto-close by deadline, manual unpublish, and status refresh | `mvn -q -DskipTests compile` | Ready for Review |
