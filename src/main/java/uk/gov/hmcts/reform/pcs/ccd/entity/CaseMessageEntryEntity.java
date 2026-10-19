package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageRecipient;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

/** What one person wrote in a message thread: the first message or a reply, and whom it was for. */
@Entity
@Table(name = "case_message_entry")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseMessageEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "message_id")
    @JsonBackReference
    private CaseMessageEntity message;

    private Instant createdAt;

    private String senderName;

    @Enumerated(EnumType.STRING)
    private MessageRecipient senderRole;

    @Enumerated(EnumType.STRING)
    private MessageRecipient recipientRole;

    private boolean urgent;

    private String body;
}
