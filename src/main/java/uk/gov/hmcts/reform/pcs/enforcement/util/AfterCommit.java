package uk.gov.hmcts.reform.pcs.enforcement.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Defers work until the current transaction has committed, matching how
 * {@code DefendantResponseService} schedules its post-submit work.
 *
 * <p>A decentralised event's submit handler runs inside the transaction
 * {@code CaseSubmissionService} opens, so anything it triggers directly is visible to the outside world
 * before the case data it describes is committed. Work handed to {@link #run(Runnable)} instead runs after
 * that commit, and not at all if the transaction rolls back.</p>
 */
public final class AfterCommit {

    private AfterCommit() {
    }

    /**
     * Runs {@code work} once the current transaction commits, or immediately if there is no transaction.
     */
    public static void run(Runnable work) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    work.run();
                }
            });
        } else {
            work.run();
        }
    }
}
