package uk.gov.hmcts.reform.pcs.ccd.config;

import java.util.List;
import java.util.Locale;
import javax.crypto.AEADBadTagException;
import javax.net.ssl.SSLException;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;
import uk.gov.hmcts.befta.dse.ccd.CcdRoleConfig;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.ImportException;
import uk.gov.hmcts.befta.util.BeftaUtils;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    private static final Logger logger = LoggerFactory.getLogger(HighLevelDataSetupApp.class);

    private static final CcdRoleConfig[] CCD_ROLES = {
        new CcdRoleConfig("caseworker-civil", "PUBLIC"),
    };

    private final CcdEnvironment environment;
    private static PostgreSQLContainer<?> postgres;

    public HighLevelDataSetupApp(CcdEnvironment dataSetupEnvironment) {
        super(dataSetupEnvironment);
        environment = dataSetupEnvironment;
    }

    public static void main(String[] args) throws Throwable {
        // Start PostgreSQL TestContainer
        startPostgreSQLContainer();

        // Set system properties for Spring to use
        setDatabaseSystemProperties();

        // Add shutdown hook to stop container
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (postgres != null && postgres.isRunning()) {
                logger.info("Stopping PostgreSQL TestContainer...");
                postgres.stop();
                logger.info("PostgreSQL TestContainer stopped");
            }
        }));

        try {
            DataLoaderToDefinitionStore.main(HighLevelDataSetupApp.class, args);
        } catch (Exception e) {
            logger.error("Error during data setup", e);
            // Ensure container is stopped even if there's an error
            if (postgres != null && postgres.isRunning()) {
                postgres.stop();
            }
            throw e;
        }
    }

    private static void startPostgreSQLContainer() {
        logger.info("Starting PostgreSQL TestContainer...");
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false); // Set to true if you want to reuse containers across runs

        postgres.start();
        logger.info("PostgreSQL TestContainer started successfully on {}", postgres.getJdbcUrl());
    }

    private static void setDatabaseSystemProperties() {
        // Set datasource properties
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");

        // Set JPA properties with different property names to avoid circular reference
        System.setProperty("database.platform", "org.hibernate.dialect.PostgreSQLDialect");
        System.setProperty("database.url", postgres.getJdbcUrl());
        System.setProperty("database.username", postgres.getUsername());
        System.setProperty("database.password", postgres.getPassword());
        System.setProperty("database.driver", "org.postgresql.Driver");

        logger.info("Database connection configured:");
        logger.info("  URL: {}", postgres.getJdbcUrl());
        logger.info("  Username: {}", postgres.getUsername());
        logger.info("  Driver: org.postgresql.Driver");
        logger.info("  Platform: org.hibernate.dialect.PostgreSQLDialect");
    }

    @Override
    public void addCcdRoles() {
        for (CcdRoleConfig roleConfig : CCD_ROLES) {
            try {
                logger.info("\n\nAdding CCD Role {}.", roleConfig);
                addCcdRole(roleConfig);
                logger.info("\n\nAdded CCD Role {}.", roleConfig);
            } catch (Exception e) {
                logger.error("\n\nCouldn't add CCD Role {}.\n\n", roleConfig, e);
                if (!shouldTolerateDataSetupFailure()) {
                    throw e;
                }
            }
        }
    }

    @Override
    protected List<String> getAllDefinitionFilesToLoadAt(String definitionsPath) {
        String environmentName = environment.name().toLowerCase(Locale.UK);
        return List.of(
            "build/definitions/CCD_Definition_" + CaseType.getCaseType() + "_" + environmentName + ".xlsx"
        );
    }

    @Override
    public void createRoleAssignments() {
        // Do not create role assignments.
        BeftaUtils.defaultLog("Will NOT create role assignments!");
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure() {
        var env = getDataSetupEnvironment();
        return CcdEnvironment.PERFTEST == env || CcdEnvironment.DEMO == env || CcdEnvironment.ITHC == env;
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure(Throwable e) {
        return switch (e) {
            case ImportException importException -> isGatewayTimeout(importException);
            case SSLException sslException -> true;
            case AEADBadTagException aeadBadTagException -> true;
            default -> shouldTolerateDataSetupFailure();
        };
    }

    private boolean isGatewayTimeout(ImportException importException) {
        return importException.getHttpStatusCode() == HttpStatus.SC_GATEWAY_TIMEOUT;
    }
}
