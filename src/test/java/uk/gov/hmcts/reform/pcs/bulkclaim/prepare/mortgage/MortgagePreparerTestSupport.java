package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.mortgage;

import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkAddressMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkClaimantMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkCommonParticularsMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkDefendantMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPbaCheck;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkStatementOfTruthMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkUnderlesseeMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.BulkClaimXmlCodec;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Wires a real mortgage preparer around mocked reference-data lookups.
 */
final class MortgagePreparerTestSupport {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneId.of("Europe/London"));

    private MortgagePreparerTestSupport() {
    }

    static DefaultMortgageBulkClaimPreparer preparer(BulkPropertyEligibility eligibility,
                                                     BulkClaimantOrganisationResolver resolver,
                                                     FeatureToggleService featureToggleService,
                                                     BulkClaimProperties properties) {
        BulkAddressMapper addressMapper = new BulkAddressMapper(new PostcodeValidator());
        return new DefaultMortgageBulkClaimPreparer(
            properties,
            new BulkClaimXmlCodec(),
            featureToggleService,
            new BulkPbaCheck(resolver),
            eligibility,
            addressMapper,
            new BulkClaimantMapper(addressMapper),
            new BulkDefendantMapper(addressMapper),
            new BulkUnderlesseeMapper(addressMapper),
            new BulkStatementOfTruthMapper(CLOCK),
            new BulkCommonParticularsMapper(),
            new MortgageParticularsMapper(CLOCK)
        );
    }

    static OrganisationDetailsResponse organisation() {
        return OrganisationDetailsResponse.builder()
            .name("Example Bank plc")
            .organisationIdentifier("ORG1")
            .organisationProfileIds(List.of("SOLICITOR_PROFILE"))
            .status("ACTIVE")
            .paymentAccount(List.of("PBA1234567", "PBA7654321"))
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                                            .addressLine1("1 Org Street")
                                            .townCity("London")
                                            .postCode("SW1A 1AA")
                                            .build()))
            .superUser(OrganisationDetailsResponse.SuperUser.builder().email("admin@example.org").build())
            .build();
    }

    static byte[] fixture(String name) {
        try (InputStream in = MortgagePreparerTestSupport.class.getResourceAsStream("/bulkclaim/v2/" + name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
