package uk.gov.hmcts.reform.pcs.data.migration;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import uk.gov.hmcts.reform.pcs.exception.PendingMigrationScriptException;
import uk.gov.hmcts.reform.pcs.exception.RedactionContext;

import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.exception.ErrorCode.MIGRATION_NOT_YET_APPLIED;

public class FlywayNoOpStrategy implements FlywayMigrationStrategy {

    @Override
    public void migrate(Flyway flyway) {
        Stream.of(flyway.info().all())
            .filter(info -> !info.getState().isApplied())
            .findFirst()
            .ifPresent(info -> {
                throw new PendingMigrationScriptException(
                    MIGRATION_NOT_YET_APPLIED,
                    RedactionContext.of("Found migration not yet applied", info.getScript()));
            });
    }
}
