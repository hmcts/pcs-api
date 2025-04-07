package uk.gov.hmcts.reform.pcs.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestContainerHolder {

    private static final PostgreSQLContainer<?> container;

    static {
        container = new PostgreSQLContainer<>("postgres:17.4").withReuse(true);
        container.start();
    }

    public static PostgreSQLContainer<?> getInstance() {
        return container;
    }

}
