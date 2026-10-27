package mrm.record;

/** Shared picker/submission predicate. The medical_records alias must be mr. */
public final class BorrowAvailability {
    private BorrowAvailability() { }
    public static final String NO_ACTIVE_REQUEST="""
        NOT EXISTS(SELECT 1 FROM reading_requests active_request WHERE active_request.record_id=mr.id
            AND (active_request.state IN ('SUBMITTED','NEEDS_INFO','PENDING_APPROVAL','APPROVED')
                OR (active_request.state='CONFIRMED' AND (
                    NOT EXISTS(SELECT 1 FROM reading_sessions old_session WHERE old_session.request_id=active_request.id)
                    OR EXISTS(SELECT 1 FROM reading_sessions open_session WHERE open_session.request_id=active_request.id
                        AND open_session.state IN ('REQUESTED','CONFIRMED','READING'))))))
        """;
}
