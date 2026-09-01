package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.MakeOrderCaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderDraftPayload;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderDraftPayload.OrderType;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class MakeOrderService {

    private final OrderRepository orderRepository;
    private final HearingRepository hearingRepository;
    private final ObjectMapper objectMapper;
    private final AddressMapper addressMapper;
    private final Clock ukClock;

    public MakeOrderService(OrderRepository orderRepository,
                            HearingRepository hearingRepository,
                            ObjectMapper objectMapper,
                            AddressMapper addressMapper,
                            @Qualifier("ukClock") Clock ukClock) {
        this.orderRepository = orderRepository;
        this.hearingRepository = hearingRepository;
        this.objectMapper = objectMapper;
        this.addressMapper = addressMapper;
        this.ukClock = ukClock;
    }

    @Transactional(readOnly = true)
    public String start(long caseReference, PCSCase pcsCase) {
        HearingEntity hearing = findTodaysHearing(caseReference);
        OrderEntity order = orderRepository
            .findFirstByPcsCase_IdAndHearing_IdAndStateOrderByCreatedAtDesc(
                hearing.getPcsCase().getId(), hearing.getId(), OrderState.DRAFT)
            .orElseGet(() -> OrderEntity.builder()
                .pcsCase(hearing.getPcsCase())
                .hearing(hearing)
                .state(OrderState.DRAFT)
                .draftPayload(writeJson(emptyDraftPayload()))
                .build());

        return writeJson(toEnvelope(caseReference, pcsCase, order));
    }

    @Transactional
    public Action submit(long caseReference, String payload) {
        MakeOrderEnvelope submitted = readEnvelope(payload);
        if (submitted.action() == null) {
            throw new IllegalArgumentException("The order action is missing");
        }
        if (submitted.order() == null) {
            throw new IllegalArgumentException("The order is missing");
        }

        HearingEntity hearing = findTodaysHearing(caseReference);
        if (submitted.action() == Action.START_DRAFT) {
            startDraft(hearing);
            return submitted.action();
        }
        if (submitted.order().id() == null) {
            throw new IllegalArgumentException("The order draft identifier is missing");
        }

        OrderEntity order = existingOrder(submitted, hearing);

        MakeOrderDraftPayload draftPayload = validateDraftPayload(
            submitted.order().draftPayload(), submitted.action());

        order.setDraftPayload(writeJson(draftPayload));
        OrderState nextState = switch (submitted.action()) {
            case SAVE_DRAFT -> OrderState.DRAFT;
            case SUBMIT_FOR_REVIEW -> OrderState.SUBMITTED_FOR_REVIEW;
            case START_DRAFT -> throw new IllegalStateException("The start action was not handled");
        };
        order.setState(nextState);
        orderRepository.saveAndFlush(order);
        return submitted.action();
    }

    private void startDraft(HearingEntity hearing) {
        if (orderRepository.findFirstByPcsCase_IdAndHearing_IdAndStateOrderByCreatedAtDesc(
            hearing.getPcsCase().getId(), hearing.getId(), OrderState.DRAFT).isPresent()) {
            throw new IllegalStateException("An order draft already exists for today's hearing");
        }
        orderRepository.saveAndFlush(newOrder(hearing));
    }

    private OrderEntity newOrder(HearingEntity hearing) {
        return OrderEntity.builder()
            .pcsCase(hearing.getPcsCase())
            .hearing(hearing)
            .state(OrderState.DRAFT)
            .draftPayload(writeJson(emptyDraftPayload()))
            .build();
    }

    private OrderEntity existingOrder(MakeOrderEnvelope submitted, HearingEntity hearing) {
        OrderEntity order = orderRepository.findById(submitted.order().id())
            .orElseThrow(() -> new IllegalStateException("The order draft does not exist"));

        if (!order.getPcsCase().getId().equals(hearing.getPcsCase().getId())
            || order.getHearing() == null
            || !order.getHearing().getId().equals(hearing.getId())) {
            throw new IllegalStateException("The order draft does not belong to today's hearing");
        }
        if (order.getVersion() != submitted.order().version()) {
            throw new IllegalStateException(
                "The order draft has been updated by another user. Reload it and try again");
        }
        if (order.getState() != OrderState.DRAFT) {
            throw new IllegalStateException("Only a draft order can be changed");
        }
        return order;
    }

    private HearingEntity findTodaysHearing(long caseReference) {
        LocalDate today = LocalDate.now(ukClock);
        List<HearingEntity> hearings = hearingRepository.findActiveHearingsBetween(
            caseReference, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        if (hearings.isEmpty()) {
            throw new IllegalStateException("No hearing is listed for this case today");
        }
        if (hearings.size() > 1) {
            throw new IllegalStateException("More than one hearing is listed for this case today");
        }
        return hearings.getFirst();
    }

    private MakeOrderEnvelope toEnvelope(long caseReference, PCSCase pcsCase, OrderEntity order) {
        return new MakeOrderEnvelope(
            null,
            new MakeOrderEnvelope.Order(
                order.getId(), order.getState(), order.getVersion(), readDraftPayload(order.getDraftPayload())),
            new MakeOrderEnvelope.MakeOrderCaseContext(
                caseReference,
                addressMapper.toAddressUK(order.getPcsCase().getPropertyAddress()),
                toParties(pcsCase.getAllClaimants()),
                toParties(pcsCase.getAllDefendants()),
                toCaseFacts(pcsCase)
            )
        );
    }

    private MakeOrderCaseFacts toCaseFacts(PCSCase pcsCase) {
        TenancyLicenceDetails tenancy = pcsCase.getTenancyLicenceDetails();
        OccupationLicenceDetailsWales occupation = pcsCase.getOccupationLicenceDetailsWales();
        RentDetails rent = pcsCase.getRentDetails();
        RentArrearsSection arrears = pcsCase.getRentArrears();

        return new MakeOrderCaseFacts(
            tenancy != null && tenancy.getTenancyLicenceDate() != null
                ? tenancy.getTenancyLicenceDate()
                : occupation == null ? null : occupation.getLicenceStartDate(),
            tenancy != null && tenancy.getTypeOfTenancyLicence() != null
                ? tenancy.getTypeOfTenancyLicence().name()
                : occupation == null || occupation.getOccupationLicenceTypeWales() == null
                    ? null : occupation.getOccupationLicenceTypeWales().name(),
            noticeDate(pcsCase.getNoticeServedDetails()),
            rent == null ? null : rent.getCurrentRent(),
            rent == null || rent.getFrequency() == null ? null : rent.getFrequency().name(),
            groundsPleaded(pcsCase),
            arrears == null ? null : arrears.getTotal()
        );
    }

    private LocalDate noticeDate(NoticeServedDetails notice) {
        if (notice == null || notice.getServiceMethod() == null) {
            return null;
        }
        return switch (notice.getServiceMethod()) {
            case FIRST_CLASS_POST -> notice.getPostedDate();
            case DELIVERED_PERMITTED_PLACE -> notice.getDeliveredDate();
            case PERSONALLY_HANDED -> notice.getHandedOverDateTime() == null
                ? null : notice.getHandedOverDateTime().toLocalDate();
            case EMAIL -> notice.getEmailSentDateTime() == null
                ? null : notice.getEmailSentDateTime().toLocalDate();
            case OTHER_ELECTRONIC -> notice.getOtherElectronicDateTime() == null
                ? null : notice.getOtherElectronicDateTime().toLocalDate();
            case OTHER -> notice.getOtherDateTime() == null ? null : notice.getOtherDateTime().toLocalDate();
        };
    }

    private String groundsPleaded(PCSCase pcsCase) {
        if (pcsCase.getClaimGroundSummaries() == null) {
            return null;
        }
        String grounds = pcsCase.getClaimGroundSummaries().stream()
            .map(ListValue::getValue)
            .filter(Objects::nonNull)
            .map(summary -> summary.getLabel())
            .filter(Objects::nonNull)
            .filter(label -> !label.isBlank())
            .distinct()
            .collect(java.util.stream.Collectors.joining(", "));
        return grounds.isEmpty() ? null : grounds;
    }

    private List<MakeOrderEnvelope.Party> toParties(List<ListValue<Party>> parties) {
        if (parties == null) {
            return List.of();
        }
        return parties.stream()
            .map(value -> new MakeOrderEnvelope.Party(value.getId(), displayName(value.getValue())))
            .toList();
    }

    private String displayName(Party party) {
        if (party == null) {
            return "";
        }
        if (party.getOrgName() != null && !party.getOrgName().isBlank()) {
            return party.getOrgName();
        }
        return String.join(" ", Stream.of(party.getFirstName(), party.getLastName())
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .toList());
    }

    private MakeOrderEnvelope readEnvelope(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("The order draft payload is missing");
        }
        try {
            JsonNode envelope = objectMapper.readTree(payload);
            if (envelope.isTextual()) {
                envelope = objectMapper.readTree(envelope.textValue());
            }
            return objectMapper.treeToValue(envelope, MakeOrderEnvelope.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("The order draft payload is not valid JSON", exception);
        }
    }

    private MakeOrderDraftPayload readDraftPayload(String payload) {
        try {
            if (payload == null || payload.isBlank()) {
                return emptyDraftPayload();
            }
            return objectMapper.readValue(payload, MakeOrderDraftPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The stored order draft payload is not valid JSON", exception);
        }
    }

    private MakeOrderDraftPayload emptyDraftPayload() {
        return new MakeOrderDraftPayload(
            1, OrderType.OUTRIGHT_POSSESSION, objectMapper.createObjectNode(), Map.of());
    }

    private MakeOrderDraftPayload validateDraftPayload(MakeOrderDraftPayload draftPayload, Action action) {
        if (draftPayload == null) {
            throw new IllegalArgumentException("The order draft payload is missing");
        }
        if (draftPayload.version() != 1) {
            throw new IllegalArgumentException("The order draft payload version is not supported");
        }
        if (draftPayload.orderType() == null) {
            throw new IllegalArgumentException("The order type is missing");
        }
        if (draftPayload.formData() == null || !draftPayload.formData().isObject()) {
            throw new IllegalArgumentException("The order draft form data must be a JSON object");
        }
        if (draftPayload.documents() == null
            || draftPayload.documents().values().stream()
                .anyMatch(document -> document == null || !document.isObject())) {
            throw new IllegalArgumentException("The order documents must be JSON objects");
        }
        if (action == Action.SUBMIT_FOR_REVIEW) {
            validateSubmissionDocument(draftPayload);
        }
        return draftPayload;
    }

    private void validateSubmissionDocument(MakeOrderDraftPayload draftPayload) {
        if (draftPayload.orderType() != OrderType.OUTRIGHT_POSSESSION) {
            throw new IllegalArgumentException("Only outright possession orders can be submitted for review");
        }
        JsonNode document = draftPayload.documents().get(OrderType.OUTRIGHT_POSSESSION);
        if (document == null
            || !"docweave-document".equals(document.path("schema").asText())
            || document.path("version").asInt() != 1
            || !document.path("current").isObject()
            || !document.path("generated").isObject()) {
            throw new IllegalArgumentException("The outright possession order document is invalid");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The order draft payload could not be encoded", exception);
        }
    }
}
