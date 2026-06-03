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
import uk.gov.hmcts.reform.pcs.document.model.claimpack.ClaimPackFormPayload;
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

        @Test
        void defendantWithNameKnownNoIsPersonsUnknownButAddressStillPopulated() {
            // Address is always known — "persons unknown" only flips the NAME, not the address.
            // Template §6.3.4 still renders address rows under the "Persons unknown" label.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            PartyEntity claimant = party("A", "Owner", VerticalYesNo.YES, address("1 High St"));
            PartyEntity unknownDef = party(null, null, VerticalYesNo.NO, address("99 Last Known Rd"));
            attach(pcsCase, claimant, PartyRole.CLAIMANT, 1);
            attach(pcsCase, unknownDef, PartyRole.DEFENDANT, 1);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendant1()).isNotNull();
            assertThat(payload.getDefendant1().isPersonsUnknown()).isTrue();
            assertThat(payload.getDefendant1().getAddress()).isNotNull();
            assertThat(payload.getDefendant1().getAddress().getAddressLine1())
                .isEqualTo("99 Last Known Rd");
        }

        @Test
        void multipleDefendantsSplitIntoFirstAndAdditional() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            PartyEntity claimant = party("A", "Owner", VerticalYesNo.YES, address("1 High St"));
            attach(pcsCase, claimant, PartyRole.CLAIMANT, 1);
            attach(pcsCase, party("Bob", "One", VerticalYesNo.YES, address("X")), PartyRole.DEFENDANT, 1);
            attach(pcsCase, party("Carol", "Two", VerticalYesNo.YES, address("Y")), PartyRole.DEFENDANT, 2);
            attach(pcsCase, party("Dave", "Three", VerticalYesNo.YES, address("Z")), PartyRole.DEFENDANT, 3);

            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendant1().getLastName()).isEqualTo("One");
            assertThat(payload.getAdditionalDefendants()).hasSize(2);
            assertThat(payload.getAdditionalDefendants().get(0).getLastName()).isEqualTo("Two");
            assertThat(payload.getAdditionalDefendants().get(1).getLastName()).isEqualTo("Three");
        }
    }

    @Nested
    class Grounds {

        @Test
        void noGroundsImpliesNoOrAbsoluteOrOtherGrounds() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimPackFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasGroundsYesNo()).isEqualTo(VerticalYesNo.NO);
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
            assertThat(payload.getHasGroundsYesNo()).isEqualTo(VerticalYesNo.YES);
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
            assertThat(payload.isMethodRequiresTime()).isTrue();
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

            assertThat(payload.isMethodRequiresTime()).isFalse();
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
            assertThat(payload.getRentAmount()).isEqualByComparingTo("100");
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

            assertThat(payload.getIsDemotionClaimYesNo()).isEqualTo(VerticalYesNo.YES);
            assertThat(payload.getDemotionReasonsFreeText()).isEqualTo("Anti-social behaviour");
            assertThat(payload.getIsSuspensionClaimYesNo()).isEqualTo(VerticalYesNo.NO);
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

            assertThat(payload.getAsbAllegedYesNo()).isEqualTo(VerticalYesNo.YES);
            assertThat(payload.getAsbDetailsFreeText()).isEqualTo("Loud noise complaints");
            assertThat(payload.getIsPcscYesNo()).isEqualTo(VerticalYesNo.YES);
            assertThat(payload.getPcscReasonFreeText()).isEqualTo("Reason for PCSC");
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

            assertThat(payload.getRentArrearsTotal()).isEqualByComparingTo("1500.00");
            assertThat(payload.getJudgmentRequestedYesNo()).isEqualTo(VerticalYesNo.YES);
            assertThat(payload.getHasPreviousStepsYesNo()).isEqualTo(VerticalYesNo.YES);
            assertThat(payload.getPreviousStepsFreeText()).isEqualTo("Two letters sent");
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
