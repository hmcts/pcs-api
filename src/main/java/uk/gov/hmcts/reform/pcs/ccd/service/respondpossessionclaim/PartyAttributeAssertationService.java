package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertionStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAttributeAssertionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PartyAttributeAssertationService {

    private final PartyAttributeAssertionRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public PartyAttributeAssertationService(PartyAttributeAssertionRepository repository,
                                            ObjectMapper objectMapper,
                                            @Qualifier("utcClock") Clock utcClock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.utcClock = utcClock;
    }

    public void buildPartyAttributeEntities(PossessionClaimResponse response, PartyEntity partyEntity) {
        DefendantContactDetails defendantContact = response.getDefendantContactDetails();
        DefendantResponses responses = response.getDefendantResponses();

        List<PartyAttributeAssertationEntity> assertions = new ArrayList<>();

        addNameAssertion(defendantContact, responses, partyEntity, assertions);
        addAddressAssertion(defendantContact, responses, partyEntity, assertions);
        addTenancyTypeAssertion(responses, partyEntity, assertions);
        addTenancyStartDateAssertion(responses, partyEntity, assertions);
        addPossessionNoticeAssertion(responses, partyEntity, assertions);
        addNoticeDateAssertion(responses, partyEntity, assertions);
        addRentArrearsAssertion(responses, partyEntity, assertions);

        repository.saveAll(assertions);
    }

    /**
     * Adds a name assertion when disputed or not provided by the claimant.
     * Condition: defendantNameConfirmation is NO (disputed) or null (claimant did not provide a name).
     */
    private void addNameAssertion(DefendantContactDetails defendantContact, DefendantResponses responses,
                                  PartyEntity partyEntity, List<PartyAttributeAssertationEntity> assertions) {
        if (defendantContact == null || defendantContact.getParty() == null) {
            return;
        }
        VerticalYesNo nameConfirmation = responses.getDefendantNameConfirmation();
        if (nameConfirmation == null || nameConfirmation == VerticalYesNo.NO) {
            Party defendantParty = defendantContact.getParty();
            Map<String, String> nameMap = toNameMap(defendantParty.getFirstName(), defendantParty.getLastName());
            if (!nameMap.isEmpty()) {
                addJsonAssertion(PartyAttributeType.DEFENDANT_NAME, nameMap, partyEntity, assertions);
            }
        }
    }

    /**
     * Adds a correspondence address assertion when disputed or not provided by the claimant.
     * Condition: correspondenceAddressConfirmation is NO (disputed) or null (claimant did not provide an address).
     */
    private void addAddressAssertion(DefendantContactDetails defendantContact, DefendantResponses responses,
                                     PartyEntity partyEntity, List<PartyAttributeAssertationEntity> assertions) {
        if (defendantContact == null || defendantContact.getParty() == null) {
            return;
        }
        VerticalYesNo addressConfirmation = responses.getCorrespondenceAddressConfirmation();
        if (addressConfirmation == null || addressConfirmation == VerticalYesNo.NO) {
            AddressUK address = defendantContact.getParty().getAddress();
            if (address != null) {
                addJsonAssertion(PartyAttributeType.CORRESPONDENCE_ADDRESS, address, partyEntity, assertions);
            }
        }
    }

    /**
     * Adds a tenancy type assertion when disputed by the defendant.
     * Condition: tenancyTypeConfirmation is NO and the defendant has supplied an alternative tenancy type.
     */
    private void addTenancyTypeAssertion(DefendantResponses responses,
                                         PartyEntity partyEntity, List<PartyAttributeAssertationEntity> assertions) {
        if (responses.getTenancyTypeConfirmation() == YesNoNotSure.NO && responses.getTenancyType() != null) {
            assertions.add(buildAssertion(PartyAttributeType.TENANCY_TYPE,
                responses.getTenancyType(), partyEntity));
        }
    }

    /**
     * Adds a tenancy start date assertion when disputed or not provided by the claimant.
     * Condition: a date is present AND tenancyStartDateConfirmation is NO (disputed) or null (not provided).
     */
    private void addTenancyStartDateAssertion(DefendantResponses responses,
                                              PartyEntity partyEntity,
                                              List<PartyAttributeAssertationEntity> assertions) {
        YesNoNotSure tenancyStartDateConfirmation = responses.getTenancyStartDateConfirmation();
        if (responses.getTenancyStartDate() != null
            && (tenancyStartDateConfirmation == null || tenancyStartDateConfirmation == YesNoNotSure.NO)) {
            assertions.add(buildAssertion(PartyAttributeType.TENANCY_START_DATE,
                responses.getTenancyStartDate().toString(), partyEntity));
        }
    }

    /**
     * Adds a possession notice assertion when the defendant disputes receiving it.
     * Condition: possessionNoticeReceived is NO.
     */
    private void addPossessionNoticeAssertion(DefendantResponses responses,
                                              PartyEntity partyEntity,
                                              List<PartyAttributeAssertationEntity> assertions) {
        if (responses.getPossessionNoticeReceived() == YesNoNotSure.NO) {
            assertions.add(buildAssertion(PartyAttributeType.POSSESSION_NOTICE_RECEIVED,
                responses.getPossessionNoticeReceived().name(), partyEntity));
        }
    }

    /**
     * Adds a notice received date assertion when the defendant confirms receiving the notice and provides a date.
     * Condition: possessionNoticeReceived is YES and a date is present.
     */
    private void addNoticeDateAssertion(DefendantResponses responses,
                                        PartyEntity partyEntity, List<PartyAttributeAssertationEntity> assertions) {
        if (responses.getPossessionNoticeReceived() == YesNoNotSure.YES
            && responses.getNoticeReceivedDate() != null) {
            assertions.add(buildAssertion(PartyAttributeType.NOTICE_RECEIVED_DATE,
                responses.getNoticeReceivedDate().toString(), partyEntity));
        }
    }

    /**
     * Adds a rent arrears amount assertion when disputed by the defendant.
     * Condition: rentArrearsAmountConfirmation is NO and the defendant has supplied an amount.
     */
    private void addRentArrearsAssertion(DefendantResponses responses,
                                         PartyEntity partyEntity, List<PartyAttributeAssertationEntity> assertions) {
        if (responses.getRentArrearsAmount() != null
            && responses.getRentArrearsAmountConfirmation() == YesNoNotSure.NO) {
            assertions.add(buildAssertion(PartyAttributeType.RENT_ARREARS_AMOUNT,
                responses.getRentArrearsAmount().toPlainString(), partyEntity));
        }
    }

    private Map<String, String> toNameMap(String firstName, String lastName) {
        Map<String, String> map = new LinkedHashMap<>();
        if (firstName != null) {
            map.put("firstName", firstName);
        }
        if (lastName != null) {
            map.put("lastName", lastName);
        }
        return map;
    }

    private void addJsonAssertion(PartyAttributeType attributeType, Object value,
                                 PartyEntity partyEntity, List<PartyAttributeAssertationEntity> assertions) {
        try {
            assertions.add(buildAssertion(attributeType, objectMapper.writeValueAsString(value), partyEntity));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise assertion value for attribute: {}", attributeType, e);
        }
    }

    private PartyAttributeAssertationEntity buildAssertion(PartyAttributeType attributeType, String value,
                                                           PartyEntity partyEntity) {
        LocalDateTime now = LocalDateTime.now(utcClock);
        return PartyAttributeAssertationEntity.builder()
            .party(partyEntity)
            .attributesName(attributeType)
            .assertedValue(value)
            .assertedBy(PartyAttributeAssertedBy.DEFENDANT)
            .status(PartyAttributeAssertionStatus.SUBMITTED)
            .createdAt(now)
            .createdBy(partyEntity)
            .lastUpdatedBy(partyEntity)
            .lastUpdatedAt(now)
            .build();
    }
}