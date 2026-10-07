package uk.gov.hmcts.reform.pcs.ccd.event.order;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalEventId;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalRejection;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalStartResponse;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitRequest;
import uk.gov.hmcts.ccd.sdk.api.external.ExternalSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.DraftOrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.OrderChange;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderStart;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderStart.CaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DraftOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.TenancyLicenceEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.NoticeOfPossessionEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.DraftOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderRequest.Action.SUBMIT_FOR_REVIEW;

/**
 * A judge makes an order through pcs-frontend's make order journey. Starting the event sends them
 * their working order and the facts of the case; each submission saves or submits for review their
 * draft. A judge has one working draft per case, and a change is made from the version they last
 * saw; a change with no id starts their draft.
 */
@Component
@AllArgsConstructor
public class MakeOrder implements CCDConfig<PCSCase, State, UserRole> {

    public static final ExternalEventId<MakeOrderStart, MakeOrderRequest> MAKE_ORDER =
        ExternalEventId.of("ext:makeOrder", MakeOrderStart.class, MakeOrderRequest.class);

    private final DraftOrderRepository draftOrderRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final AddressMapper addressMapper;
    private final PartyService partyService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .externalEvent(MAKE_ORDER, this::submit)
            .forStates(
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME
            )
            .name("Make an order")
            .showCondition(ShowConditions.featureFlagsEnabled(FeatureFlag.MAKE_ORDER))
            .grant(Permission.CRUD,
                UserRole.JUDGE,
                UserRole.FEE_PAID_JUDGE,
                UserRole.CIRCUIT_JUDGE,
                UserRole.LEADERSHIP_JUDGE)
            .onStart(this::start);
    }

    private ExternalStartResponse<MakeOrderStart> start(ExternalStartRequest start) {
        long caseReference = start.caseReference();
        MakeOrderStart.Order workingOrder = findDraft(caseReference, UUID.fromString(start.user().id()))
            .map(MakeOrder::toOrder)
            .orElseGet(() -> new MakeOrderStart.Order(null, DraftOrderState.DRAFT, 0, null, Map.of(), null));
        return ExternalStartResponse.started(new MakeOrderStart(workingOrder, caseContext(findCase(caseReference))));
    }

    private ExternalSubmitResponse<State> submit(ExternalSubmitRequest<MakeOrderRequest> submit) {
        MakeOrderRequest request = submit.payload();
        OrderChange change = request.order();
        long caseReference = submit.caseReference();
        UUID judge = UUID.fromString(submit.user().id());
        DraftOrderEntity draft = change.id() == null
            ? newDraft(caseReference, judge)
            : workingDraft(caseReference, judge, change);
        draft.setOrderType(change.orderType());
        draft.setFormData(change.formData());
        draft.setDocweaveSnapshot(change.docweaveSnapshot());
        if (request.action() == SUBMIT_FOR_REVIEW) {
            draft.setState(DraftOrderState.SUBMITTED_FOR_REVIEW);
        }
        draftOrderRepository.saveAndFlush(draft);
        return request.action() == SUBMIT_FOR_REVIEW
            ? ExternalSubmitResponse.accepted("Order submitted for review", "Submitted an order for caseworker review")
            : ExternalSubmitResponse.accepted("Order draft saved", "Saved an order as a draft");
    }

    /** A judge has one draft per case, which the database also enforces. */
    private DraftOrderEntity newDraft(long caseReference, UUID judge) {
        if (findDraft(caseReference, judge).isPresent()) {
            throw ExternalRejection.because("You already have an order draft for this case");
        }
        return DraftOrderEntity.builder()
            .pcsCase(findCase(caseReference))
            .authorIdamUserId(judge)
            .state(DraftOrderState.DRAFT)
            .build();
    }

    /** The judge's own draft this change is for, at the version they last saw. */
    private DraftOrderEntity workingDraft(long caseReference, UUID judge, OrderChange change) {
        DraftOrderEntity draft = draftOrderRepository
            .findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(
                change.id(), caseReference, judge, DraftOrderState.DRAFT)
            .orElseThrow(() -> ExternalRejection.because("The order draft does not exist for this case"));
        if (draft.getVersion() != change.version()) {
            throw ExternalRejection.because(
                "The order draft has been updated by another user. Reload it and try again");
        }
        return draft;
    }

    private Optional<DraftOrderEntity> findDraft(long caseReference, UUID judge) {
        return draftOrderRepository.findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndState(
            caseReference, judge, DraftOrderState.DRAFT);
    }

    private PcsCaseEntity findCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    private static MakeOrderStart.Order toOrder(DraftOrderEntity order) {
        return new MakeOrderStart.Order(order.getId(), order.getState(), order.getVersion(),
            order.getOrderType(), order.getFormData(), order.getDocweaveSnapshot());
    }

    // The case a judge makes an order on, as the frontend shows it: its property and the parties
    // and facts of its main claim.

    private MakeOrderStart.CaseContext caseContext(PcsCaseEntity pcsCase) {
        Optional<ClaimEntity> claim = pcsCase.getClaims().stream().findFirst();
        return new MakeOrderStart.CaseContext(
            pcsCase.getCaseReference(),
            addressMapper.toAddressUK(pcsCase.getPropertyAddress()),
            claim.map(c -> parties(c, PartyRole.CLAIMANT)).orElse(List.of()),
            claim.map(c -> parties(c, PartyRole.DEFENDANT)).orElse(List.of()),
            caseFacts(pcsCase.getTenancyLicence(), claim.orElse(null))
        );
    }

    private static CaseFacts caseFacts(TenancyLicenceEntity tenancy, ClaimEntity claim) {
        NoticeOfPossessionEntity notice = claim == null ? null : claim.getNoticeOfPossession();
        return new CaseFacts(
            tenancy == null ? null : tenancy.getStartDate(),
            tenancy == null || tenancy.getType() == null ? null : tenancy.getType().name(),
            noticeDate(notice),
            tenancy == null ? null : tenancy.getRentAmount(),
            tenancy == null || tenancy.getRentFrequency() == null ? null : tenancy.getRentFrequency().name(),
            claim == null ? null : groundsPleaded(claim)
        );
    }

    /** The day the notice was served, whether the claimant recorded a date or a moment. */
    private static LocalDate noticeDate(NoticeOfPossessionEntity notice) {
        if (notice == null) {
            return null;
        }
        if (notice.getNoticeDate() != null) {
            return notice.getNoticeDate();
        }
        return notice.getNoticeDateTime() == null ? null : notice.getNoticeDateTime().toLocalDate();
    }

    private static String groundsPleaded(ClaimEntity claim) {
        String grounds = claim.getClaimGrounds().stream()
            .map(ground -> ClaimGroundSummary.resolveGround(ground.getCategory(), ground.getCode()))
            .filter(Objects::nonNull)
            .map(PossessionGroundEnum::getLabel)
            .distinct()
            .sorted()
            .collect(Collectors.joining(", "));
        return grounds.isEmpty() ? null : grounds;
    }

    private List<MakeOrderStart.Party> parties(ClaimEntity claim, PartyRole role) {
        return claim.getClaimParties().stream()
            .filter(claimParty -> claimParty.getRole() == role)
            .map(claimParty -> new MakeOrderStart.Party(
                claimParty.getId().getPartyId().toString(), partyService.getPartyName(claimParty.getParty())))
            .toList();
    }
}
