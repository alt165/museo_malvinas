package com.proveedores.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class TransactionalFileLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TransactionalFileLifecycle.class);

    public void deleteOnRollback(Runnable cleanup) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) runSafely(cleanup, "rollback");
            }
        });
    }

    public void deleteAfterCommit(Runnable cleanup) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runSafely(cleanup, "commit");
            }
        });
    }

    private void runSafely(Runnable cleanup, String phase) {
        try {
            cleanup.run();
        } catch (RuntimeException exception) {
            log.error("event=file.cleanup_failed phase={} exception={}", phase, exception.getClass().getSimpleName());
        }
    }
}
