package uk.gov.hmcts.reform.pcs.idam;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps Java {@link String} IDAM ids after columns have been widened to {@code text} (Part 2).
 * Still binds UUID values on write so leftover uuid-shaped data and rolling pods stay compatible
 * until Part 3 removes this bridge.
 */
public class IdamUserIdUuidConverter implements UserType<String> {

    @Override
    public int getSqlType() {
        // Match text columns after V033 for schema validation.
        return SqlTypes.VARCHAR;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(String x) {
        return Objects.hashCode(x);
    }

    @Override
    public String nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
        throws SQLException {
        Object value = rs.getObject(position);
        return value == null ? null : value.toString();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
        throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            // UUID bind satisfies uuid columns; PostgreSQL also accepts it for text via cast.
            st.setObject(index, UUID.fromString(value), Types.OTHER);
        }
    }

    @Override
    public String deepCopy(String value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String value) {
        return value;
    }

    @Override
    public String assemble(Serializable cached, Object owner) {
        return (String) cached;
    }
}
