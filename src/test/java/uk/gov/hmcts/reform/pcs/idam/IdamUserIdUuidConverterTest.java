package uk.gov.hmcts.reform.pcs.idam;

import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdamUserIdUuidConverter")
class IdamUserIdUuidConverterTest {

    private static final String IDAM_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private final IdamUserIdUuidConverter converter = new IdamUserIdUuidConverter();

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Test
    void reportsUuidSqlTypeAndStringReturnedClass() {
        assertThat(converter.getSqlType()).isEqualTo(SqlTypes.UUID);
        assertThat(converter.returnedClass()).isEqualTo(String.class);
        assertThat(converter.isMutable()).isFalse();
    }

    @Test
    void equalsAndHashCodeDelegateToObjects() {
        assertThat(converter.equals(IDAM_ID, IDAM_ID)).isTrue();
        assertThat(converter.equals(IDAM_ID, "other")).isFalse();
        assertThat(converter.equals(null, null)).isTrue();
        assertThat(converter.hashCode(IDAM_ID)).isEqualTo(IDAM_ID.hashCode());
        assertThat(converter.hashCode(null)).isZero();
    }

    @Test
    void nullSafeGetReturnsNullWhenColumnIsNull() throws Exception {
        when(resultSet.getObject(1)).thenReturn(null);

        assertThat(converter.nullSafeGet(resultSet, 1, null, null)).isNull();
    }

    @Test
    void nullSafeGetStringifiesUuidAndTextValues() throws Exception {
        when(resultSet.getObject(1)).thenReturn(UUID.fromString(IDAM_ID));
        assertThat(converter.nullSafeGet(resultSet, 1, null, null)).isEqualTo(IDAM_ID);

        when(resultSet.getObject(2)).thenReturn(IDAM_ID);
        assertThat(converter.nullSafeGet(resultSet, 2, null, null)).isEqualTo(IDAM_ID);
    }

    @Test
    void nullSafeSetBindsNullAsOther() throws Exception {
        converter.nullSafeSet(preparedStatement, null, 1, null);

        verify(preparedStatement).setNull(1, Types.OTHER);
    }

    @Test
    void nullSafeSetBindsUuidValue() throws Exception {
        converter.nullSafeSet(preparedStatement, IDAM_ID, 1, null);

        verify(preparedStatement).setObject(1, UUID.fromString(IDAM_ID), Types.OTHER);
    }

    @Test
    void deepCopyDisassembleAndAssembleRoundTrip() {
        assertThat(converter.deepCopy(IDAM_ID)).isEqualTo(IDAM_ID);
        assertThat(converter.deepCopy(null)).isNull();

        assertThat(converter.disassemble(IDAM_ID)).isEqualTo(IDAM_ID);
        assertThat(converter.assemble(IDAM_ID, null)).isEqualTo(IDAM_ID);
    }
}
