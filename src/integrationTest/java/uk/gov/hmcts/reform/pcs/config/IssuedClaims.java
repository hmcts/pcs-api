package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport.Actor;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.testingsupport.service.BaseClaimPayloads;

/**
 * Issued possession claims, each created through pcs-api's own claim events by a claimant solicitor
 * from {@link #CLAIMANT_ORGANISATION}, then issued as the payment flow would.
 */
public class IssuedClaims {

    public static final String CLAIMANT_ORGANISATION = "Possession Claims Solicitor Org";
    /** The defendant named in the base claim. */
    public static final String DEFENDANT_NAME = "Jane Doe";

    private static final String CREATE_CLAIM = "createPossessionClaim";
    private static final String RESUME_CLAIM = "resumePossessionClaim";

    private final CcdEventTestSupport<PCSCase, State> events;
    private final TestOrganisations organisations;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    IssuedClaims(CcdEventTestSupport<PCSCase, State> events, TestOrganisations organisations,
                 ObjectMapper objectMapper, JdbcTemplate jdbc) {
        this.events = events;
        this.organisations = organisations;
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
    }

    /** The claim that pcs-api's testing support submits, at the property address it creates cases with. */
    public PCSCase baseClaim(LegislativeCountry country) {
        PCSCase claim = objectMapper.convertValue(BaseClaimPayloads.read(objectMapper, country), PCSCase.class);
        claim.setLegislativeCountry(country);
        claim.setPropertyAddress(BaseClaimPayloads.propertyAddress(country));
        return claim;
    }

    public long issue(LegislativeCountry country) {
        return issue(baseClaim(country));
    }

    public long issue(PCSCase claim) {
        Actor claimant = events.registerActor("Claimant", "Solicitor", "caseworker-pcs", "caseworker-pcs-solicitor");
        organisations.register(claimant, CLAIMANT_ORGANISATION);
        var create = events.create(CREATE_CLAIM, State.AWAITING_SUBMISSION_TO_HMCTS, PCSCase.builder()
                .propertyAddress(claim.getPropertyAddress())
                .legislativeCountry(claim.getLegislativeCountry())
                .build())
            .as(claimant);
        create.submitExpectingSuccess();
        events.event(create.reference(), RESUME_CLAIM, claim).as(claimant).submitExpectingSuccess();
        jdbc.update("update ccd.case_data set state = 'CASE_ISSUED' where reference = ?", create.reference());
        return create.reference();
    }
}
