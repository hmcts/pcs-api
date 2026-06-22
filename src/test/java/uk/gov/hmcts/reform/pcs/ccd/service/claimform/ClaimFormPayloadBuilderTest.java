package uk.gov.hmcts.reform.pcs.ccd.service.claimform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CombinedLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
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
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormDefendantRow;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormGround;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormPayload;
import uk.gov.hmcts.reform.pcs.document.model.claimform.ClaimFormUnderlesseeRow;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimFormPayloadBuilderTest {

    // Fixed UK clock: generation happens on 16 July 2026 (BST).
    private static final Clock UK_CLOCK =
        Clock.fixed(Instant.parse("2026-07-16T08:00:00Z"), ZoneId.of("Europe/London"));

    private ClaimFormPayloadBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ClaimFormPayloadBuilder(
            new CaseReferenceFormatter(),
            new ClaimFormPartyMapper(new CaseNameFormatter()),
            UK_CLOCK
        );
    }

    @Nested
    class HeaderAndCountry {

        @Test
        void englandCaseIsWalesFalse() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimFormPayload payload = builder.build(pcsCase);
            assertThat(payload.isWales()).isFalse();
        }

        @Test
        void walesCaseIsWalesTrue() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimFormPayload payload = builder.build(pcsCase);
            assertThat(payload.isWales()).isTrue();
        }

        @Test
        void isEnglandIsComplementOfIsWales() {
            // Docmosis compact syntax can't negate, so the payload sends both.
            ClaimFormPayload eng = builder.build(minimalCase(LegislativeCountry.ENGLAND));
            ClaimFormPayload wal = builder.build(minimalCase(LegislativeCountry.WALES));

            assertThat(eng.isEngland()).isTrue();
            assertThat(eng.isWales()).isFalse();
            assertThat(wal.isEngland()).isFalse();
            assertThat(wal.isWales()).isTrue();
        }

        @Test
        void caseReferenceFormattedWithDashes() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setCaseReference(1234567812345678L);
            ClaimFormPayload payload = builder.build(pcsCase);
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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(1);
            ClaimFormDefendantRow row = payload.getDefendants().getFirst();
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(3);
            assertThat(payload.getDefendants()).extracting(ClaimFormDefendantRow::getHeading)
                .containsExactly("Defendant 1 details",
                    "Additional defendant 1 details", "Additional defendant 2 details");
            assertThat(payload.getDefendants()).extracting(ClaimFormDefendantRow::getDisplayName)
                .containsExactly("Bob One", "Carol Two", "Dave Three");
            assertThat(payload.getDefendants()).extracting(ClaimFormDefendantRow::getAddressLine1)
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(1);
            ClaimFormDefendantRow row = payload.getDefendants().getFirst();
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).hasSize(3);
            assertThat(payload.getDefendants()).extracting(ClaimFormDefendantRow::getDisplayName)
                .containsExactly("Persons unknown", "Carwyn Jones", "Persons unknown");
            assertThat(payload.getDefendants()).extracting(ClaimFormDefendantRow::getHeading)
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

            ClaimFormPayload payload = builder.build(pcsCase);

            ClaimFormDefendantRow row = payload.getDefendants().getFirst();
            assertThat(row.getDisplayName()).isEqualTo("Bob Tenant");
            assertThat(row.getAddressLine1()).isEqualTo("99 Property Lane");
        }

        @Test
        void defendantWithNoAddressAndNoPropertyAddressDoesNotThrow() {
            // Defensive: both the defendant's own address and the property fallback can be absent.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setPropertyAddress(null);
            attach(pcsCase, party("A", "Owner", VerticalYesNo.YES, address("1 High St")),
                PartyRole.CLAIMANT, 1);
            attach(pcsCase, party("Bob", "Tenant", VerticalYesNo.YES, null),
                PartyRole.DEFENDANT, 1);

            ClaimFormPayload payload = builder.build(pcsCase);

            ClaimFormDefendantRow row = payload.getDefendants().getFirst();
            assertThat(row.getDisplayName()).isEqualTo("Bob Tenant");
            assertThat(row.getAddressLine1()).isNull();
            assertThat(row.isHasAddressLine2()).isFalse();
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getDefendants()).extracting(ClaimFormDefendantRow::getHeading)
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantDisplayName()).isEqualTo("Possession Claims Solicitor Org");
        }

        @Test
        void displayNameUsesFirstAndLastNameWhenNoOrg() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party("Alice", "Owner", VerticalYesNo.YES, address("X")), PartyRole.CLAIMANT, 1);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantDisplayName()).isEqualTo("Alice Owner");
        }

        @Test
        void displayNameFallsBackToPersonsUnknown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, party(null, null, VerticalYesNo.NO, address("X")), PartyRole.CLAIMANT, 1);

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantIsExemptLandlord()).isEqualTo("No");
            // Housing (Wales) Act 2014 question — shown on Wales.
            assertThat(payload.isShowExemptLandlordQuestion()).isTrue();
        }

        @Test
        void exemptLandlordNullWhenSourceNull() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            attach(pcsCase, orgParty("Org", address("X")), PartyRole.CLAIMANT, 1);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getClaimantIsExemptLandlord()).isNull();
            // The Welsh-only exempt-landlord row must be hidden on England.
            assertThat(payload.isShowExemptLandlordQuestion()).isFalse();
        }
    }

    @Nested
    class Grounds {

        @Test
        void noGroundsImpliesNoOrAbsoluteOrOtherGrounds() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasRentArrearsGround()).isTrue();
            assertThat(payload.getGrounds()).hasSize(1);
            assertThat(payload.getHasGroundsYesNo()).isEqualTo("Yes");
        }

        @Test
        void groundLabelRendersHumanReadableNotRawEnum() {
            // D10/D12: the grounds list must show the ground label, not "<CATEGORY>: <code>".
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.ASSURED_MANDATORY)
                .code("SERIOUS_RENT_ARREARS_GROUND8")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds().getFirst().getNameAndNumber())
                .isEqualTo("Serious rent arrears (ground 8)");
        }

        @Test
        void antisocialConditionsPrefixedWithParentAndRepeated() {
            // Parent checkbox "Antisocial behaviour" with two child conditions: each row repeats the
            // parent, e.g. "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985".
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_ANTISOCIAL)
                .code("S84A_CONDITION_1")
                .reason("condition 1 reason")
                .claim(claim)
                .build());
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_ANTISOCIAL)
                .code("S84A_CONDITION_3")
                .reason("condition 3 reason")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactlyInAnyOrder(
                    "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985",
                    "Antisocial behaviour: Condition 3 of Section 84A of the Housing Act 1985");
            assertThat(payload.getGroundsWithReasons()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactlyInAnyOrder(
                    "Antisocial behaviour: Condition 1 of Section 84A of the Housing Act 1985",
                    "Antisocial behaviour: Condition 3 of Section 84A of the Housing Act 1985");
        }

        @Test
        void groundOneRentArrearsOnlyShowsChildWithNoReasonRow() {
            // Ground 1, "Rent arrears" child only: appears in the grounds list as a child but has no
            // free-text reason, so it never produces a "reason for claiming possession" row.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                .code("RENT_ARREARS_OR_BREACH_OF_TENANCY")
                .isRentArrears(true)
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactly("Rent arrears or breach of the tenancy (ground 1): Rent arrears");
            assertThat(payload.getGroundsWithReasons()).isEmpty();
        }

        @Test
        void groundOneBreachOnlyShowsChildWithReason() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                .code("RENT_ARREARS_OR_BREACH_OF_TENANCY")
                .isRentArrears(false)
                .reason("kept a dog despite a no-pets clause")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactly("Rent arrears or breach of the tenancy (ground 1): Breach of the tenancy");
            assertThat(payload.getGroundsWithReasons()).singleElement()
                .satisfies(row -> {
                    assertThat(row.getNameAndNumber())
                        .isEqualTo("Rent arrears or breach of the tenancy (ground 1): Breach of the tenancy");
                    assertThat(row.getReasonFreeText()).isEqualTo("kept a dog despite a no-pets clause");
                });
        }

        @Test
        void groundOneBothChildrenShowsTwoListRowsAndOneReasonRow() {
            // Both children selected: list shows both; only breach (with free text) is a reason row.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.SECURE_OR_FLEXIBLE_DISCRETIONARY)
                .code("RENT_ARREARS_OR_BREACH_OF_TENANCY")
                .isRentArrears(true)
                .reason("kept a dog despite a no-pets clause")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactlyInAnyOrder(
                    "Rent arrears or breach of the tenancy (ground 1): Rent arrears",
                    "Rent arrears or breach of the tenancy (ground 1): Breach of the tenancy");
            assertThat(payload.getGroundsWithReasons()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactly("Rent arrears or breach of the tenancy (ground 1): Breach of the tenancy");
        }

        @Test
        void walesStandardEstateManagement_prefixedChildrenNoStandaloneParent() {
            // Estate management (s.160) is a parent checkbox: GFP shows prefixed children, not a bare
            // "Estate management grounds (section 160)" parent row.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_STANDARD_OTHER_DISCRETIONARY)
                .code("ESTATE_MANAGEMENT_GROUNDS_S160")
                .claim(claim)
                .build());
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                .code("BUILDING_WORKS")
                .reason("building works reason")
                .claim(claim)
                .build());
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_STANDARD_OTHER_ESTATE_MANAGEMENT)
                .code("CHARITIES")
                .reason("charities reason")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactlyInAnyOrder(
                    "Estate management grounds (section 160): Building works (ground A)",
                    "Estate management grounds (section 160): Charities (ground C)");
            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .doesNotContain("Estate management grounds (section 160)");
            assertThat(payload.getGroundsWithReasons()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactlyInAnyOrder(
                    "Estate management grounds (section 160): Building works (ground A)",
                    "Estate management grounds (section 160): Charities (ground C)");
        }

        @Test
        void walesSecureEstateManagement_prefixedChildrenNoStandaloneParent() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_SECURE_DISCRETIONARY)
                .code("ESTATE_MANAGEMENT_GROUNDS_S160")
                .claim(claim)
                .build());
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.WALES_SECURE_ESTATE_MANAGEMENT)
                .code("RESERVE_SUCCESSORS")
                .reason("reserve successors reason")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .containsExactly("Estate management grounds (section 160): Reserve successors (ground G)");
            assertThat(payload.getGrounds()).extracting(ClaimFormGround::getNameAndNumber)
                .doesNotContain("Estate management grounds (section 160)");
        }
    }

    @Nested
    class ClaimDetailsShowFlags {

        @ParameterizedTest
        @MethodSource("countryGatedSections")
        void countryOnlySectionsAreGatedByLegislativeCountry(LegislativeCountry country, boolean wales) {
            ClaimFormPayload payload = builder.build(minimalCase(country));

            // Wales-only sections.
            assertThat(payload.isShowPcscSection()).isEqualTo(wales);
            assertThat(payload.isShowRequiredDocumentsSection()).isEqualTo(wales);
            assertThat(payload.isShowExemptLandlordQuestion()).isEqualTo(wales);
            // (showTenancyUploadedQuestion is England-only AND answer-gated — covered separately.)
        }

        private static Stream<Arguments> countryGatedSections() {
            return Stream.of(
                Arguments.argumentSet("England", LegislativeCountry.ENGLAND, false),
                Arguments.argumentSet("Wales", LegislativeCountry.WALES, true)
            );
        }

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
                .reason("why other")
                .description("other description")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDescriptionOfGrounds()).isTrue();
            assertThat(payload.getOtherGroundsDescription()).isEqualTo("other description");
            assertThat(payload.getWhyClaimingPossessionGrounds()).hasSize(1);
            assertThat(payload.getWhyClaimingPossessionGrounds().getFirst().getNameAndNumber())
                .isEqualTo("Other grounds");
        }

        @Test
        void englandIntroDemotedOther_showsGroundsYesNoQuestion() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.OTHER)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowGroundsYesNoQuestion()).isTrue();
        }

        @Test
        void walesOtherOccupationType_hidesGroundsYesNoQuestion() {
            // Wales "Other" shares CombinedLicenceType.OTHER with England, but the grounds yes/no
            // question is England intro/demoted/other only and must not render for Wales.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.OTHER)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowGroundsYesNoQuestion()).isFalse();
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDescriptionOfGrounds()).isFalse();
            assertThat(payload.getWhyClaimingPossessionGrounds()).isEmpty();
        }

        @Test
        void englandIntroDemotedOtherWithAbsoluteGround_whyClaimingShownDescriptionHidden() {
            // Absolute grounds answer the general "Why are you claiming possession?" question; they are
            // identified by the code ABSOLUTE_GROUNDS, not by the category.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER)
                .code("ABSOLUTE_GROUNDS")
                .reason("absolute reason")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getWhyClaimingPossessionGrounds()).hasSize(1);
            assertThat(payload.getWhyClaimingPossessionGrounds().getFirst().getNameAndNumber())
                .isEqualTo("Absolute grounds");
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isHasOtherGround()).isFalse();
            assertThat(payload.isShowDescriptionOfGrounds()).isFalse();
            assertThat(payload.getWhyClaimingPossessionGrounds()).isEmpty();
        }

        @Test
        void englandIntroNoGrounds_whyClaimingPossessionShowsTheNoGroundsReason() {
            // HDPI-6478 bug fix: the no-grounds reason was captured but never mapped, so the
            // "Why is the claimant claiming possession?" row rendered with a blank value.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER_NO_GROUNDS)
                .code("NO_GROUNDS")
                .reason("test-intro flow")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getWhyClaimingPossessionGrounds()).hasSize(1);
            assertThat(payload.getWhyClaimingPossessionGrounds().getFirst().getNameAndNumber()).isNull();
            assertThat(payload.getWhyClaimingPossessionGrounds().getFirst().getReasonFreeText())
                .isEqualTo("test-intro flow");
        }

        @Test
        void englandIntroCombination_absoluteAndOtherReasonsFeedWhyClaimingNotPerGround() {
            // Combination flow: the Absolute and Other answers are the "Why are you claiming
            // possession?" answers (D13), combined; Breach is an "under this ground" answer (D12).
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.INTRODUCTORY_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER).code("ABSOLUTE_GROUNDS")
                .reason("3").claim(claim).build());
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER).code("OTHER")
                .reason("4").claim(claim).build());
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.INTRODUCTORY_DEMOTED_OTHER).code("BREACH_OF_THE_TENANCY")
                .reason("2").claim(claim).build());

            ClaimFormPayload payload = builder.build(pcsCase);

            List<ClaimFormGround> why = payload.getWhyClaimingPossessionGrounds();
            assertThat(why).hasSize(2);
            assertThat(why.get(0).getNameAndNumber()).isEqualTo("Absolute grounds");
            assertThat(why.get(0).getReasonFreeText()).isEqualTo("3");
            assertThat(why.get(1).getNameAndNumber()).isEqualTo("Other grounds");
            assertThat(why.get(1).getReasonFreeText()).isEqualTo("4");
            // Absolute/Other reasons are NOT duplicated in the per-ground D12 list; only Breach remains.
            assertThat(payload.getGroundsWithReasons()).hasSize(1);
            assertThat(payload.getGroundsWithReasons().getFirst().getReasonFreeText()).isEqualTo("2");
            // The grounds list (D10) still shows all three selected grounds.
            assertThat(payload.getGrounds()).hasSize(3);
        }

        @Test
        void assuredOtherGround_descriptionShown() {
            // Gap fix: the assured "Other" ground description was captured but never rendered.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .build());
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.getClaimGrounds().add(ClaimGroundEntity.builder()
                .category(ClaimGroundCategory.ASSURED_OTHER)
                .code("OTHER")
                .description("assured other description")
                .claim(claim)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDescriptionOfGrounds()).isTrue();
            assertThat(payload.getOtherGroundsDescription()).isEqualTo("assured other description");
            // Assured never hits the "Why is the claimant claiming possession?" row.
            assertThat(payload.getWhyClaimingPossessionGrounds()).isEmpty();
        }

        @Test
        void issueDateSealedMappedFromClaimIssuedDate() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setClaimIssuedDate(LocalDateTime.of(2026, 2, 12, 9, 0));

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, 2, 12));
        }

        @Test
        void issueDateUsesUkCalendarDayForTimestampJustAfterMidnightBst() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            // 23:30 UTC on 15 July = 00:30 BST on 16 July - the UK day is the 16th.
            pcsCase.getClaims().getFirst().setClaimIssuedDate(LocalDateTime.of(2026, 7, 15, 23, 30));

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIssueDateSealed()).isEqualTo(LocalDate.of(2026, 7, 16));
        }

        @Test
        void submittedOnUsesStoredSubmissionDateAsUkDate() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setClaimSubmittedDate(LocalDateTime.of(2026, 3, 1, 10, 30));

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getSubmittedOn()).isEqualTo(LocalDate.of(2026, 3, 1));
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getGrounds()).hasSize(2);
            assertThat(payload.getGroundsWithReasons()).hasSize(1);
            assertThat(payload.getGroundsWithReasons().getFirst().getReasonFreeText())
                .isEqualTo("Defendant in continuous arrears for 18 months.");
        }

        @Test
        void additionalReasonsNotProvided_freeTextRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setAdditionalReasonsProvided(VerticalYesNo.NO);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasAdditionalReasonsYesNo()).isEqualTo("No");
            assertThat(payload.isAdditionalReasonsProvided()).isFalse();
        }

        @Test
        void additionalReasonsProvided_freeTextRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setAdditionalReasonsProvided(VerticalYesNo.YES);
            claim.setAdditionalReasons("Defendant has refused mediation.");

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowDescriptionOfGrounds()).isFalse();
            assertThat(payload.getWhyClaimingPossessionGrounds()).isEmpty();
        }
    }

    @Nested
    class GapFieldsRemainNull {

        @Test
        void issueDateAndWhyClaimingAndNoticeNotUploadedAndWalesDocsAllNull() {
            // With no source data these stay null/empty (not thrown).
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIssueDateSealed()).isNull();
            assertThat(payload.getWhyClaimingPossessionGrounds()).isEmpty();
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
            ClaimFormPayload payload = builder.build(pcsCase);
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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isIntroDemotedOtherTenancy()).isTrue();
            assertThat(payload.getTenancyStartDate()).isEqualTo("1 January 2024");
            assertThat(payload.getRentAmount()).isEqualTo("£100.00");
        }

        @Test
        void assuredTenancyIsNotIntroDemotedOther() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            TenancyLicenceEntity t = TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .build();
            pcsCase.setTenancyLicence(t);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isIntroDemotedOtherTenancy()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("tenancyTypeLabels")
        void tenancyTypeRendersHumanLabelNotEnumName(LegislativeCountry country,
                                                     CombinedLicenceType type, String expectedLabel) {
            PcsCaseEntity pcsCase = minimalCase(country);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder().type(type).build());

            assertThat(builder.build(pcsCase).getTenancyTypeLabel()).isEqualTo(expectedLabel);
        }

        @ParameterizedTest
        @MethodSource("rentFrequencyDescriptions")
        void rentFrequencyRendersHumanLabelNotEnumName(RentPaymentFrequency frequency, String expectedDescription) {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .rentAmount(new BigDecimal("500.00"))
                .rentFrequency(frequency)
                .build());

            assertThat(builder.build(pcsCase).getRentCalculatedDescription()).isEqualTo(expectedDescription);
        }

        @Test
        void otherTenancyTypeAppendsTheFreeTextDetails() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.OTHER)
                .otherTypeDetails("Lifetime tenancy")
                .build());

            assertThat(builder.build(pcsCase).getTenancyTypeLabel()).isEqualTo("Other: Lifetime tenancy");
        }

        private static Stream<Arguments> tenancyTypeLabels() {
            return Stream.of(
                Arguments.argumentSet("England assured", LegislativeCountry.ENGLAND,
                    CombinedLicenceType.ASSURED_TENANCY, "Assured tenancy"),
                Arguments.argumentSet("England secure", LegislativeCountry.ENGLAND,
                    CombinedLicenceType.SECURE_TENANCY, "Secure tenancy"),
                Arguments.argumentSet("England introductory", LegislativeCountry.ENGLAND,
                    CombinedLicenceType.INTRODUCTORY_TENANCY, "Introductory tenancy"),
                Arguments.argumentSet("England flexible", LegislativeCountry.ENGLAND,
                    CombinedLicenceType.FLEXIBLE_TENANCY, "Flexible tenancy"),
                Arguments.argumentSet("England demoted", LegislativeCountry.ENGLAND,
                    CombinedLicenceType.DEMOTED_TENANCY, "Demoted tenancy"),
                Arguments.argumentSet("Wales secure contract", LegislativeCountry.WALES,
                    CombinedLicenceType.SECURE_CONTRACT, "Secure contract"),
                Arguments.argumentSet("Wales standard contract", LegislativeCountry.WALES,
                    CombinedLicenceType.STANDARD_CONTRACT, "Standard contract")
            );
        }

        private static Stream<Arguments> rentFrequencyDescriptions() {
            return Stream.of(
                Arguments.argumentSet("weekly", RentPaymentFrequency.WEEKLY, "Weekly"),
                Arguments.argumentSet("fortnightly", RentPaymentFrequency.FORTNIGHTLY, "Fortnightly"),
                Arguments.argumentSet("monthly", RentPaymentFrequency.MONTHLY, "Monthly"),
                Arguments.argumentSet("other", RentPaymentFrequency.OTHER, "Other")
            );
        }

        @Test
        void tenancyUploadedComplementaryBooleansDriveDocmosisBranches() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            TenancyLicenceEntity t = TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .hasCopyOfTenancyLicence(VerticalYesNo.YES)
                .build();
            pcsCase.setTenancyLicence(t);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isTenancyUploadedYes()).isTrue();
            assertThat(payload.isTenancyUploadedNo()).isFalse();
        }

        @Test
        void tenancyUploadedQuestionShownForEnglandWhenAnswered() {
            // D49/D50 are England-only (Cook [35]/[36]); shown only when the claimant answered.
            PcsCaseEntity england = minimalCase(LegislativeCountry.ENGLAND);
            england.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .hasCopyOfTenancyLicence(VerticalYesNo.YES).build());
            assertThat(builder.build(england).isShowTenancyUploadedQuestion()).isTrue();

            PcsCaseEntity wales = minimalCase(LegislativeCountry.WALES);
            wales.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.SECURE_CONTRACT)
                .hasCopyOfTenancyLicence(VerticalYesNo.YES).build());
            assertThat(builder.build(wales).isShowTenancyUploadedQuestion()).isFalse();
        }

        @Test
        void tenancyUploadedQuestionHiddenWhenUnanswered() {
            // Hide the row (don't print a blank label) when the England claimant didn't answer.
            PcsCaseEntity england = minimalCase(LegislativeCountry.ENGLAND);
            england.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY).build());  // no hasCopyOfTenancyLicence
            assertThat(builder.build(england).isShowTenancyUploadedQuestion()).isFalse();
        }

        @Test
        void noticeUploadQuestionHiddenWhileUnsourced() {
            // R43/R44 has no entity source yet → row hidden, not a blank label.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES).build());
            assertThat(builder.build(pcsCase).isShowNoticeUploadQuestion()).isFalse();
        }

        @Test
        void tenancyUploadedNoFlipsTheOtherDirection() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            TenancyLicenceEntity t = TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .hasCopyOfTenancyLicence(VerticalYesNo.NO)
                .build();
            pcsCase.setTenancyLicence(t);

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getIsDemotionClaimYesNo()).isEqualTo("Yes");
            assertThat(payload.getDemotionReasonsFreeText()).isEqualTo("Anti-social behaviour");
            assertThat(payload.getIsSuspensionClaimYesNo()).isEqualTo("No");
        }

        @Test
        void demotionNotAnswered_wholeSectionHidden() {
            // Optional Y/N: when user didn't answer, the whole demotion section disappears.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            // PossessionAlternativesEntity not set at all.

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasClaimantCircsYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowClaimantCircsFreeText()).isTrue();
            assertThat(payload.getClaimantCircsFreeText()).isEqualTo("Two children in the household.");
        }

        @Test
        void claimantCircsNo_detailsRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setClaimantCircumstancesProvided(VerticalYesNo.NO);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasClaimantCircsYesNo()).isEqualTo("No");
            assertThat(payload.isShowClaimantCircsFreeText()).isFalse();
        }

        @Test
        void defendantCircsYes_detailsRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setDefendantCircumstancesProvided(VerticalYesNo.YES);
            claim.setDefendantCircumstances("Defendant has been unemployed.");

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getHasDefendantCircsYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowDefendantCircsFreeText()).isTrue();
        }

        @Test
        void defendantCircsNo_detailsRowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setDefendantCircumstancesProvided(VerticalYesNo.NO);

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowTenancyStartDate()).isTrue();
            assertThat(payload.getTenancyStartDate()).isEqualTo("1 January 2024");
        }

        @Test
        void startDateNull_rowHidden() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.setTenancyLicence(TenancyLicenceEntity.builder()
                .type(CombinedLicenceType.ASSURED_TENANCY)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowPcscSection()).isTrue();
        }

        @Test
        void englandHidesPcscSection() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowPcscDetails()).isTrue();
            assertThat(payload.isShowPcscTermsFreeText()).isFalse();
        }
    }

    @Nested
    class RequiredDocumentsSection {

        @Test
        void walesShowsRequiredDocumentsSection() {
            ClaimFormPayload payload = builder.build(minimalCase(LegislativeCountry.WALES));
            assertThat(payload.isShowRequiredDocumentsSection()).isTrue();
        }

        @Test
        void englandHidesRequiredDocumentsSection() {
            ClaimFormPayload payload = builder.build(minimalCase(LegislativeCountry.ENGLAND));
            assertThat(payload.isShowRequiredDocumentsSection()).isFalse();
        }

        @Test
        void mapsUploadedAnswersWhenAllProvided() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setEnergyPerformanceCertificateProvided(VerticalYesNo.YES);
            claim.setGasSafetyReportProvided(VerticalYesNo.YES);
            claim.setElectricalInstallationConditionProvided(VerticalYesNo.YES);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getEpcUploadedYesNo()).isEqualTo("Yes");
            assertThat(payload.getGasSafetyUploadedYesNo()).isEqualTo("Yes");
            assertThat(payload.getEicrUploadedYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowEpcNotUploadedReason()).isFalse();
            assertThat(payload.isShowGasSafetyNotUploadedReason()).isFalse();
            assertThat(payload.isShowEicrNotUploadedReason()).isFalse();
        }

        @Test
        void mapsNotUploadedReasonsWhenNotProvided() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setEnergyPerformanceCertificateProvided(VerticalYesNo.NO);
            claim.setNoEnergyPerformanceCertificateReason("EPC pending from agent");
            claim.setGasSafetyReportProvided(VerticalYesNo.NO);
            claim.setNoGasSafetyReportReason("Gas check booked");
            claim.setElectricalInstallationConditionProvided(VerticalYesNo.NO);
            claim.setNoElectricalInstallationConditionReason("EICR not yet done");

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getEpcUploadedYesNo()).isEqualTo("No");
            assertThat(payload.isShowEpcNotUploadedReason()).isTrue();
            assertThat(payload.getEpcNotUploadedReason()).isEqualTo("EPC pending from agent");
            assertThat(payload.getGasSafetyUploadedYesNo()).isEqualTo("No");
            assertThat(payload.isShowGasSafetyNotUploadedReason()).isTrue();
            assertThat(payload.getGasSafetyNotUploadedReason()).isEqualTo("Gas check booked");
            assertThat(payload.getEicrUploadedYesNo()).isEqualTo("No");
            assertThat(payload.isShowEicrNotUploadedReason()).isTrue();
            assertThat(payload.getEicrNotUploadedReason()).isEqualTo("EICR not yet done");
        }

        @Test
        void leavesValuesUnsetWhenNotAnswered() {
            ClaimFormPayload payload = builder.build(minimalCase(LegislativeCountry.WALES));
            assertThat(payload.getEpcUploadedYesNo()).isNull();
            assertThat(payload.getGasSafetyUploadedYesNo()).isNull();
            assertThat(payload.getEicrUploadedYesNo()).isNull();
            assertThat(payload.isShowEpcNotUploadedReason()).isFalse();
            assertThat(payload.isShowGasSafetyNotUploadedReason()).isFalse();
            assertThat(payload.isShowEicrNotUploadedReason()).isFalse();
        }
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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getPreActionProtocolFollowedYesNo()).isEqualTo("Yes");
            assertThat(payload.isShowPreActionProtocolNotFollowedReason()).isFalse();
        }

        @Test
        void preActionProtocolNotFollowed_reasonRowShown() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setPreActionProtocolFollowed(VerticalYesNo.NO);
            claim.setPreActionProtocolIncompleteExplanation("Defendant in hospital — couldn't deliver.");

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getPreActionProtocolFollowedYesNo()).isEqualTo("No");
            assertThat(payload.isShowPreActionProtocolNotFollowedReason()).isTrue();
            assertThat(payload.getPreActionProtocolNotFollowedReason())
                .isEqualTo("Defendant in hospital — couldn't deliver.");
        }

        @Test
        void preActionProtocolNotFollowedButNoReason_reasonRowHidden() {
            // Drop-on-null: e.g. the Wales journey doesn't capture the reason → don't print a blank row.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            pcsCase.getClaims().getFirst().setPreActionProtocolFollowed(VerticalYesNo.NO);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowPreActionProtocolNotFollowedReason()).isFalse();
        }

        @Test
        void mediationAndSettlementYNRenderedAsTitleCaseString() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setMediationAttempted(VerticalYesNo.YES);
            claim.setSettlementAttempted(VerticalYesNo.NO);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getMediationAttemptedYesNo()).isEqualTo("Yes");
            assertThat(payload.getSettlementAttemptedYesNo()).isEqualTo("No");
        }

        @Test
        void mediationAndSettlementAnswered_showFlagsTrue() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setMediationAttempted(VerticalYesNo.YES);
            claim.setSettlementAttempted(VerticalYesNo.NO);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowMediationAttempted()).isTrue();
            assertThat(payload.isShowSettlementAttempted()).isTrue();
        }

        @Test
        void mediationAndSettlementUnanswered_rowsHidden() {
            // Optional page skipped: both rows must hide rather than render a blank value.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setMediationAttempted(null);
            claim.setSettlementAttempted(null);

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowMediationAttempted()).isFalse();
            assertThat(payload.getMediationAttemptedYesNo()).isNull();
            assertThat(payload.isShowSettlementAttempted()).isFalse();
            assertThat(payload.getSettlementAttemptedYesNo()).isNull();
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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getNoticeServedYesNo()).isEqualTo("No");
            assertThat(payload.isNoticeNotServedDisplayed()).isTrue();
            assertThat(payload.getNoticeNotServedReason())
                .isEqualTo("Defendant address unknown — no notice possible.");
        }

        @Test
        void noticeServedNoButNoStatement_whyNotServedRowHidden() {
            // Drop-on-null: England doesn't capture a 'why not served' reason → don't print a blank row.
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.NO)
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getNoticeServedYesNo()).isEqualTo("No");
            assertThat(payload.isNoticeNotServedDisplayed()).isFalse();
        }

        @Test
        void walesAndNoticeServedYes_showNoticeTypeTrue() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.WALES);
            ClaimEntity claim = pcsCase.getClaims().getFirst();
            claim.setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .noticeType("Section 173")
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeLeftWithName()).isFalse();
            assertThat(payload.isShowNoticeServedToEmail()).isFalse();
            assertThat(payload.isShowNoticeOtherElectronicDetails()).isFalse();
            assertThat(payload.isShowNoticeOtherMeansDetails()).isFalse();
        }

        @Test
        void dateOnlyMethodRendersFormattedDateAndNoTime() {
            // FIRST_CLASS_POST / DELIVERED_PERMITTED_PLACE store a date-only value (noticeDate).
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .servingMethod(NoticeServiceMethod.FIRST_CLASS_POST)
                .noticeDate(LocalDate.of(2024, 1, 10))
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeServedOn()).isTrue();
            assertThat(payload.getNoticeServedOn()).isEqualTo("10 January 2024");
            assertThat(payload.isShowNoticeServedTime()).isFalse();
            assertThat(payload.getNoticeServedTime()).isNull();
        }

        @Test
        void dateTimeMethodRendersBothFormattedDateAndTime() {
            // Regression: PERSONALLY_HANDED / EMAIL / OTHER_ELECTRONIC / OTHER store a date+time
            // (noticeDateTime). The served DATE must still render — it was previously dropped
            // because the date was sourced only from noticeDate (null for these methods).
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            pcsCase.getClaims().getFirst().setNoticeOfPossession(NoticeOfPossessionEntity.builder()
                .noticeServed(YesOrNo.YES)
                .servingMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                .noticeDateTime(LocalDateTime.of(2024, 1, 10, 14, 30))
                .build());

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.isShowNoticeServedOn()).isTrue();
            assertThat(payload.getNoticeServedOn()).isEqualTo("10 January 2024");
            assertThat(payload.isShowNoticeServedTime()).isTrue();
            assertThat(payload.getNoticeServedTime()).isEqualTo("2:30pm");
        }
    }

    @Nested
    class Underlessees {

        @Test
        void emptyListWhenNoUnderlessees() {
            PcsCaseEntity pcsCase = minimalCase(LegislativeCountry.ENGLAND);
            // No underlessees attached.

            ClaimFormPayload payload = builder.build(pcsCase);

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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getUnderlessees()).hasSize(1);
            ClaimFormUnderlesseeRow row = payload.getUnderlessees().getFirst();
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

            ClaimFormPayload payload = builder.build(pcsCase);

            ClaimFormUnderlesseeRow row = payload.getUnderlessees().getFirst();
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

            ClaimFormPayload payload = builder.build(pcsCase);

            ClaimFormUnderlesseeRow row = payload.getUnderlessees().getFirst();
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

            ClaimFormPayload payload = builder.build(pcsCase);

            assertThat(payload.getUnderlessees()).hasSize(2);
            assertThat(payload.getUnderlessees()).extracting(ClaimFormUnderlesseeRow::getHeading)
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
