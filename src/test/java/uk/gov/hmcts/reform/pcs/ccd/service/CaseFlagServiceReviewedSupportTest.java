package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.FlagVisibility;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.entity.CasePartyFlagEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CaseFlagServiceReviewedSupportTest {

    @InjectMocks
    private CaseFlagService underTest;

    private PartyEntity partyEntity;
    private CasePartyFlagEntity requestedFlag;
    private CasePartyFlagEntity activeFlag;
    private CasePartyFlagEntity internalRequestedFlag;

    @BeforeEach
    void setUp() {
        requestedFlag = flagEntity("Requested", FlagVisibility.EXTERNAL);
        activeFlag = flagEntity("Active", FlagVisibility.EXTERNAL);
        internalRequestedFlag = flagEntity("Requested", FlagVisibility.INTERNAL);

        partyEntity = PartyEntity.builder().id(UUID.randomUUID()).build();
        partyEntity.setDefendantFlags(new ArrayList<>(
            List.of(requestedFlag, activeFlag, internalRequestedFlag)));
    }

    @Test
    void shouldApplyStatusAndReasonToTheReviewedFlag() {
        LocalDateTime modified = LocalDateTime.of(2026, 8, 12, 10, 0);

        underTest.applyReviewedSupportFlags(
            reviewedSupport(requestedFlag.getId(), "Active", "Approved by team leader", modified),
            Set.of(partyEntity));

        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(requestedFlag.getFlagUpdateComment()).isEqualTo("Approved by team leader");
        assertThat(requestedFlag.getDateTimeModified()).isEqualTo(modified);
    }

    /**
     * Setting a reviewed support flag to Inactive is the outcome the reviewer journey is signed off on,
     * so the stored status, reason and modified timestamp are all asserted rather than only checking
     * that the flag survived the review.
     */
    @Test
    void shouldPersistInactiveStatusReasonAndModifiedDateToTheReviewedFlag() {
        LocalDateTime modified = LocalDateTime.of(2026, 8, 21, 9, 30);

        underTest.applyReviewedSupportFlags(
            reviewedSupport(requestedFlag.getId(), "Inactive", "Support no longer required", modified),
            Set.of(partyEntity));

        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Inactive");
        assertThat(requestedFlag.getFlagUpdateComment()).isEqualTo("Support no longer required");
        assertThat(requestedFlag.getDateTimeModified()).isEqualTo(modified);
        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(internalRequestedFlag.getDefaultStatus()).isEqualTo("Requested");
    }

    @Test
    void shouldSupportNotApprovedDecision() {
        underTest.applyReviewedSupportFlags(
            reviewedSupport(requestedFlag.getId(), "Not approved", "Insufficient evidence", null),
            Set.of(partyEntity));

        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Not approved");
    }

    @Test
    void shouldNotRemoveAnyExistingFlags() {
        underTest.applyReviewedSupportFlags(
            reviewedSupport(requestedFlag.getId(), "Inactive", "No longer needed", null),
            Set.of(partyEntity));

        assertThat(partyEntity.getDefendantFlags())
            .containsExactlyInAnyOrder(requestedFlag, activeFlag, internalRequestedFlag);
    }

    @Test
    void shouldNotModifyFlagsThatWereNotRequested() {
        underTest.applyReviewedSupportFlags(
            reviewedSupport(activeFlag.getId(), "Inactive", "Attempted change", null),
            Set.of(partyEntity));

        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(activeFlag.getFlagUpdateComment()).isNull();
    }

    @Test
    void shouldApplyReviewToARequestedFlagStoredAsInternal() {
        LocalDateTime modified = LocalDateTime.of(2026, 8, 27, 11, 15);

        underTest.applyReviewedSupportFlags(
            reviewedSupport(internalRequestedFlag.getId(), "Active", "Adjustment agreed", modified),
            Set.of(partyEntity));

        assertThat(internalRequestedFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(internalRequestedFlag.getFlagUpdateComment()).isEqualTo("Adjustment agreed");
        assertThat(internalRequestedFlag.getDateTimeModified()).isEqualTo(modified);
        assertThat(internalRequestedFlag.getVisibility()).isEqualTo(FlagVisibility.INTERNAL.getValue());
    }

    @Test
    void shouldLeaveStoredVisibilityUntouchedWhenReviewingAnExternalFlag() {
        underTest.applyReviewedSupportFlags(
            reviewedSupport(requestedFlag.getId(), "Not approved", "Insufficient evidence", null),
            Set.of(partyEntity));

        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Not approved");
        assertThat(requestedFlag.getVisibility()).isEqualTo(FlagVisibility.EXTERNAL.getValue());
    }

    @Test
    void shouldReviewInternalAndExternalRequestedFlagsInOneSubmission() {
        List<ListValue<PartySupport>> reviewed = List.of(
            ListValue.<PartySupport>builder()
                .id(partyEntity.getId().toString())
                .value(PartySupport.builder()
                    .supportFlags(Flags.builder()
                        .details(List.of(
                            reviewedDetail(requestedFlag.getId(), "Active", "External approved"),
                            reviewedDetail(internalRequestedFlag.getId(), "Not approved", "Internal refused")))
                        .build())
                    .build())
                .build());

        underTest.applyReviewedSupportFlags(reviewed, Set.of(partyEntity));

        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(requestedFlag.getFlagUpdateComment()).isEqualTo("External approved");
        assertThat(internalRequestedFlag.getDefaultStatus()).isEqualTo("Not approved");
        assertThat(internalRequestedFlag.getFlagUpdateComment()).isEqualTo("Internal refused");
        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(activeFlag.getFlagUpdateComment()).isNull();
    }

    @Test
    void shouldIgnoreUnknownFlagIds() {
        underTest.applyReviewedSupportFlags(
            reviewedSupport(UUID.randomUUID(), "Active", "Unknown flag", null),
            Set.of(partyEntity));

        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Requested");
        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
    }

    @Test
    void shouldRejectSupportForAPartyNotOnTheCase() {
        List<ListValue<PartySupport>> reviewed =
            reviewedSupport(requestedFlag.getId(), "Active", "Approved", null);
        reviewed.getFirst().setId(UUID.randomUUID().toString());

        assertThatThrownBy(() -> underTest.applyReviewedSupportFlags(reviewed, Set.of(partyEntity)))
            .isInstanceOf(CaseAccessException.class);
    }

    @Test
    void shouldIgnoreEntryWhenReviewedValueIsNull() {
        // Given
        List<ListValue<PartySupport>> reviewed = List.of(
            ListValue.<PartySupport>builder().id(partyEntity.getId().toString()).value(null).build()
        );

        // When
        underTest.applyReviewedSupportFlags(reviewed, Set.of(partyEntity));

        // Then
        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Requested");
        assertThat(requestedFlag.getFlagUpdateComment()).isNull();
        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
    }

    @Test
    void shouldIgnoreEntryWhenSupportFlagsAreNull() {
        // Given
        List<ListValue<PartySupport>> reviewed = List.of(
            ListValue.<PartySupport>builder().id(partyEntity.getId().toString())
                .value(PartySupport.builder().supportFlags(null).build()).build()
        );

        // When
        underTest.applyReviewedSupportFlags(reviewed, Set.of(partyEntity));

        // Then
        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Requested");
        assertThat(requestedFlag.getFlagUpdateComment()).isNull();
        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
    }

    @Test
    void shouldIgnoreEntryWhenFlagDetailsAreNull() {
        // Given
        List<ListValue<PartySupport>> reviewed = List.of(
            ListValue.<PartySupport>builder().id(partyEntity.getId().toString())
                .value(PartySupport.builder().supportFlags(Flags.builder().details(null).build()).build())
                .build()
        );

        // When
        underTest.applyReviewedSupportFlags(reviewed, Set.of(partyEntity));

        // Then
        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Requested");
        assertThat(requestedFlag.getFlagUpdateComment()).isNull();
        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
    }

    @Test
    void shouldIgnoreEntryWhenFlagDetailsAreEmpty() {
        // Given
        List<ListValue<PartySupport>> reviewed = List.of(
            ListValue.<PartySupport>builder().id(partyEntity.getId().toString())
                .value(PartySupport.builder().supportFlags(Flags.builder().details(List.of()).build()).build())
                .build()
        );

        // When
        underTest.applyReviewedSupportFlags(reviewed, Set.of(partyEntity));

        // Then
        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Requested");
        assertThat(requestedFlag.getFlagUpdateComment()).isNull();
        assertThat(activeFlag.getDefaultStatus()).isEqualTo("Active");
    }

    @Test
    void shouldIgnoreReviewedDetailWithNoValue() {
        // Given
        List<ListValue<PartySupport>> reviewed = List.of(
            ListValue.<PartySupport>builder().id(partyEntity.getId().toString())
                .value(PartySupport.builder()
                    .supportFlags(Flags.builder()
                        .details(List.of(ListValue.<FlagDetail>builder()
                            .id(requestedFlag.getId().toString())
                            .value(null)
                            .build()))
                        .build())
                    .build())
                .build()
        );

        // When
        underTest.applyReviewedSupportFlags(reviewed, Set.of(partyEntity));

        // Then
        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Requested");
        assertThat(requestedFlag.getFlagUpdateComment()).isNull();
    }

    private ListValue<FlagDetail> reviewedDetail(UUID flagId, String status, String reason) {
        return ListValue.<FlagDetail>builder()
            .id(flagId.toString())
            .value(FlagDetail.builder().status(status).flagUpdateComment(reason).build())
            .build();
    }

    private List<ListValue<PartySupport>> reviewedSupport(UUID flagId, String status, String reason,
                                                          LocalDateTime modified) {
        List<ListValue<PartySupport>> reviewed = new ArrayList<>();
        reviewed.add(ListValue.<PartySupport>builder()
            .id(partyEntity.getId().toString())
            .value(PartySupport.builder()
                .supportFlags(Flags.builder()
                    .details(List.of(ListValue.<FlagDetail>builder()
                        .id(flagId.toString())
                        .value(FlagDetail.builder()
                            .status(status)
                            .flagUpdateComment(reason)
                            .dateTimeModified(modified)
                            .build())
                        .build()))
                    .build())
                .build())
            .build());
        return reviewed;
    }

    @Test
    void shouldRecordTheReviewReasonWithoutDisturbingThePartysOwnComment() {
        requestedFlag.setFlagComment("Tell us why you need a nearby parking space");
        requestedFlag.setFlagCommentWelsh("Welsh original");
        requestedFlag.setSubTypeValue("British Sign Language (BSL)");
        requestedFlag.setOtherDescription("Other description");
        LocalDateTime created = LocalDateTime.of(2026, 8, 28, 14, 57, 52);
        requestedFlag.setDateTimeCreated(created);

        UUID flagId = requestedFlag.getId();
        LocalDateTime modified = LocalDateTime.of(2026, 8, 28, 15, 20, 36);
        underTest.applyReviewedSupportFlags(
            reviewedSupport(flagId, "Active", "Updated", modified),
            Set.of(partyEntity));

        assertThat(requestedFlag.getId()).isEqualTo(flagId);
        assertThat(requestedFlag.getVisibility()).isEqualTo(FlagVisibility.EXTERNAL.getValue());
        assertThat(requestedFlag.getFlagUpdateComment()).isEqualTo("Updated");
        assertThat(requestedFlag.getDefaultStatus()).isEqualTo("Active");
        assertThat(requestedFlag.getDateTimeModified()).isEqualTo(modified);

        assertThat(requestedFlag.getFlagComment())
            .as("the party's own support comment must survive the review")
            .isEqualTo("Tell us why you need a nearby parking space");
        assertThat(requestedFlag.getFlagCommentWelsh()).isEqualTo("Welsh original");
        assertThat(requestedFlag.getSubTypeValue()).isEqualTo("British Sign Language (BSL)");
        assertThat(requestedFlag.getOtherDescription()).isEqualTo("Other description");
        assertThat(requestedFlag.getDateTimeCreated()).isEqualTo(created);
    }

    private CasePartyFlagEntity flagEntity(String status, FlagVisibility visibility) {
        CasePartyFlagEntity flag = new CasePartyFlagEntity();
        flag.setId(UUID.randomUUID());
        flag.setDefaultStatus(status);
        flag.setVisibility(visibility.getValue());
        return flag;
    }
}
