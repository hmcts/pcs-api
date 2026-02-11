package uk.gov.hmcts.reform.pcs.ccd.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.crypto.AEADBadTagException;
import javax.net.ssl.SSLException;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.befta.dse.ccd.CcdEnvironment;
import uk.gov.hmcts.befta.dse.ccd.CcdRoleConfig;
import uk.gov.hmcts.befta.dse.ccd.DataLoaderToDefinitionStore;
import uk.gov.hmcts.befta.exception.ImportException;
import uk.gov.hmcts.befta.util.BeftaUtils;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    private static final Logger logger = LoggerFactory.getLogger(HighLevelDataSetupApp.class);

    private static final CcdRoleConfig[] CCD_ROLES = {
        new CcdRoleConfig("caseworker-pcs", "PUBLIC"),
        new CcdRoleConfig("caseworker-pcs-solicitor", "PUBLIC"),
        new CcdRoleConfig("citizen", "PUBLIC")
    };


    private final CcdEnvironment environment;

    public HighLevelDataSetupApp(CcdEnvironment dataSetupEnvironment) {
        super(dataSetupEnvironment,"build/definitions/" );
        environment = dataSetupEnvironment;
    }

    public static void main(String[] args) throws Throwable {
        DataLoaderToDefinitionStore.main(HighLevelDataSetupApp.class, args);
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
        try (Stream<Path> paths = Files.list(Paths.get(definitionsPath))) {
            return paths
                .filter(Files::isRegularFile)
                .map(Path::toString) // optional filter
                .filter(string -> string.endsWith(".xlsx"))
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read definition files from " + definitionsPath, e);
        }
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
