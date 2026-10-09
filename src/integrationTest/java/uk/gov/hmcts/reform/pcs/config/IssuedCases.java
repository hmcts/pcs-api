package uk.gov.hmcts.reform.pcs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.ccd.sdk.testing.CcdEventTestSupport;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.TenancyLicenceService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Issued possession cases, seeded straight into CCD and pcs-api's tables for tests of what happens
 * after a claim is issued. The claim is stored by pcs-api's own claim and tenancy mapping; its
 * parties are one claimant organisation and one defendant.
 */
@RequiredArgsConstructor
public class IssuedCases {

    public static final String CLAIMANT = "Possession Claims Solicitor Org";
    public static final String DEFENDANT = "Jane Doe";

    private final CcdEventTestSupport<PCSCase, State> events;
    private final PcsCaseRepository pcsCases;
    private final ClaimService claims;
    private final TenancyLicenceService tenancies;
    private final AddressMapper addresses;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transaction;

    /** The claim pcs-api's testing support creates cases with, for a test to adjust before issuing it. */
    @SneakyThrows
    public PCSCase claim(LegislativeCountry country) {
        String path = "/testing-support/Create-Case-" + country + "-Base.json";
        PCSCase claim;
        try (InputStream payload = IssuedCases.class.getResourceAsStream(path)) {
            claim = objectMapper.readValue(payload, PCSCase.class);
        }
        claim.setLegislativeCountry(country);
        claim.setPropertyAddress(AddressUK.builder()
            .addressLine1("1 Test Street")
            .postTown(country == LegislativeCountry.WALES ? "Cardiff" : "London")
            .postCode(country == LegislativeCountry.WALES ? "CF11 6QX" : "W3 7RX")
            .build());
        return claim;
    }

    public long issue() {
        return issue(claim(LegislativeCountry.ENGLAND));
    }

    public long issue(PCSCase claim) {
        return issue(claim, PartyEntity.builder().firstName("Jane").lastName("Doe").build());
    }

    public long issue(PCSCase claim, PartyEntity defendant) {
        long reference = events.seed(State.CASE_ISSUED, PCSCase.builder().build());
        transaction.executeWithoutResult(status -> storeClaim(reference, claim, defendant));
        return reference;
    }

    /** Changes an issued case's stored entities, as another event would. */
    public void update(long reference, Consumer<PcsCaseEntity> change) {
        transaction.executeWithoutResult(status ->
            change.accept(pcsCases.findByCaseReference(reference).orElseThrow()));
    }

    private void storeClaim(long reference, PCSCase claim, PartyEntity defendant) {
        PcsCaseEntity pcsCase = new PcsCaseEntity();
        pcsCase.setCaseReference(reference);
        pcsCase.setLegislativeCountry(claim.getLegislativeCountry());
        pcsCase.setPropertyAddress(addresses.toAddressEntityAndNormalise(claim.getPropertyAddress()));
        pcsCase.setTenancyLicence(tenancies.createTenancyLicenceEntity(claim));
        ClaimEntity mainClaim = claims.createMainClaimEntity(claim);
        pcsCase.addClaim(mainClaim);
        addParty(pcsCase, mainClaim, PartyEntity.builder().orgName(CLAIMANT).build(), PartyRole.CLAIMANT);
        addParty(pcsCase, mainClaim, defendant, PartyRole.DEFENDANT);
        pcsCases.save(pcsCase);
    }

    private static void addParty(PcsCaseEntity pcsCase, ClaimEntity claim, PartyEntity party, PartyRole role) {
        pcsCase.addParty(party);
        claim.addParty(party, role);
    }
}
