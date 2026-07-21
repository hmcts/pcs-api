package uk.gov.hmcts.reform.pcs.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * R1A one-time reset (HDPI-7834). Runs before Flyway validation on every boot.
 * If the database still carries a pre-baseline Flyway history (the old incremental
 * V001-V135 world, or an intermediate branch build), clears pcs-api's objects so the
 * consolidated baseline V001__r1a_baseline_schema.sql applies to a clean, empty schema.
 * No-ops forever once the baseline row is present. Never touches the ccd schema.
 * Delete this class in a cleanup PR once all environments are on the baseline.
 */
@Slf4j
@Component
public class R1aResetCallback implements Callback {

    private static final String RESET_SQL = """
        DO $$
        DECLARE
            r RECORD;
        BEGIN
            -- Nested IFs: PL/pgSQL plans the whole expression before evaluating it, so the
            -- NOT EXISTS must not be planned when flyway_schema_history is absent (fresh DB).
            IF to_regclass('public.flyway_schema_history') IS NOT NULL THEN
              IF NOT EXISTS (SELECT 1 FROM public.flyway_schema_history
                             WHERE script = 'V001__r1a_baseline_schema.sql') THEN

                RAISE NOTICE 'R1A reset: pre-baseline schema detected - clearing public + draft';

                FOR r IN SELECT viewname FROM pg_views WHERE schemaname = 'public' LOOP
                    EXECUTE format('DROP VIEW IF EXISTS public.%I CASCADE', r.viewname);
                END LOOP;

                FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
                    EXECUTE format('DROP TABLE IF EXISTS public.%I CASCADE', r.tablename);
                END LOOP;

                FOR r IN SELECT sequencename FROM pg_sequences WHERE schemaname = 'public' LOOP
                    EXECUTE format('DROP SEQUENCE IF EXISTS public.%I CASCADE', r.sequencename);
                END LOOP;

                FOR r IN SELECT p.oid::regprocedure AS sig FROM pg_proc p
                         JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'public' LOOP
                    EXECUTE format('DROP FUNCTION IF EXISTS %s CASCADE', r.sig);
                END LOOP;

                FOR r IN SELECT t.typname FROM pg_type t
                         JOIN pg_namespace n ON n.oid = t.typnamespace
                         WHERE n.nspname = 'public' AND t.typtype = 'e' LOOP
                    EXECUTE format('DROP TYPE IF EXISTS public.%I CASCADE', r.typname);
                END LOOP;

                DROP SCHEMA IF EXISTS draft CASCADE;

                RAISE NOTICE 'R1A reset: done - public emptied, draft removed; baseline will now apply';
              END IF;
            END IF;
        END $$""";

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_VALIDATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute(RESET_SQL);
        } catch (SQLException e) {
            throw new IllegalStateException("R1A reset callback failed", e);
        }
    }

    @Override
    public String getCallbackName() {
        return "r1aReset";
    }
}
