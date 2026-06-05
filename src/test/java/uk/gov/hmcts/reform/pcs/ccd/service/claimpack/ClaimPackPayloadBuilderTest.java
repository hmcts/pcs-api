package uk.gov.hmcts.reform.pcs.ccd.service.claimpack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.AsbProhibitedConductEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.PossessionAlternativesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.StatementOfTruthEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackDefendantRow;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackUnderlesseeRow;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimPackPayloadBuilderTest {

    private ClaimPackPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ClaimPackPayloadBuilder(
            new CaseReferenceFormatter(),
            new CaseNameFormatter()
        );
    }

    @Nested
    class HeaderAndCountry {

        @Test
        void englandCaseIsWalesFalse() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimPackFormPayload payload = builder.build(pcsCase);
            assertThat(payload.isWales()).isFalse();
        }

        @Test
        void walesCaseIsWalesTrue() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimPackFormPayload payload = builder.build(pcsCase);
            assertThat(payload.isWales()).isTrue();
        }

        @Test
        void isEnglandIsComplementOfIsWales() {
            // Docmosis compact syntax can't negate, so the payload sends both.
            ClaimPackFormPayload eng = builder.build(minimalCase(LegislativeCountry.ENGLAND));
            ClaimPackFormPayload wal = builder.build(minimalCase(LegislativeCountry.WALES));

            assertThat(eng.isEngland()).isTrue();
            assertThat(eng.isWales()).isFalse();
            assertThat(wal.isEngland()).isFalse();
            assertThat(wal.isWales()).isTrue();
        }

        @Test
        void caseReferenceFormattedWithDashes() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setCaseReference(1234567812345678L);
            ClaimPackFormPayload payload = builder.build(pcsCase);
            assertThat(payload.getReferenceNumber()).isEqualTo("1234-5678-1234-5678");
        }
    }

    @Nested
    class Parties {

        @Test
        void claimantMappedWithAddress() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            PartyEntity claimant = party("Alice", "Owner", VerticalYesNo.YES, address("1 High St"));
            attach(pcsCase, claimant, PartyRole.CLAIMANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimant()).isNotNull();
            assertThat(payload.getClaimant().getFirstName()).isEqualTo("Alice");
            assertThat(payload.getClaimant().getLastName()).isEqualTo("Owner");
            assertThat(payload.getClaimant().isPersonsUnknown()).isFalse();
            assertThat(payload.getClaimant().getAddress().getAddressLine1()).isEqualTo("1 High St");
        }

    }

    @Nested
    class Defendants {

        @Test
        void singleKnownDefendantProducesOneRow() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("Alice", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase, party("Bob", "Tenant", VerticalYesNo.YES, address("42 Renters Way")),
                PartyRole.DEFENDANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(1);
            ClaimPackDefendantRow row = payload.getDefendants().getFirst();
            assertThat(row.getDefendantNumber()).isEqualTo(1);
            assertThat(row.getHeading()).isEqualTo("Defendant 1 details");
            assertThat(row.getDisplayName()).isEqualTo("Bob Tenant");
            assertThat(row.getAddressLine1()).isEqualTo("42 Renters Way");
        }

        @Test
        void multipleKnownDefendantsAllGetSequentialHeadings() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase, party("Bob", "One", VerticalYesNo.YES, address("X1")), PartyRole.DEFENDANT, 1);
            attach(pcsCase, party("Carol", "Two", VerticalYesNo.YES, address("X2")), PartyRole.DEFENDANT, 2);
            attach(pcsCase, party("Dave", "Three", VerticalYesNo.YES, address("X3")), PartyRole.DEFENDANT, 3);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(3);
            assertThat(payload.getDefendants()).extracting(ClaimPackDefendantRow::getHeading)
                .containsExactly("Defendant 1 details",
                    "Additional defendant 1 details", "Additional defendant 2 details");
            assertThat(payload.getDefendants()).extracting(ClaimPackDefendantRow::getDisplayName)
                .containsExactly("Bob One", "Carol Two", "Dave Three");
            assertThat(payload.getDefendants()).extracting(ClaimPackDefendantRow::getAddressLine1)
                .containsExactly("X1", "X2", "X3");
        }

        @Test
        void personsUnknownDefendantRendersAsPersonsUnknownWithAddressStillPresent() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            // nameKnown=NO is the "persons unknown" marker on the entity side.
            attach(pcsCase, party(null, null, VerticalYesNo.NO, address("99 Last Known Rd")),
                PartyRole.DEFENDANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(1);
            ClaimPackDefendantRow row = payload.getDefendants().getFirst();
            assertThat(row.getDisplayName()).isEqualTo("Persons unknown");
            // Address still rendered — spec: defendant address cannot be "Unknown".
            assertThat(row.getAddressLine1()).isEqualTo("99 Last Known Rd");
        }

        @Test
        void mixedKnownAndUnknownDefendantsRenderTogether() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase, party(null, null, VerticalYesNo.NO, address("5 Maes Y Coed Rd")),
                PartyRole.DEFENDANT, 1);
            attach(pcsCase, party("Carwyn", "Jones", VerticalYesNo.YES, address("5 Maes Y Coed Rd")),
                PartyRole.DEFENDANT, 2);
            attach(pcsCase, party(null, null, VerticalYesNo.NO, address("5 Maes Y Coed Rd")),
                PartyRole.DEFENDANT, 3);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(3);
            assertThat(payload.getDefendants()).extracting(ClaimPackDefendantRow::getDisplayName)
                .containsExactly("Persons unknown", "Carwyn Jones", "Persons unknown");
            assertThat(payload.getDefendants()).extracting(ClaimPackDefendantRow::getHeading)
                .containsExactly("Defendant 1 details",
                    "Additional defendant 1 details", "Additional defendant 2 details");
        }

        @Test
        void defendantWithNoAddressFallsBackToPropertyAddress() {
            // Spec: "If the claimant has not provided an address for the defendant then
            // the address of the property to be possessed becomes the defendant's address."
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setPropertyAddress(address("99 Property Lane"));
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            // Defendant with no own address — should pick up property address instead.
            attach(pcsCase, party("Bob", "Tenant", VerticalYesNo.YES, null),
                PartyRole.DEFENDANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            ClaimPackDefendantRow row = payload.getDefendants().getFirst();
            assertThat(row.getDisplayName()).isEqualTo("Bob Tenant");
            assertThat(row.getAddressLine1()).isEqualTo("99 Property Lane");
        }

        @Test
        void defendantHeadingsFollowAc06Sequence() {
            // AC06: After "Defendant 1 details" the next sections must be
            // "Additional defendant 1 details", "Additional defendant 2 details", … sequentially.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase, party("Bob", "One", VerticalYesNo.YES, address("X1")), PartyRole.DEFENDANT, 1);
            attach(pcsCase, party("Carol", "Two", VerticalYesNo.YES, address("X2")), PartyRole.DEFENDANT, 2);
            attach(pcsCase, party("Dave", "Three", VerticalYesNo.YES, address("X3")), PartyRole.DEFENDANT, 3);
            attach(pcsCase, party("Eve", "Four", VerticalYesNo.YES, address("X4")), PartyRole.DEFENDANT, 4);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).extracting(ClaimPackDefendantRow::getHeading)
                .containsExactly(
                    "Defendant 1 details",
                    "Additional defendant 1 details",
                    "Additional defendant 2 details",
                    "Additional defendant 3 details");
        }
    }

    @Nested
    class ClaimantDerivedFields {

        @Test
        void displayNameUsesOrgNameWhenSet() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            attach(pcsCase, orgParty("Possession Claims Solicitor Org", address("MOJ")), PartyRole.CLAIMANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantDisplayName()).isEqualTo("Possession Claims Solicitor Org");
        }

        @Test
        void displayNameUsesFirstAndLastNameWhenNoOrg() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("Alice", "Owner", VerticalYesNo.YES, address("X")), PartyRole.CLAIMANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantDisplayName()).isEqualTo("Alice Owner");
        }

        @Test
        void displayNameFallsBackToPersonsUnknown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party(null, null, VerticalYesNo.NO, address("X")), PartyRole.CLAIMANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantDisplayName()).isEqualTo("Persons unknown");
        }

        @Test
        void addressLinePresenceFlagsReflectNullability() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            AddressEntity addr = AddressEntity.builder()
                .addressLine1("Ministry Of Justice")
                .addressLine2("Seventh Floor 102 Petty France")
                .addressLine3(null)
                .postTown("London")
                .county(null)
                .postcode("SW1H 9AJ")
                .build();
            attach(pcsCase, orgParty("Org", addr), PartyRole.CLAIMANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasClaimantAddressLine2()).isTrue();
            assertThat(payload.isHasClaimantAddressLine3()).isFalse();
            assertThat(payload.isHasClaimantCounty()).isFalse();
        }

        @Test
        void exemptLandlordRenderedAsTitleCaseString() {
            // VerticalYesNo.NO.getLabel() == "No" — payload exposes the title-case string
            // so Docmosis renders "No" directly without further mapping.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            pcsCase.getClaims().getFirst().setIsExemptLandlord(VerticalYesNo.NO);
            attach(pcsCase, orgParty("Org", address("X")), PartyRole.CLAIMANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantIsExemptLandlord()).isEqualTo("No");
        }

        @Test
        void exemptLandlordNullWhenSourceNull() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, orgParty("Org", address("X")), PartyRole.CLAIMANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantIsExemptLandlord()).isNull();
        }
    }

    @Nested
    class Grounds {

        @Test
        void noGroundsImpliesNoOrAbsoluteOrOtherGrounds() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasGroundsYesNo()).isEqualTo("No");
            assertThat(payload.getGrounds()).isEmpty();
            assertThat(payload.isNoOrAbsoluteOrOtherGrounds()).isTrue();
            assertThat(payload.isHasRentArrearsGround()).isFalse();
            assertThat(payload.isHasAsbGround()).isFalse();
        }

        @Test
        void rentArrearsGroundDetected() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            ClaimGroundEntity g = ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.ASSURED_MANDATORY)
                .code("8")
                .isRentArrears(true)
                .claim(claim)
                .build();
            claim.getClaimGrounds().add(g);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasRentArrearsGround()).isTrue();
            assertThat(payload.getGrounds()).hasSize(1);
            assertThat(payload.getHasGroundsYesNo()).isEqualTo("Yes");
        }
    }

    @Nested
    class ClaimDetailsShowFlags {

        @Test
        void englandIntroDemotedOtherWithOtherGround_descriptionAndWhyClaimingShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                .code("OTHER")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDescriptionOfGrounds()).isTrue();
            assertThat(payload.isShowWhyClaimingPossession()).isTrue();
        }

        @Test
        void englandIntroDemotedOtherWithRegularGround_descriptionAndWhyClaimingHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.ASSURED_MANDATORY)
                .code("8")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDescriptionOfGrounds()).isFalse();
            assertThat(payload.isShowWhyClaimingPossession()).isFalse();
        }

        @Test
        void englandIntroDemotedOtherWithAbsoluteGround_whyClaimingShownDescriptionHidden() {
            // D13 (Cook [17]): absolute grounds triggers "Why claiming possession?". Absolute grounds
            // are identified by the code ABSOLUTE_GROUNDS, not by the category.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                .code("ABSOLUTE_GROUNDS")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowWhyClaimingPossession()).isTrue();
            assertThat(payload.isShowDescriptionOfGrounds()).isFalse();
            assertThat(payload.isHasOtherGround()).isFalse();
            assertThat(payload.getHasGroundsYesNo()).isEqualTo("Yes");
        }

        @Test
        void englandIntroDemotedOtherCategoryNonOtherCode_otherFlagsStayFalse() {
            // Regression: category INTRODUCTORY_DEMOTED_OTHER contains the substring "OTHER", but a
            // non-OTHER code (RENT_ARREARS) must NOT count as the "Other" ground.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                .code("RENT_ARREARS")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasOtherGround()).isFalse();
            assertThat(payload.isShowDescriptionOfGrounds()).isFalse();
            assertThat(payload.isShowWhyClaimingPossession()).isFalse();
        }

        @Test
        void groundsWithReasonsListIncludesOnlyGroundsWithReasonText() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.ASSURED_MANDATORY)
                .code("8")
                .claim(claim)
                .build());
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.ASSURED_DISCRETIONARY)
                .code("10")
                .reason("Defendant in continuous arrears for 18 months.")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).hasSize(2);
            assertThat(payload.getGroundsWithReasons()).hasSize(1);
            assertThat(payload.getGroundsWithReasons().getFirst().getReasonFreeText())
                .isEqualTo("Defendant in continuous arrears for 18 months.");
        }

        @Test
        void additionalReasonsNotProvided_freeTextRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setAdditionalReasonsProvided(VerticalYesNo.NO);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasAdditionalReasonsYesNo()).isEqualTo("No");
            assertThat(payload.isAdditionalReasonsProvided()).isFalse();
        }

        @Test
        void additionalReasonsProvided_freeTextRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setAdditionalReasonsProvided(VerticalYesNo.YES);
            claim.setAdditionalReasons("Defendant has refused mediation.");

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasAdditionalReasonsYesNo()).isEqualTo("Yes");
            assertThat(payload.isAdditionalReasonsProvided()).isTrue();
            assertThat(payload.getAdditionalReasonsFreeText()).isEqualTo("Defendant has refused mediation.");
        }

        @Test
        void walesJourneyEvenWithOtherGround_descriptionAndWhyClaimingStillHidden() {
            // Spec: "N/A for Welsh journey" — both show-flags must stay false regardless.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                .code("OTHER")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDescriptionOfGrounds()).isFalse();
            assertThat(payload.isShowWhyClaimingPossession()).isFalse();
        }
    }

    @Nested
    class GapFieldsRemainNull {

        @Test
        void issueDateAndWhyClaimingAndNoticeNotUploadedAndWalesDocsAllNull() {
            // §13.3 — these fields have no entity source yet; payload must leave them null
            // (not throw) so the template renders empty rows for them.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIssueDateSealed()).isNull();
            assertThat(payload.getWhyClaimingPossession()).isNull();
            assertThat(payload.getNoticeNotUploadedReason()).isNull();
            assertThat(payload.getEpcUploadedYesNo()).isNull();
            assertThat(payload.getEpcNotUploadedReason()).isNull();
            assertThat(payload.getGasSafetyUploadedYesNo()).isNull();
            assertThat(payload.getGasSafetyNotUploadedReason()).isNull();
            assertThat(payload.getEicrUploadedYesNo()).isNull();
            assertThat(payload.getEicrNotUploadedReason()).isNull();
        }
    }

    @Nested
    class OptionalSubEntities {

        @Test
        void missingNoticeAsbRentTenancySotDoesNotThrow() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            // claim has all sub-entities null — builder must tolerate this without NPE.
            ClaimPackFormPayload payload = builder.build(pcsCase);
            assertThat(payload).isNotNull();
            assertThat(payload.getNoticeServedYesNo()).isNull();
            assertThat(payload.getAsbAllegedYesNo()).isNull();
            assertThat(payload.getRentArrearsTotal()).isNull();
            assertThat(payload.getTenancyTypeLabel()).isNull();
            assertThat(payload.getSotFullName()).isNull();
        }
    }

    @Nested
    class NoticeMethodRouting {

        @Test
        void emailMethodRoutesDetailsToEmailField() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .servingMethod(NoticeServiceMethod.EMAIL)
                .noticeDetails("served@example.com")
                .noticeDate(LocalDate.of(2026, 5, 1))
                .noticeDateTime(LocalDateTime.of(2026, 5, 1, 10, 30))
                .claim(claim)
                .build();
            claim.setNoticeOfPossession(notice);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getMethodOfService()).isEqualTo(NoticeServiceMethod.EMAIL);
            assertThat(payload.getNoticeServedToEmail()).isEqualTo("served@example.com");
            assertThat(payload.getNoticeLeftWithName()).isNull();
            assertThat(payload.getNoticeOtherElectronicDetails()).isNull();
        }

        @Test
        void firstClassPostMethodHasNoDetailRouting() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            NoticeOfPossessionEntity notice = NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .servingMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticeDetails("ignored")
                .claim(claim)
                .build();
            claim.setNoticeOfPossession(notice);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getNoticeLeftWithName()).isNull();
            assertThat(payload.getNoticeServedToEmail()).isNull();
            assertThat(payload.getNoticeOtherElectronicDetails()).isNull();
            assertThat(payload.getNoticeOtherMeansDetails()).isNull();
        }
    }

    @Nested
    class Tenancy {

        @Test
        void introductoryTenancyMarkedAsIntroDemotedOther() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            TenancyLicenceEntity t = TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .startDate(LocalDate.of(2024, 1, 1))
                .rentAmount(new BigDecimal("100"))
                .build();
            pcsCase.setTenancyLicence(t);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isIntroDemotedOtherTenancy()).isTrue();
            assertThat(payload.getTenancyStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(payload.getRentAmount()).isEqualTo("£100.00");
        }

        @Test
        void assuredTenancyIsNotIntroDemotedOther() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            TenancyLicenceEntity t = TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .build();
            pcsCase.setTenancyLicence(t);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isIntroDemotedOtherTenancy()).isFalse();
        }

        @Test
        void tenancyUploadedComplementaryBooleansDriveDocmosisBranches() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            TenancyLicenceEntity t = TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .hasCopyOfTenancyLicence(VerticalYesNo.YES)
                .build();
            pcsCase.setTenancyLicence(t);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isTenancyUploadedYes()).isTrue();
            assertThat(payload.isTenancyUploadedNo()).isFalse();
        }

        @Test
        void tenancyUploadedNoFlipsTheOtherDirection() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            TenancyLicenceEntity t = TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .hasCopyOfTenancyLicence(VerticalYesNo.NO)
                .build();
            pcsCase.setTenancyLicence(t);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isTenancyUploadedYes()).isFalse();
            assertThat(payload.isTenancyUploadedNo()).isTrue();
        }
    }

    @Nested
    class StatementOfTruth {

        @Test
        void claimantSigningIsNotLegalRep() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            StatementOfTruthEntity sot = StatementOfTruthEntity.builder()
                .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                .fullName("Alice Owner")
                .build();
            claim.setStatementOfTruth(sot);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isSignedByLegalRep()).isFalse();
            assertThat(payload.getSotFullName()).isEqualTo("Alice Owner");
        }

        @Test
        void legalRepSigningTrueAndFirmNamePopulated() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            StatementOfTruthEntity sot = StatementOfTruthEntity.builder()
                .completedBy(StatementOfTruthCompletedBy.LEGAL_REPRESENTATIVE)
                .fullName("Bob Solicitor")
                .firmName("Solicitors Ltd")
                .positionHeld("Partner")
                .build();
            claim.setStatementOfTruth(sot);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isSignedByLegalRep()).isTrue();
            assertThat(payload.getSotFirmName()).isEqualTo("Solicitors Ltd");
            assertThat(payload.getSotPositionHeld()).isEqualTo("Partner");
        }
    }

    @Nested
    class PossessionAlternatives {

        @Test
        void demotionAndSuspensionMappedFromPossessionAlternatives() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            PossessionAlternativesEntity alt = PossessionAlternativesEntity.builder()
                .dotRequested(YesOrNo.YES)
                .dotReason("Anti-social behaviour")
                .suspensionOfRTB(YesOrNo.NO)
                .build();
            claim.setPossessionAlternativesEntity(alt);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIsDemotionClaimYesNo()).isEqualTo("Yes");
            assertThat(payload.getDemotionReasonsFreeText()).isEqualTo("Anti-social behaviour");
            assertThat(payload.getIsSuspensionClaimYesNo()).isEqualTo("No");
        }

        @Test
        void demotionNotAnswered_wholeSectionHidden() {
            // Optional Y/N: when user didn't answer, the whole demotion section disappears.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            // PossessionAlternativesEntity not set at all.

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowIsDemotionClaim()).isFalse();
            assertThat(payload.isShowDemotionDetails()).isFalse();
            assertThat(payload.getIsDemotionClaimYesNo()).isNull();
        }

        @Test
        void demotionYes_sectionVisibleWithFollowUps() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setPossessionAlternativesEntity(PossessionAlternativesEntity.builder()
                .dotRequested(YesOrNo.YES)
                .dotStatementServed(YesOrNo.YES)
                .dotStatementDetails("Notice given on 1 March.")
                .dotReason("Breach of conditions.")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowIsDemotionClaim()).isTrue();
            assertThat(payload.isShowDemotionDetails()).isTrue();
            assertThat(payload.isShowDemotionTermsFreeText()).isTrue();
            assertThat(payload.getHasServedDemotionTermsYesNo()).isEqualTo("Yes");
        }

        @Test
        void demotionNo_sectionVisibleButFollowUpsHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setPossessionAlternativesEntity(PossessionAlternativesEntity.builder()
                .dotRequested(YesOrNo.NO)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowIsDemotionClaim()).isTrue();   // Y/N row visible
            assertThat(payload.isShowDemotionDetails()).isFalse();  // 4 follow-ups hidden
            assertThat(payload.getIsDemotionClaimYesNo()).isEqualTo("No");
        }

        @Test
        void demotionExpressTermsNo_termsDetailsHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setPossessionAlternativesEntity(PossessionAlternativesEntity.builder()
                .dotRequested(YesOrNo.YES)
                .dotStatementServed(YesOrNo.NO)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDemotionDetails()).isTrue();
            assertThat(payload.isShowDemotionTermsFreeText()).isFalse();
        }

        @Test
        void suspensionYes_sectionVisibleWithFollowUps() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setPossessionAlternativesEntity(PossessionAlternativesEntity.builder()
                .suspensionOfRTB(YesOrNo.YES)
                .suspensionOfRTBReason("Reasons given.")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowIsSuspensionClaim()).isTrue();
            assertThat(payload.isShowSuspensionDetails()).isTrue();
        }
    }

    @Nested
    class Circumstances {

        @Test
        void claimantCircsYes_detailsRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setClaimantCircumstancesProvided(VerticalYesNo.YES);
            claim.setClaimantCircumstances("Two children in the household.");

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasClaimantCircsYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowClaimantCircsFreeText()).isTrue();
            assertThat(payload.getClaimantCircsFreeText()).isEqualTo("Two children in the household.");
        }

        @Test
        void claimantCircsNo_detailsRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setClaimantCircumstancesProvided(VerticalYesNo.NO);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasClaimantCircsYesNo()).isEqualTo("No");
            assertThat(payload.isShowClaimantCircsFreeText()).isFalse();
        }

        @Test
        void defendantCircsYes_detailsRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setDefendantCircumstancesProvided(VerticalYesNo.YES);
            claim.setDefendantCircumstances("Defendant has been unemployed.");

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasDefendantCircsYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowDefendantCircsFreeText()).isTrue();
        }

        @Test
        void defendantCircsNo_detailsRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setDefendantCircumstancesProvided(VerticalYesNo.NO);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasDefendantCircsYesNo()).isEqualTo("No");
            assertThat(payload.isShowDefendantCircsFreeText()).isFalse();
        }
    }

    @Nested
    class TenancyStartDate {

        @Test
        void startDatePopulated_rowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .startDate(LocalDate.of(2024, 1, 1))
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowTenancyStartDate()).isTrue();
            assertThat(payload.getTenancyStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        }

        @Test
        void startDateNull_rowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowTenancyStartDate()).isFalse();
        }
    }

    @Nested
    class AsbAndPcsc {

        @Test
        void asbAllegedAndPcscMappedFromSharedEntity() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            AsbProhibitedConductEntity asb = AsbProhibitedConductEntity.builder()
                .antisocialBehaviour(VerticalYesNo.YES)
                .antisocialBehaviourDetails("Loud noise complaints")
                .claimingStandardContract(VerticalYesNo.YES)
                .claimingStandardContractDetails("Reason for PCSC")
                .build();
            claim.setAsbProhibitedConductEntity(asb);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getAsbAllegedYesNo()).isEqualTo("Yes");
            assertThat(payload.getAsbDetailsFreeText()).isEqualTo("Loud noise complaints");
            assertThat(payload.getIsPcscYesNo()).isEqualTo("Yes");
            assertThat(payload.getPcscReasonFreeText()).isEqualTo("Reason for PCSC");
        }
    }

    @Nested
    class PcscSection {

        @Test
        void walesShowsPcscSection() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowPcscSection()).isTrue();
        }

        @Test
        void englandHidesPcscSection() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowPcscSection()).isFalse();
        }

        @Test
        void pcscYes_detailsShownAndTermsShownIfAgreedYes() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setAsbProhibitedConductEntity(AsbProhibitedConductEntity.builder()
                .claimingStandardContract(VerticalYesNo.YES)
                .periodicContractAgreed(VerticalYesNo.YES)
                .periodicContractDetails("Standard 12-month PCSC terms.")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIsPcscYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowPcscDetails()).isTrue();
            assertThat(payload.getPcscTermsAgreedYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowPcscTermsFreeText()).isTrue();
        }

        @Test
        void pcscNo_detailsAndTermsHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setAsbProhibitedConductEntity(AsbProhibitedConductEntity.builder()
                .claimingStandardContract(VerticalYesNo.NO)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIsPcscYesNo()).isEqualTo("No");
            assertThat(payload.isShowPcscDetails()).isFalse();
            assertThat(payload.isShowPcscTermsFreeText()).isFalse();
        }

        @Test
        void pcscYesButTermsNo_termsFreeTextHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setAsbProhibitedConductEntity(AsbProhibitedConductEntity.builder()
                .claimingStandardContract(VerticalYesNo.YES)
                .periodicContractAgreed(VerticalYesNo.NO)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowPcscDetails()).isTrue();
            assertThat(payload.isShowPcscTermsFreeText()).isFalse();
        }
    }

    @Nested
    class RequiredDocumentsSection {

        @Test
        void walesShowsRequiredDocumentsSection() {
            ClaimPackFormPayload payload = builder.build(minimalCase(LegislativeCountry.WALES));
            assertThat(payload.isShowRequiredDocumentsSection()).isTrue();
        }

        @Test
        void englandHidesRequiredDocumentsSection() {
            ClaimPackFormPayload payload = builder.build(minimalCase(LegislativeCountry.ENGLAND));
            assertThat(payload.isShowRequiredDocumentsSection()).isFalse();
        }
        // EPC/gas/EICR field data still §13.3 gap; show*NotUploadedReason booleans default to
        // false (since the upload Y/N fields are null until source is wired).
    }

    @Nested
    class AsbSection {

        @Test
        void englandJourneyHidesAsbSection() {
            // Even if grounds include an English ASB ground, the section stays hidden
            // — spec marks the whole block "WALES ONLY".
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_ANTISOCIAL)
                .code("S84A_CONDITION_1")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowAsbSection()).isFalse();
        }

        @Test
        void walesWithoutAsbGroundHidesAsbSection() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                .code("OTHER_BREACH_OF_CONTRACT_S157")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowAsbSection()).isFalse();
        }

        @Test
        void walesWithSecureContractAsbGroundShowsSection() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            // Wales secure contract ASB ground — persisted with code "ANTISOCIAL_BEHAVIOUR_S157"
            // by WalesSecureClaimGroundService.
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                .code("ANTISOCIAL_BEHAVIOUR_S157")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowAsbSection()).isTrue();
        }

        @Test
        void walesWithStandardContractAsbGroundAlsoShowsSection() {
            // Same code, different category — both standard and secure contract Wales
            // services persist ANTISOCIAL_BEHAVIOUR_S157 as the ASB ground code.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                .code("ANTISOCIAL_BEHAVIOUR_S157")
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowAsbSection()).isTrue();
        }

        @Test
        void detailsRowsGatedIndependentlyByEachYesNoAnswer() {
            // showAsbDetails follows asbAlleged; showIllegalUseDetails follows illegalPurposes;
            // showOtherProhibitedDetails follows otherProhibitedConduct — independently.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            AsbProhibitedConductEntity asb = AsbProhibitedConductEntity.builder()
                .antisocialBehaviour(VerticalYesNo.YES)
                .antisocialBehaviourDetails("Loud noise.")
                .illegalPurposes(VerticalYesNo.NO)
                .otherProhibitedConduct(VerticalYesNo.YES)
                .otherProhibitedConductDetails("Damage to property.")
                .build();
            claim.setAsbProhibitedConductEntity(asb);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getAsbAllegedYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowAsbDetails()).isTrue();
            assertThat(payload.getIllegalUseAllegedYesNo()).isEqualTo("No");
            assertThat(payload.isShowIllegalUseDetails()).isFalse();
            assertThat(payload.getOtherProhibitedAllegedYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowOtherProhibitedDetails()).isTrue();
        }

        @Test
        void asbAllegedRowGatedOnAnswerPresence() {
            // D16 Yes/No row hides when unanswered (parity with D18/D20 null-gates).
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setAsbProhibitedConductEntity(AsbProhibitedConductEntity.builder()
                .antisocialBehaviour(VerticalYesNo.NO)
                .build());

            assertThat(builder.build(pcsCase).isShowAsbAlleged()).isTrue();

            claim.setAsbProhibitedConductEntity(AsbProhibitedConductEntity.builder().build());

            assertThat(builder.build(pcsCase).isShowAsbAlleged()).isFalse();
        }
    }

    @Nested
    class RentArrears {

        @Test
        void rentArrearsFieldsMapped() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            RentArrearsEntity rent = RentArrearsEntity.builder()
                .totalRentArrears(new BigDecimal("1500.00"))
                .arrearsJudgmentWanted(VerticalYesNo.YES)
                .recoveryAttempted(VerticalYesNo.YES)
                .recoveryAttemptDetails("Two letters sent")
                .build();
            claim.setRentArrears(rent);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getRentArrearsTotal()).isEqualTo("£1,500.00");
            assertThat(payload.getJudgmentRequestedYesNo()).isEqualTo("Yes");
            assertThat(payload.getHasPreviousStepsYesNo()).isEqualTo("Yes");
            assertThat(payload.getPreviousStepsFreeText()).isEqualTo("Two letters sent");
        }

        @Test
        void rentArrearsSectionGatedByHasRentArrearsGround() {
            // England with a rent-arrears ground → hasRentArrearsGround = true.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.ASSURED_MANDATORY)
                .code("8")
                .isRentArrears(true)
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasRentArrearsGround()).isTrue();
        }

        @Test
        void rentArrearsSectionHiddenWhenNoRentArrearsGround() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_ANTISOCIAL)
                .code("S84A_CONDITION_1")
                .isRentArrears(false)
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasRentArrearsGround()).isFalse();
        }

        @Test
        void walesRentArrearsS157AlsoSetsHasRentArrearsGround() {
            // Wales: the WalesSecure/Standard services persist isRentArrears=true when the
            // user selected RENT_ARREARS_S157.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                .code("RENT_ARREARS_S157")
                .isRentArrears(true)
                .claim(claim)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasRentArrearsGround()).isTrue();
        }

        @Test
        void previousStepsFreeTextShownWhenPreviousStepsYes() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setRentArrears(RentArrearsEntity.builder()
                .recoveryAttempted(VerticalYesNo.YES)
                .recoveryAttemptDetails("Three letters sent.")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasPreviousStepsYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowPreviousStepsFreeText()).isTrue();
            assertThat(payload.getPreviousStepsFreeText()).isEqualTo("Three letters sent.");
        }

        @Test
        void previousStepsFreeTextHiddenWhenPreviousStepsNo() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setRentArrears(RentArrearsEntity.builder()
                .recoveryAttempted(VerticalYesNo.NO)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasPreviousStepsYesNo()).isEqualTo("No");
            assertThat(payload.isShowPreviousStepsFreeText()).isFalse();
        }
    }

    @Nested
    class ActionAlreadyTaken {

        @Test
        void preActionProtocolFollowedYes_reasonRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setPreActionProtocolFollowed(VerticalYesNo.YES);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getPreActionProtocolFollowedYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowPreActionProtocolNotFollowedReason()).isFalse();
        }

        @Test
        void preActionProtocolNotFollowed_reasonRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setPreActionProtocolFollowed(VerticalYesNo.NO);
            claim.setPreActionProtocolIncompleteExplanation("Defendant in hospital — couldn't deliver.");

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getPreActionProtocolFollowedYesNo()).isEqualTo("No");
            assertThat(payload.isShowPreActionProtocolNotFollowedReason()).isTrue();
            assertThat(payload.getPreActionProtocolNotFollowedReason())
                .isEqualTo("Defendant in hospital — couldn't deliver.");
        }

        @Test
        void mediationAndSettlementYNRenderedAsTitleCaseString() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setMediationAttempted(VerticalYesNo.YES);
            claim.setSettlementAttempted(VerticalYesNo.NO);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getMediationAttemptedYesNo()).isEqualTo("Yes");
            assertThat(payload.getSettlementAttemptedYesNo()).isEqualTo("No");
        }
    }

    @Nested
    class NoticeServed {

        @Test
        void noticeServedYes_whyNotServedRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getNoticeServedYesNo()).isEqualTo("Yes");
            assertThat(payload.isNoticeNotServedDisplayed()).isFalse();
        }

        @Test
        void noticeServedNo_whyNotServedRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.NO)
                .noticeStatement("Defendant address unknown — no notice possible.")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getNoticeServedYesNo()).isEqualTo("No");
            assertThat(payload.isNoticeNotServedDisplayed()).isTrue();
            assertThat(payload.getNoticeNotServedReason())
                .isEqualTo("Defendant address unknown — no notice possible.");
        }

        @Test
        void walesAndNoticeServedYes_showNoticeTypeTrue() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .noticeType("Section 173")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeType()).isTrue();
            assertThat(payload.getNoticeType()).isEqualTo("Section 173");
        }

        @Test
        void walesAndNoticeServedNo_showNoticeTypeFalse() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.NO)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeType()).isFalse();
        }

        @Test
        void englandJourneyHidesNoticeTypeEvenWhenServed() {
            // Spec: Notice type is Wales-only — England never renders this row.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .noticeType("Section 8")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeType()).isFalse();
        }

        @Test
        void noticeServedYesSetsPositiveBooleanForOuterSubTableGate() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .noticeDate(LocalDate.of(2026, 3, 1))
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isNoticeServedYes()).isTrue();
            assertThat(payload.isShowNoticeServedOn()).isTrue();
        }

        @Test
        void emailMethodSetsOnlyEmailShowFlag() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .servingMethod(NoticeServiceMethod.EMAIL)
                .noticeDetails("served@example.com")
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeServedToEmail()).isTrue();
            assertThat(payload.isShowNoticeLeftWithName()).isFalse();
            assertThat(payload.isShowNoticeOtherElectronicDetails()).isFalse();
            assertThat(payload.isShowNoticeOtherMeansDetails()).isFalse();
        }

        @Test
        void firstClassPostMethodSetsNoDetailShowFlags() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .servingMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .build());

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeLeftWithName()).isFalse();
            assertThat(payload.isShowNoticeServedToEmail()).isFalse();
            assertThat(payload.isShowNoticeOtherElectronicDetails()).isFalse();
            assertThat(payload.isShowNoticeOtherMeansDetails()).isFalse();
        }
    }

    @Nested
    class Underlessees {

        @Test
        void emptyListWhenNoUnderlessees() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            // No underlessees attached.

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getUnderlessees()).isEmpty();
        }

        @Test
        void underlesseeWithAddressKnownRendersAddressBlock() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase,
                party("Building Society", "Ltd", VerticalYesNo.YES, address("100 King Street")),
                PartyRole.UNDERLESSEE_OR_MORTGAGEE, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getUnderlessees()).hasSize(1);
            ClaimPackUnderlesseeRow row = payload.getUnderlessees().getFirst();
            assertThat(row.isAddressKnown()).isTrue();
            assertThat(row.isAddressUnknown()).isFalse();
            assertThat(row.getAddressLine1()).isEqualTo("100 King Street");
            assertThat(row.getDisplayName()).isEqualTo("Building Society Ltd");
        }

        @Test
        void underlesseeWithNoAddressRendersAddressUnknown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            // Underlessee with null address — should fall back to "Address unknown".
            attach(pcsCase, party("Building", "Society", VerticalYesNo.YES, null),
                PartyRole.UNDERLESSEE_OR_MORTGAGEE, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            ClaimPackUnderlesseeRow row = payload.getUnderlessees().getFirst();
            assertThat(row.isAddressKnown()).isFalse();
            assertThat(row.isAddressUnknown()).isTrue();
            assertThat(row.getAddressLine1()).isNull();
        }

        @Test
        void underlesseeWithNameUnknownRendersPersonsUnknown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase, party(null, null, VerticalYesNo.NO, address("Some address")),
                PartyRole.UNDERLESSEE_OR_MORTGAGEE, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            ClaimPackUnderlesseeRow row = payload.getUnderlessees().getFirst();
            assertThat(row.getDisplayName()).isEqualTo("Persons unknown");
            assertThat(row.isAddressKnown()).isTrue();
        }

        @Test
        void multipleUnderlesseesSequentialNumbering() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase, party("First", "Ltd", VerticalYesNo.YES, address("Addr A")),
                PartyRole.UNDERLESSEE_OR_MORTGAGEE, 1);
            attach(pcsCase, party("Second", "Ltd", VerticalYesNo.YES, address("Addr B")),
                PartyRole.UNDERLESSEE_OR_MORTGAGEE, 2);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getUnderlessees()).hasSize(2);
            assertThat(payload.getUnderlessees()).extracting(ClaimPackUnderlesseeRow::getHeading)
                .containsExactly(
                    "Underlessee or mortgagee 1 details",
                    "Additional underlessee or mortgagee 1 details");
        }
    }

    // =====================================================================
    // Fixture helpers
    // =====================================================================

    private PcsCaseEntity minimalCase(LegislativeCountry country) {
        PcsCaseEntity pcs = new PcsCaseEntity();
        pcs.setCaseReference(1234567812345678L);
        pcs.setLegislativeCountry(country);
        pcs.setPropertyAddress(address("1 Property Lane"));
        pcs.setParties(new HashSet<>());

        ClaimEntity claim = ClaimEntity.builder()
            .pcsCase(pcs)
            .claimGrounds(new HashSet<>())
            .claimParties(new ArrayList<>())
            .build();
        pcs.setClaims(new ArrayList<>(List.of(claim)));
        return pcs;
    }

    private PartyEntity party(String first, String last, VerticalYesNo nameKnown, AddressEntity addr) {
        return PartyEntity.builder()
            .firstName(first)
            .lastName(last)
            .nameKnown(nameKnown)
            .address(addr)
            .claimParties(new HashSet<>())
            .build();
    }

    private PartyEntity orgParty(String orgName, AddressEntity addr) {
        return PartyEntity.builder()
            .orgName(orgName)
            .nameKnown(VerticalYesNo.YES)
            .address(addr)
            .claimParties(new HashSet<>())
            .build();
    }

    private AddressEntity address(String line1) {
        return AddressEntity.builder()
            .addressLine1(line1)
            .postcode("AA1 1AA")
            .build();
    }

    private void attach(PcsCaseEntity pcsCase, PartyEntity party, PartyRole role, int rank) {
        party.setPcsCase(pcsCase);
        pcsCase.getParties().add(party);

        ClaimEntity claim = pcsCase.getClaims().getFirst();
        ClaimPartyEntity cp = ClaimPartyEntity.builder()
            .claim(claim)
            .party(party)
            .role(role)
            .rank(rank)
            .build();
        claim.getClaimParties().add(cp);
    }
}
