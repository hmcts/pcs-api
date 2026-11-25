package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Read-only reference data for a party's role on the case, referenced by party.entity_role_code_key.
 */
@Entity
@Immutable
@Table(name = "entity_role_code")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityRoleCodeEntity {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "value_en")
    private String valueEn;

    @Column(name = "value_cy")
    private String valueCy;
}
