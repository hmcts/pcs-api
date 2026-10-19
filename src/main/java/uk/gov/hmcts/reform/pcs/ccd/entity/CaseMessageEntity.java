package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageCategory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

/**
 * A message thread on a case between court staff and judges: its subject, what it is about, and what each side
 * wrote, until someone closes it. A message about an application names it, and one sent with an order names that.
 */
@Entity
@Table(name = "case_message")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseMessageEntity {

    public enum Status { OPEN, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    private String subject;

    /** The categories, comma separated. */
    private String categories;

    @Column(name = "gen_app_id")
    private UUID genAppId;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;

    private Instant updatedAt;

    private String closedByName;

    private String closureNote;

    @OneToMany(mappedBy = "message", fetch = LAZY, cascade = ALL)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<CaseMessageEntryEntity> entries = new ArrayList<>();

    public Set<MessageCategory> categorySet() {
        return categories == null || categories.isBlank() ? Set.of()
            : Arrays.stream(categories.split(",")).map(MessageCategory::valueOf).collect(Collectors.toSet());
    }

    public void addEntry(CaseMessageEntryEntity entry) {
        entries.add(entry);
        entry.setMessage(this);
        updatedAt = entry.getCreatedAt();
    }

    public CaseMessageEntryEntity latest() {
        return entries.getLast();
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** Urgent if the latest message says so. */
    public boolean isUrgent() {
        return !entries.isEmpty() && latest().isUrgent();
    }
}
