package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.org.BulkClaimantOrganisationResolver;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility.PropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.AddressType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimantType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ContactDetailsType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.DefendantType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.LegislativeCountryType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageDefendantType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NameType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.SigneeStatusType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.SolicitorType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.StatementOfTruthType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.UnderlesseeOrMortgageeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.service.EligibilityService;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BulkCommonMappersTest {

    private static final Clock CLOCK = Clock.fixed(
        LocalDate.of(2026, 11, 19).atStartOfDay(ZoneId.of("Europe/London")).toInstant(), ZoneId.of("Europe/London"));

    private final BulkAddressMapper addressMapper = new BulkAddressMapper(new PostcodeValidator());
    private final BulkErrorCollector errors = BulkErrorCollector.forClaimPath("/RentalClaimSet/RentalClaim[2]");

    private static AddressType address(String postcode) {
        AddressType address = new AddressType();
        address.setAddressLine1("1 Street");
        address.setPostTown("Town");
        address.setPostCode(postcode);
        return address;
    }

    private static ContactDetailsType contact(String postcode) {
        ContactDetailsType contact = new ContactDetailsType();
        contact.setAddress(address(postcode));
        return contact;
    }

    private static NameType name(String first, String last) {
        NameType name = new NameType();
        name.setFirstName(first);
        name.setLastName(last);
        return name;
    }

    private static OrganisationDetailsResponse solicitorOrganisation(String name) {
        OrganisationDetailsResponse organisation = organisation(name);
        organisation.setOrganisationProfileIds(List.of("SOLICITOR_PROFILE"));
        return organisation;
    }

    private static OrganisationDetailsResponse organisation(String name) {
        return OrganisationDetailsResponse.builder()
            .name(name)
            .superUser(OrganisationDetailsResponse.SuperUser.builder().email("admin@example.org").build())
            .contactInformation(List.of(OrganisationDetailsResponse.ContactInformation.builder()
                                            .addressLine1("1 Org Street").townCity("London")
                                            .postCode("SW1A 1AA").build()))
            .build();
    }

    // ---- error collector ----

    @Test
    void errorPathsAreOneBasedAndClaimQualified() {
        errors.add(BulkErrorCollector.indexed("Ground", 0) + "/Code", "X", "m");

        assertThat(errors.errors().getFirst().path()).isEqualTo("/RentalClaimSet/RentalClaim[2]/Ground[1]/Code");
    }

    @Test
    void errorCollectorChecksRequiredAndLength() {
        assertThat(errors.required("A", " ", "A")).isFalse();
        assertThat(errors.maxLength("B", "abcd", 3, "B")).isFalse();
        assertThat(errors.maxLength("C", "abc", 3, "C")).isTrue();
        assertThat(errors.errors()).extracting(e -> e.code())
            .containsExactly(BulkErrorCollector.REQUIRED, BulkErrorCollector.TOO_LONG);
    }

    // ---- address ----

    @Test
    void addressPostcodeIsUppercased() {
        assertThat(addressMapper.toAddress(address(" sw1a 1aa ")).getPostCode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void invalidPostcodeIsReported() {
        addressMapper.toValidAddress(address("??"), "Property/Address", errors);

        assertThat(errors.errors()).extracting(e -> e.path())
            .containsExactly("/RentalClaimSet/RentalClaim[2]/Property/Address/PostCode");
    }

    // ---- claimant ----

    private ClaimantType company(String companyName) {
        ClaimantType claimant = new ClaimantType();
        claimant.setCompanyName(companyName);
        claimant.setContactEmailAddress("claims@example.org");
        return claimant;
    }

    @Test
    void claimantNameMatchingPrdIsNotAnOverride() {
        var mapping = new BulkClaimantMapper(addressMapper).map(
            company("example housing"), null, organisation("Example Housing"), "Claimant", "Solicitor", errors);

        assertThat(mapping.claimantInformation().getIsClaimantNameCorrect()).isEqualTo(VerticalYesNo.YES);
        assertThat(mapping.contactPreferences().getIsCorrectClaimantContactAddress()).isEqualTo(VerticalYesNo.YES);
        assertThat(mapping.contactPreferences().getOverriddenClaimantContactEmail()).isEqualTo("claims@example.org");
        assertThat(mapping.contactPreferences().getClaimantContactEmail()).isEqualTo("admin@example.org");
        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void claimantNameDifferingFromPrdIsOverridden() {
        var mapping = new BulkClaimantMapper(addressMapper).map(
            company("Trading Name"), null, organisation("Example Housing"), "Claimant", "Solicitor", errors);

        assertThat(mapping.claimantInformation().getIsClaimantNameCorrect()).isEqualTo(VerticalYesNo.NO);
        assertThat(mapping.claimantInformation().getOverriddenClaimantName()).isEqualTo("Trading Name");
    }

    @Test
    void claimantNameIsFallbackWhenPrdHasNone() {
        var mapping = new BulkClaimantMapper(addressMapper).map(
            company("Trading Name"), null, organisation(null), "Claimant", "Solicitor", errors);

        assertThat(mapping.claimantInformation().getFallbackClaimantName()).isEqualTo("Trading Name");
    }

    @Test
    void individualClaimantUsesFirstAndLastName() {
        ClaimantType claimant = company(null);
        claimant.setIndividualName(name("Lee", "Landlord"));

        var mapping = new BulkClaimantMapper(addressMapper).map(
            claimant, null, organisation(null), "Claimant", "Solicitor", errors);

        assertThat(mapping.claimantInformation().getFallbackClaimantName()).isEqualTo("Lee Landlord");
    }

    @Test
    void solicitorAddressIsUsedBeforeClaimantAndOrganisationAddress() {
        ClaimantType claimant = company("Example Housing");
        claimant.setContactDetails(contact("M1 1AA"));
        SolicitorType solicitor = new SolicitorType();
        solicitor.setFirm("Firm LLP");
        solicitor.setContactDetails(contact("LS1 1AA"));
        solicitor.setPCOLReferenceNumber("99");

        var mapping = new BulkClaimantMapper(addressMapper).map(
            claimant, solicitor, solicitorOrganisation("Example Housing"), "Claimant", "Solicitor", errors);

        assertThat(mapping.contactPreferences().getOverriddenClaimantContactAddress().getPostCode())
            .isEqualTo("LS1 1AA");
        assertThat(mapping.solicitorFirmName()).isEqualTo("Firm LLP");
        assertThat(mapping.legacyPcolSolicitorReference()).isEqualTo("99");
    }

    @Test
    void solicitorBlockIsIgnoredForANonSolicitorOrganisationExceptFirmAndReference() {
        ClaimantType claimant = company("Example Housing");
        claimant.setContactDetails(contact("M1 1AA"));
        SolicitorType solicitor = new SolicitorType();
        solicitor.setFirm("Firm LLP");
        ContactDetailsType solicitorContact = contact("LS1 1AA");
        solicitorContact.setTelephoneNumber("01132223333");
        solicitor.setContactDetails(solicitorContact);
        solicitor.setPCOLReferenceNumber("99");

        var mapping = new BulkClaimantMapper(addressMapper).map(
            claimant, solicitor, organisation("Example Housing"), "Claimant", "Solicitor", errors);

        assertThat(mapping.noticeOfActingRequired()).isTrue();
        assertThat(mapping.contactPreferences().getOverriddenClaimantContactAddress().getPostCode())
            .isEqualTo("M1 1AA");
        assertThat(mapping.contactPreferences().getClaimantProvidePhoneNumber()).isEqualTo(VerticalYesNo.NO);
        assertThat(mapping.solicitorFirmName()).isEqualTo("Firm LLP");
        assertThat(mapping.legacyPcolSolicitorReference()).isEqualTo("99");
    }

    @Test
    void defendantPhoneAndEmailAreMappedToExtras() {
        DefendantType defendant = new DefendantType();
        defendant.setName(name("A", "One"));
        ContactDetailsType contact = contact("LS1 1AA");
        contact.setTelephoneNumber("0113 222 3333");
        contact.setEmailAddress("a@example.org");
        defendant.setContactDetails(contact);

        var mapping = new BulkDefendantMapper(addressMapper).map(List.of(defendant), "Defendant", errors);

        assertThat(mapping.extras().getFirst().phoneNumber()).isEqualTo("01132223333");
        assertThat(mapping.extras().getFirst().emailAddress()).isEqualTo("a@example.org");
        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void badDefendantPhoneAndEmailAreReported() {
        DefendantType defendant = new DefendantType();
        defendant.setName(name("A", "One"));
        ContactDetailsType contact = contact("LS1 1AA");
        contact.setTelephoneNumber("123");
        contact.setEmailAddress("not-an-email");
        defendant.setContactDetails(contact);

        new BulkDefendantMapper(addressMapper).map(List.of(defendant), "Defendant", errors);

        assertThat(errors.errors()).extracting(e -> e.path()).containsExactly(
            "/RentalClaimSet/RentalClaim[2]/Defendant[1]/ContactDetails/TelephoneNumber",
            "/RentalClaimSet/RentalClaim[2]/Defendant[1]/ContactDetails/EmailAddress");
    }

    @Test
    void missingContactAddressIsAnErrorWhenOrganisationHasNone() {
        OrganisationDetailsResponse noAddress = organisation("Example Housing");
        noAddress.setContactInformation(null);

        new BulkClaimantMapper(addressMapper).map(
            company("Example Housing"), null, noAddress, "Claimant", "Solicitor", errors);

        assertThat(errors.errors()).extracting(e -> e.path())
            .containsExactly("/RentalClaimSet/RentalClaim[2]/Claimant/ContactDetails");
    }

    @Test
    void badTelephoneNumberIsReported() {
        ClaimantType claimant = company("Example Housing");
        ContactDetailsType contact = contact("M1 1AA");
        contact.setTelephoneNumber("12345");
        claimant.setContactDetails(contact);

        new BulkClaimantMapper(addressMapper).map(
            claimant, null, organisation("Example Housing"), "Claimant", "Solicitor", errors);

        assertThat(errors.errors()).hasSize(1);
    }

    // ---- defendants ----

    @Test
    void defendantsMapAddressStates() {
        DefendantType sameAddress = new DefendantType();
        sameAddress.setName(name("A", "One"));
        sameAddress.setAddressSameAsProperty(true);
        DefendantType elsewhere = new DefendantType();
        elsewhere.setName(name("B", "Two"));
        elsewhere.setContactDetails(contact("LS1 1AA"));
        MortgageDefendantType unknown = new MortgageDefendantType();
        unknown.setName(name("C", "Three"));
        unknown.setIsOccupant(false);

        var mapping = new BulkDefendantMapper(addressMapper).map(
            List.of(sameAddress, elsewhere, unknown), "Defendant", errors);

        assertThat(mapping.defendant1().getAddressSameAsPossession()).isEqualTo(VerticalYesNo.YES);
        assertThat(mapping.addAnotherDefendant()).isEqualTo(VerticalYesNo.YES);
        assertThat(mapping.additionalDefendants().get(0).getValue().getAddressSameAsPossession())
            .isEqualTo(VerticalYesNo.NO);
        assertThat(mapping.additionalDefendants().get(1).getValue().getAddressKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(mapping.extras()).hasSize(3);
        assertThat(mapping.extras().get(2).isOccupant()).isFalse();
        assertThat(mapping.extras().get(0).isOccupant()).isNull();
    }

    // ---- underlessees ----

    @Test
    void underlesseeWithoutNameOrAddressIsUnknown() {
        var mapping = new BulkUnderlesseeMapper(addressMapper)
            .map(List.of(new UnderlesseeOrMortgageeType()), "UnderlesseeOrMortgagee", errors);

        assertThat(mapping.hasUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.YES);
        assertThat(mapping.underlesseeOrMortgagee1().getNameKnown()).isEqualTo(VerticalYesNo.NO);
        assertThat(mapping.underlesseeOrMortgagee1().getAddressKnown()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void noUnderlesseesMeansNone() {
        assertThat(new BulkUnderlesseeMapper(addressMapper).map(List.of(), "U", errors)
                       .hasUnderlesseeOrMortgagee()).isEqualTo(VerticalYesNo.NO);
    }

    // ---- statement of truth ----

    private StatementOfTruthType statement(SigneeStatusType status, boolean agreed) {
        StatementOfTruthType statement = new StatementOfTruthType();
        statement.setSigneeStatus(status);
        statement.setAgreed(agreed);
        statement.setSignedDate(LocalDate.of(2026, 11, 1));
        statement.setSignedName("Sam");
        statement.setPosition("Officer");
        statement.setFirmName("Firm");
        return statement;
    }

    @Test
    void statementOfTruthPopulatesTheAgreementThatMatchesTheSignee() {
        var mapper = new BulkStatementOfTruthMapper(CLOCK);

        var claimant = mapper.map(statement(SigneeStatusType.CLAIMANT, true), "S", errors);
        var legalRep = mapper.map(statement(SigneeStatusType.LEGAL_REPRESENTATIVE, true), "S", errors);

        assertThat(claimant.getCompletedBy()).isEqualTo(StatementOfTruthCompletedBy.CLAIMANT);
        assertThat(claimant.getAgreementClaimant()).hasSize(1);
        assertThat(legalRep.getAgreementClaimantLegalRep()).hasSize(1);
        assertThat(legalRep.getFirmNameLegalRep()).isEqualTo("Firm");
        assertThat(claimant.getSignedDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(legalRep.getSignedDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void statementOfTruthMustBeAgreedAndNotFutureDated() {
        StatementOfTruthType statement = statement(SigneeStatusType.CLAIMANT, false);
        statement.setSignedDate(LocalDate.of(2026, 12, 1));

        new BulkStatementOfTruthMapper(CLOCK).map(statement, "S", errors);

        assertThat(errors.errors()).extracting(e -> e.code())
            .containsExactly(BulkErrorCollector.NOT_AGREED, BulkErrorCollector.INVALID_VALUE);
    }

    // ---- property eligibility ----

    private PropertyEligibility check(EligibilityResult result) {
        EligibilityService service = Mockito.mock(EligibilityService.class);
        when(service.checkEligibility(any(), any())).thenReturn(result);
        PcsCaseService caseService = Mockito.mock(PcsCaseService.class);
        Mockito.doAnswer(invocation -> {
            invocation.<PCSCase>getArgument(0).setRegionId(7);
            return null;
        }).when(caseService).allocateRegionId(any());
        return new BulkPropertyEligibility(service, caseService)
            .check(address("m1 1aa"), LegislativeCountryType.ENGLAND);
    }

    @Test
    void eligibilityStatusesMapToFailReasons() {
        assertThat(check(EligibilityResult.builder().status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED).build())
                       .failReason()).isEqualTo(BulkFailReason.LEGISLATIVE_COUNTRY_REQUIRED);
        assertThat(check(EligibilityResult.builder().status(EligibilityStatus.NO_MATCH_FOUND).build())
                       .failReason()).isEqualTo(BulkFailReason.NO_COURT_FOR_POSTCODE);
        assertThat(check(EligibilityResult.builder().status(EligibilityStatus.NOT_ELIGIBLE).build())
                       .failReason()).isEqualTo(BulkFailReason.PROPERTY_NOT_ELIGIBLE);
        assertThat(check(EligibilityResult.builder().status(EligibilityStatus.MULTIPLE_MATCHES_FOUND).build())
                       .failReason()).isEqualTo(BulkFailReason.MULTIPLE_COURTS_FOR_POSTCODE);
    }

    @Test
    void eligibleStatusCarriesCountryCourtAndRegion() {
        PropertyEligibility eligibility = check(EligibilityResult.builder().status(EligibilityStatus.ELIGIBLE)
            .epimsId(20262).legislativeCountry(LegislativeCountry.ENGLAND).build());
        PCSCase pcsCase = PCSCase.builder().build();
        eligibility.applyTo(pcsCase);

        assertThat(eligibility.isEligible()).isTrue();
        assertThat(pcsCase.getCaseManagementLocationNumber()).isEqualTo(20262);
        assertThat(pcsCase.getRegionId()).isEqualTo(7);
        assertThat(pcsCase.getLegislativeCountry()).isEqualTo(LegislativeCountry.ENGLAND);
    }

    // ---- PBA ----

    @Test
    void pbaOverrideIsCheckedOnlyWhenSupplied() {
        BulkClaimantOrganisationResolver resolver = Mockito.mock(BulkClaimantOrganisationResolver.class);
        when(resolver.isPbaAccountValid(any(), Mockito.eq("PBA0000001"))).thenReturn(false);
        BulkPbaCheck check = new BulkPbaCheck(resolver);

        assertThat(check.validateOverride(null, null, "/p")).isEmpty();
        assertThat(check.validateOverride(null, "PBA0000001", "/p")).isPresent();
        assertThat(BulkPbaCheck.effectiveAccount("PBA1", null)).isEqualTo("PBA1");
        assertThat(BulkPbaCheck.effectiveAccount("PBA1", "PBA2")).isEqualTo("PBA2");
    }
}
