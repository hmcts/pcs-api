package uk.gov.hmcts.reform.pcs.ccd.service.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertionStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAttributeAssertionRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefenceCorrespondenceAddressResolverTest {

    private static final UUID DEFENDANT_ID = UUID.randomUUID();

    @Mock
    private PartyAttributeAssertionRepository assertionRepository;

    private DefenceCorrespondenceAddressResolver underTest;

    private final AddressEntity propertyAddress = AddressEntity.builder().addressLine1("1 Property Street").build();

    @BeforeEach
    void setUp() {
        underTest = new DefenceCorrespondenceAddressResolver(
            assertionRepository, new ObjectMapper(), new AddressMapper(new ModelMapper()));
    }

    @Test
    void shouldUseCorrespondenceAssertionWhenPresent() {
        stubAssertions(jsonAssertion(PartyAttributeType.CORRESPONDENCE_ADDRESS,
            "{\"AddressLine1\":\"9 New Road\",\"PostTown\":\"Leeds\",\"PostCode\":\"LS1 1AA\"}"));
        PartyEntity defendant = defendantWith(AddressEntity.builder().addressLine1("Old Road").build());

        AddressUK result = underTest.resolveCorrespondenceAddress(defendant, propertyAddress);

        assertThat(result.getAddressLine1()).isEqualTo("9 New Road");
        assertThat(result.getPostTown()).isEqualTo("Leeds");
    }

    @Test
    void shouldFallBackToPartyAddressWhenNoAssertion() {
        stubAssertions();
        PartyEntity defendant = defendantWith(AddressEntity.builder().addressLine1("42 Renters Way").build());

        AddressUK result = underTest.resolveCorrespondenceAddress(defendant, propertyAddress);

        assertThat(result.getAddressLine1()).isEqualTo("42 Renters Way");
    }

    @Test
    void shouldFallBackToPropertyWhenNoAssertionAndNoPartyAddress() {
        stubAssertions();
        PartyEntity defendant = defendantWith(null);

        AddressUK result = underTest.resolveCorrespondenceAddress(defendant, propertyAddress);

        assertThat(result.getAddressLine1()).isEqualTo("1 Property Street");
    }

    @Test
    void shouldFallBackWhenAssertionValueIsNotParseableJson() {
        stubAssertions(jsonAssertion(PartyAttributeType.CORRESPONDENCE_ADDRESS, "not-valid-json"));
        PartyEntity defendant = defendantWith(AddressEntity.builder().addressLine1("42 Renters Way").build());

        AddressUK result = underTest.resolveCorrespondenceAddress(defendant, propertyAddress);

        assertThat(result.getAddressLine1()).isEqualTo("42 Renters Way");
    }

    private PartyEntity defendantWith(AddressEntity address) {
        return PartyEntity.builder().id(DEFENDANT_ID).address(address).build();
    }

    private void stubAssertions(PartyAttributeAssertationEntity... assertions) {
        when(assertionRepository.findByPartyIdAndAssertedByAndStatus(
            DEFENDANT_ID, PartyAttributeAssertedBy.DEFENDANT, PartyAttributeAssertionStatus.SUBMITTED))
            .thenReturn(List.of(assertions));
    }

    private static PartyAttributeAssertationEntity jsonAssertion(PartyAttributeType type, String json) {
        return PartyAttributeAssertationEntity.builder().attributesName(type).assertedValue(json).build();
    }
}
