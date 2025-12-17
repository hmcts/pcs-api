package uk.gov.hmcts.reform.pcs.ccd.service.nonprod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcement.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcement.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;
import uk.gov.hmcts.reform.pcs.factory.ClaimantPartyFactory;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@Profile({"local", "dev", "preview"})
public class EnforcementWarrantSupport extends MakeAClaimCaseGenerationSupport {

    static final String CASE_GENERATOR = "Create Enforcement Warrant Basic Case";
    private final CaseSupportHelper caseSupportHelper;
    private final EnforcementOrderService enforcementOrderService;
    private final ObjectMapper objectMapper;

    public EnforcementWarrantSupport(DraftCaseDataService draftCaseDataService,
                                     PcsCaseService pcsCaseService, ClaimantPartyFactory claimantPartyFactory,
                                     SecurityContextService securityContextService, ClaimService claimService,
                                     CaseSupportHelper caseSupportHelper,
                                     EnforcementOrderService enforcementOrderService, ObjectMapper objectMapper) {
        super(draftCaseDataService, pcsCaseService, claimantPartyFactory, securityContextService, claimService);
        this.caseSupportHelper = caseSupportHelper;
        this.enforcementOrderService = enforcementOrderService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String label) {
        return CASE_GENERATOR.equals(label);
    }

    @Override
    public CaseSupportGenerationResponse generate(long caseReference, PCSCase caseData, Resource nonProdResource) {
        log.info("Running : {}", CASE_GENERATOR);
        try {
            CaseSupportGenerationResponse generated = super.generate(caseReference, caseData,
                caseSupportHelper.getNonProdResource(super.getLabel()));
            log.info("Generated response from {}: {}", CASE_GENERATOR, generated);
            if ((generated.getErrors() == null || generated.getErrors().isEmpty())
                && generated.getState() == State.PENDING_CASE_ISSUED) {
                return generateEnforcementWarrant(caseReference, nonProdResource);
            }
        } catch (IOException e) {
            throw new SupportException(e);
        }
        return CaseSupportGenerationResponse.builder().state(State.PENDING_CASE_ISSUED).build();
    }

    private CaseSupportGenerationResponse generateEnforcementWarrant(long caseReference, Resource nonProdResource)
        throws IOException, UnsubmittedDataException {
        log.info("Back with the Enforcement generation now: {}", CASE_GENERATOR);
        String jsonString = StreamUtils.copyToString(nonProdResource.getInputStream(), StandardCharsets.UTF_8);
        EnforcementOrder enforcementOrder = parseCaseDataJson(jsonString);
        enforcementOrderService.saveAndClearDraftData(caseReference, enforcementOrder);
        return CaseSupportGenerationResponse.builder().state(State.PENDING_CASE_ISSUED).build();
    }

    @Override
    public String getLabel() {
        return CASE_GENERATOR;
    }

    public EnforcementOrder parseCaseDataJson(String caseDataJson) {
        try {
            return objectMapper.readValue(caseDataJson, EnforcementOrder.class);
        } catch (JsonProcessingException e) {
            log.error("Unable to parse draft case data JSON", e);
            throw new UnsubmittedDataException("Failed to read saved answers", e);
        }
    }

}
