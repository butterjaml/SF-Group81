## Story Traceability Matrix

| Story | Branch | Acceptance Criteria | Verification | Status |
|---|---|---|---|---|
| #1 User Registration Pre-login | hh/auth-registration | Registration UI + user model + uniqueness checks (email/ID) + hashed password storage | `mvn -q -DskipTests compile` | Merged |
| #2 User Login & Role Identification | cj/login-role | Email+password login, users.csv authentication, session context, and role-based menu routing | `mvn -q -DskipTests compile` | Merged |
| #8 Job Posting & Management | zj/mo-job-posting | MO can create/edit/publish/draft TA positions and persist to ta_positions.csv | `mvn -q -DskipTests compile` | In Progress |
