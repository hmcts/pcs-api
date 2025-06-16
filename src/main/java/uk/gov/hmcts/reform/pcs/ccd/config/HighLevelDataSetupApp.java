package uk.gov.hmcts.reform.pcs.ccd.config;

import java.util.List;
import java.util.Locale;
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
import uk.gov.hmcts.reform.pcs.ccd.CaseType;

public class HighLevelDataSetupApp extends DataLoaderToDefinitionStore {

    private static final Logger logger = LoggerFactory.getLogger(HighLevelDataSetupApp.class);

    private static final CcdRoleConfig[] CCD_ROLES = {
        new CcdRoleConfig("caseworker-civil", "PUBLIC"),
    };


    private final CcdEnvironment environment;

    public HighLevelDataSetupApp(CcdEnvironment dataSetupEnvironment) {
        super(dataSetupEnvironment);
        environment = dataSetupEnvironment;
    }

    public static void main(String[] args) throws Throwable {
        DataLoaderToDefinitionStore.main(HighLevelDataSetupApp.class, args);
    }

    @Override
    public void addCcdRoles() {
        logger.info("Skipping CCD role creation in decentralised mode");
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
        return true;
    }

    @Override
    protected boolean shouldTolerateDataSetupFailure(Throwable e) {
        return true;
    }

    private boolean isGatewayTimeout(ImportException importException) {
        return importException.getHttpStatusCode() == HttpStatus.SC_GATEWAY_TIMEOUT;
    }
}
