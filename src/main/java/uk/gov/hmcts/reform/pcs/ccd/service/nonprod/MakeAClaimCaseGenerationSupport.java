package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class MakeAClaimCaseGenerationSupport implements TestCaseGenerationStrategy {

    static final String CASE_GENERATOR = "Create Make A Claim Basic Case";

    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;
    private final SecurityContextService securityContextService;

    @Override
    public CaseSupportGenerationResponse generate(long caseReference, PCSCase caseData, Resource nonProdResource) {
        log.info("Running : {}", CASE_GENERATOR);
        generateMakeAClaim(caseReference, nonProdResource);
        return new CaseSupportGenerationResponse(State.PENDING_CASE_ISSUED, List.of());
    }

    private void generateMakeAClaim(long caseReference, Resource nonProdResource) {
        try {
            String jsonString = StreamUtils.copyToString(nonProdResource.getInputStream(), StandardCharsets.UTF_8);
            PCSCase pcsCase = draftCaseDataService.parseCaseDataJson(jsonString);
            generateMakeAClaim(caseReference, pcsCase);
        } catch (IOException e) {
            throw new TestCaseSupportException(e);
        }
    }

    private void generateMakeAClaim(long caseReference, PCSCase pcsCase) {
        pcsCaseService.createCase(caseReference, pcsCase.getPropertyAddress(), pcsCase.getLegislativeCountry());
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

        pcsCaseService.mergeCaseData(pcsCaseEntity, pcsCase);
        UserInfo userDetails = securityContextService.getCurrentUserDetails();
        pcsCaseService.addClaimantPartyAndClaim(pcsCaseEntity, pcsCase, userDetails);
    }

    @Override
    public boolean supports(String label) {
        return CASE_GENERATOR.equals(label);
    }

    @Override
    public String getLabel() {
        return CASE_GENERATOR;
    }

}
