package uk.gov.hmcts.reform.pcs.ccd.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.Action;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.MakeOrderEnvelope.MakeOrderCaseFacts;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.OrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class MakeOrderService {

    private final OrderRepository orderRepository;
    private final PcsCaseRepository pcsCaseRepository;
    private final ObjectMapper objectMapper;
    private final AddressMapper addressMapper;

    public MakeOrderService(OrderRepository orderRepository,
                            PcsCaseRepository pcsCaseRepository,
                            ObjectMapper objectMapper,
                            AddressMapper addressMapper) {
        this.orderRepository = orderRepository;
        this.pcsCaseRepository = pcsCaseRepository;
        this.objectMapper = objectMapper;
        this.addressMapper = addressMapper;
    }

    @Transactional(readOnly = true)
    public String start(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity caseEntity = findCase(caseReference);
        MakeOrderEnvelope.Order order = orderRepository
            .findFirstByPcsCaseCaseReferenceAndStateOrderByUpdatedAtDesc(caseReference, OrderState.DRAFT)
            .map(this::toOrder)
            .orElseGet(() -> new MakeOrderEnvelope.Order(
                null, OrderState.DRAFT, 0, objectMapper.createObjectNode()));

        return writeJson(new MakeOrderEnvelope(
            null,
            order,
            new MakeOrderEnvelope.MakeOrderCaseContext(
                caseReference,
                addressMapper.toAddressUK(caseEntity.getPropertyAddress()),
                toParties(pcsCase.getAllClaimants()),
                toParties(pcsCase.getAllDefendants()),
                toCaseFacts(pcsCase)
            )
        ));
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

        if (submitted.action() == Action.START_DRAFT) {
            if (orderRepository.findFirstByPcsCaseCaseReferenceAndStateOrderByUpdatedAtDesc(
                caseReference, OrderState.DRAFT).isPresent()) {
                throw new IllegalStateException("An order draft already exists for this case");
            }
            orderRepository.saveAndFlush(OrderEntity.builder()
                .pcsCase(findCase(caseReference))
                .state(OrderState.DRAFT)
                .draftPayload(writeJson(payloadOrEmpty(submitted.order().draftPayload())))
                .build());
            return submitted.action();
        }

        if (submitted.order().id() == null) {
            throw new IllegalArgumentException("The order draft identifier is missing");
        }

        OrderEntity order = orderRepository
            .findByIdAndPcsCaseCaseReference(submitted.order().id(), caseReference)
            .orElseThrow(() -> new IllegalStateException("The order draft does not exist for this case"));

        if (order.getVersion() != submitted.order().version()) {
            throw new IllegalStateException(
                "The order draft has been updated by another user. Reload it and try again");
        }
        if (order.getState() != OrderState.DRAFT) {
            throw new IllegalStateException("Only a draft order can be changed");
        }

        order.setDraftPayload(writeJson(payloadOrEmpty(submitted.order().draftPayload())));
        order.setState(submitted.action() == Action.SAVE_DRAFT
            ? OrderState.DRAFT
            : OrderState.SUBMITTED_FOR_REVIEW);
        orderRepository.saveAndFlush(order);
        return submitted.action();
    }

    private PcsCaseEntity findCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    private MakeOrderEnvelope.Order toOrder(OrderEntity order) {
        return new MakeOrderEnvelope.Order(
            order.getId(), order.getState(), order.getVersion(), readJson(order.getDraftPayload()));
    }

    private JsonNode payloadOrEmpty(JsonNode payload) {
        return payload == null ? objectMapper.createObjectNode() : payload;
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

    private java.time.LocalDate noticeDate(NoticeServedDetails notice) {
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

    private JsonNode readJson(String payload) {
        try {
            return payload == null || payload.isBlank()
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The stored order draft payload is not valid JSON", exception);
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
