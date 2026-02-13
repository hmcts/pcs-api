package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressMapperTest {

    @Mock
    private ModelMapper modelMapper;

    private AddressMapper underTest;

    @BeforeEach
    void setUp() {
        underTest = new AddressMapper(modelMapper);
    }

    @Test
    void shouldMapAddressEntityToAddressUK() {
        // Given
        AddressEntity addressEntity = mock(AddressEntity.class);
        AddressUK expectedAddressUK = mock(AddressUK.class);
        when(modelMapper.map(addressEntity, AddressUK.class)).thenReturn(expectedAddressUK);

        // When
        AddressUK result = underTest.toAddressUK(addressEntity);

        // Then
        assertThat(result).isEqualTo(expectedAddressUK);
    }

    @ParameterizedTest
    @MethodSource("postcodeScenarios")
    void shouldMapAddressUKToEntityAndNormalise(String rawPostcode, String expectedNormalisedPostcode) {
        // Given
        AddressUK addressUK = mock(AddressUK.class);
        AddressEntity mappedAddressEntity = AddressEntity.builder()
            .addressLine1("10 High Street")
            .postTown("London")
            .postcode(rawPostcode)
            .build();

        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(mappedAddressEntity);

        // When
        AddressEntity normalisedAddressEntity = underTest.toAddressEntityAndNormalise(addressUK);

        // Then
        assertThat(normalisedAddressEntity.getPostcode()).isEqualTo(expectedNormalisedPostcode);
    }

    @Test
    void shouldNotNormalisePostcodeThatIsTooShort() {
        // Given
        AddressUK addressUK = mock(AddressUK.class);
        String rawPostcode = "A1   2C";
        AddressEntity mappedAddressEntity = AddressEntity.builder()
            .addressLine1("10 High Street")
            .postTown("London")
            .postcode(rawPostcode)
            .build();

        when(modelMapper.map(addressUK, AddressEntity.class)).thenReturn(mappedAddressEntity);

        // When
        AddressEntity normalisedAddressEntity = underTest.toAddressEntityAndNormalise(addressUK);

        // Then
        assertThat(normalisedAddressEntity.getPostcode()).isEqualTo(rawPostcode);
    }

    private static Stream<Arguments> postcodeScenarios() {
        return Stream.of(
            arguments("B12CD", "B1 2CD"),
            arguments("B1 2CD", "B1 2CD"),
            arguments("B 12 CD", "B1 2CD"),
            arguments("B12   CD", "B1 2CD"),
            arguments("AB12CD", "AB1 2CD"),
            arguments("  AB12CD  ", "AB1 2CD"),
            arguments(" AB1 2CD  ", "AB1 2CD"),
            arguments("AB1 2CD", "AB1 2CD"),
            arguments("AB12 CD", "AB1 2CD"),
            arguments("A B12CD", "AB1 2CD"),
            arguments("a12Cd", "A1 2CD"),
            arguments("W1a 1a A", "W1A 1AA")
        );
    }

}
