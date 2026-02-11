package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder.EnforceTheOrder;
import uk.gov.hmcts.reform.pcs.ccd.page.testcasesupport.TestCaseSelectionPage;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseSupportException;
import uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestCaseSupportHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createTestCase;

@Component
@Slf4j
@RequiredArgsConstructor
public class TestCaseGeneration implements CCDConfig<PCSCase, State, UserRole> {

    static final String NO_NON_PROD_CASE_AVAILABLE = "No non-prod case json available.";
    static final String TEST_FEE_AMOUNT = "123.45";
    static final String EVENT_NAME = "Test Support Case Creation";
    static final String MAKE_A_CLAIM_CASE_GENERATOR = "Create Make A Claim Basic Case";
    static final String ENFORCEMENT_CASE_GENERATOR = "Create Enforcement Warrant Basic Case";

    private final ResumePossessionClaim resumePossessionClaim;
    private final EnforceTheOrder enforceTheOrder;

    private final TestCaseSupportHelper testCaseSupportHelper;

    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        log.info("Configuring non-production support event: {}", EVENT_NAME);
        if ("preview".equalsIgnoreCase(System.getenv().get("ENVIRONMENT"))
            || Boolean.parseBoolean(System.getenv().get("ENABLE_TESTING_SUPPORT"))) {
            log.info("Test support enabled, configuring event: {}", EVENT_NAME);
            configure(configBuilder);
        }
    }

    void configure(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createTestCase.name(), this::submit, this::start)
                .initialState(AWAITING_SUBMISSION_TO_HMCTS)
                .showSummary()
                .name(EVENT_NAME)
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);
        new PageBuilder(eventBuilder).add(new TestCaseSelectionPage());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setTestCaseSupportFileList(testCaseSupportHelper.getFileList());
        return caseData;
    }

    SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();
        DynamicList testFilesList = getTestFilesList(pcsCase);
        String label = testFilesList.getValue().getLabel();
        if (MAKE_A_CLAIM_CASE_GENERATOR.equalsIgnoreCase(label)) {
            makeAClaimTestCreation(label, caseReference);
        } else if (ENFORCEMENT_CASE_GENERATOR.equalsIgnoreCase(label)) {
            makeAClaimTestCreation(MAKE_A_CLAIM_CASE_GENERATOR, caseReference);
            System.gc();
            enforceTheOrder.submitOrder(caseReference, loadTestPcsCase(label));
        }
        return SubmitResponse.<State>builder().state(CASE_ISSUED).build();
    }

    void makeAClaimTestCreation(String label, Long caseReference) {
        PCSCase loadedCase = loadTestPcsCase(label);
        loadedCase.setFeeAmount(TEST_FEE_AMOUNT);
        pcsCaseService.createCase(
            caseReference, loadedCase.getPropertyAddress(),
            loadedCase.getLegislativeCountry());
        resumePossessionClaim.submitClaim(caseReference, loadedCase);
    }

    PCSCase loadTestPcsCase(String label) {
        try (InputStream inputStream = testCaseSupportHelper.getTestResource(label).getInputStream()) {
            String jsonString = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            return draftCaseDataService.parseCaseDataJson(jsonString);
        } catch (IOException e) {
            throw new TestCaseSupportException(e);
        }
    }

    public DynamicList getTestFilesList(PCSCase fromEvent) {
        return Optional.ofNullable(fromEvent.getTestCaseSupportFileList())
            .orElseThrow(() -> new IllegalArgumentException(NO_NON_PROD_CASE_AVAILABLE));
    }

}
