package uk.gov.hmcts.reform.pcs.ccd.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;

import java.util.UUID;

@Entity
@Table(name = "draft_event")
@Setter
@Getter
public class DraftEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long caseReference;

    private UUID userId;

    @Enumerated(value = EnumType.STRING)
    private EventId eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String eventData;

}
