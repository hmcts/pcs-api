package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

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
import uk.gov.hmcts.reform.pcs.ccd.service.routing.AssuredTenancyRentSectionRoutingPolicy;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.IntroductoryDemotedOtherRentSectionRoutingPolicy;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentSectionRoutingService;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.SecureFlexibleRentSectionRoutingPolicy;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.SecureWalesRentSectionRoutingPolicy;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.StandardOrOtherWalesRentSectionRoutingPolicy;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentSectionRoutingService;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.AlternativesRules;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.ClaimantEligibilityRules;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.DailyRentCalculator;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.TenancyDateRules;
import uk.gov.hmcts.reform.pcs.ccd.util.MoneyFormatter;
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
 * Wires a real rental preparer around mocked reference-data lookups.
 */
final class RentalPreparerTestSupport {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneId.of("Europe/London"));

    private RentalPreparerTestSupport() {
    }

    static DefaultRentalBulkClaimPreparer preparer(BulkPropertyEligibility eligibility,
                                                   BulkClaimantOrganisationResolver resolver,
                                                   BulkClaimProperties properties,
                                                   FeatureToggleService featureToggleService) {
        PostcodeValidator postcodeValidator = new PostcodeValidator();
        BulkAddressMapper addressMapper = new BulkAddressMapper(postcodeValidator);
        GroundsRules groundsRules = new GroundsRules();
        AlternativesRules alternativesRules = new AlternativesRules();
        TenancyDateRules tenancyDateRules = new TenancyDateRules(CLOCK);
        RentalNoticeMapper noticeMapper = new RentalNoticeMapper(properties, CLOCK);
        return new DefaultRentalBulkClaimPreparer(
            properties,
            new BulkClaimXmlCodec(),
            new BulkPbaCheck(resolver),
            eligibility,
            addressMapper,
            new BulkClaimantMapper(addressMapper),
            new BulkDefendantMapper(addressMapper),
            new BulkUnderlesseeMapper(addressMapper),
            new BulkStatementOfTruthMapper(CLOCK),
            new BulkCommonParticularsMapper(),
            new GroundRouter(groundsRules),
            noticeMapper,
            new RentalRentMapper(new DailyRentCalculator(new MoneyFormatter())),
            new RentalAlternativesMapper(alternativesRules),
            new ClaimantEligibilityRules(),
            tenancyDateRules,
            new RentSectionRoutingService(List.of(
                new AssuredTenancyRentSectionRoutingPolicy(),
                new IntroductoryDemotedOtherRentSectionRoutingPolicy(),
                new SecureFlexibleRentSectionRoutingPolicy())),
            featureToggleService,
            new WalesGroundRouter(groundsRules),
            new WalesNoticeMapper(noticeMapper),
            new WalesOccupationContractMapper(tenancyDateRules),
            new WalesDetailsMapper(),
            new WalesRentSectionRoutingService(List.of(
                new StandardOrOtherWalesRentSectionRoutingPolicy(),
                new SecureWalesRentSectionRoutingPolicy()))
        );
    }

    static OrganisationDetailsResponse organisation() {
        return OrganisationDetailsResponse.builder()
            .name("Example Housing Association")
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
        try (InputStream in = RentalPreparerTestSupport.class.getResourceAsStream("/bulkclaim/v2/" + name)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
