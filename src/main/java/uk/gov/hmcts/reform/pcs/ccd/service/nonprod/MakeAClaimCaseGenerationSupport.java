package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.factory.ClaimantPartyFactory;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
public class MakeAClaimCaseGenerationSupport implements TestCaseGenerationStrategy {

    static final String CASE_GENERATOR = "Create Make A Claim Basic Case";

    private final DraftCaseDataService draftCaseDataService;
    private final PcsCaseService pcsCaseService;
    private final ClaimantPartyFactory claimantPartyFactory;
    private final SecurityContextService securityContextService;
    private final ClaimService claimService;

    @Override
    @Transactional
    public CaseSupportGenerationResponse generate(long caseReference, PCSCase caseData, Resource nonProdResource) {
        log.info("Running : {}", CASE_GENERATOR);
        generateMakeAClaim(caseReference, nonProdResource);
        return CaseSupportGenerationResponse.builder().state(State.PENDING_CASE_ISSUED).build();
    }

    private void generateMakeAClaim(long caseReference, Resource nonProdResource) {
        try {
            String jsonString = StreamUtils.copyToString(nonProdResource.getInputStream(), StandardCharsets.UTF_8);
            PCSCase pcsCase = draftCaseDataService.parseCaseDataJson(jsonString);
            pcsCaseService.createCase(caseReference, pcsCase.getPropertyAddress(), pcsCase.getLegislativeCountry());

            PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);

            pcsCaseService.mergeCaseData(pcsCaseEntity, pcsCase);
            UserInfo userDetails = securityContextService.getCurrentUserDetails();
            PartyEntity claimantPartyEntity = claimantPartyFactory
                .createAndPersistClaimantParty(pcsCase,
                                               new ClaimantPartyFactory.ClaimantPartyContext(
                                                   UUID.fromString(userDetails.getUid()),
                                                   userDetails.getSub()
                                               ));
            pcsCaseEntity.addParty(claimantPartyEntity);
            pcsCaseEntity.addClaim(claimService.createMainClaimEntity(pcsCase, claimantPartyEntity));
            pcsCaseService.save(pcsCaseEntity);
        } catch (IOException e) {
            throw new SupportException(e);
        }
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
