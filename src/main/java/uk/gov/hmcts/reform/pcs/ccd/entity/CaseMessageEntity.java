package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import uk.gov.hmcts.reform.pcs.ccd.domain.message.MessageCategory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

/**
 * A message thread on a case between court staff and judges: its subject, what it is about, and what each side
 * wrote, until someone closes it. A message about an application names it. An order's steps point at the messages
 * sent with it, so a thread knows nothing of orders.
 */
@Entity
@Table(name = "case_message")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "case_id")
    @JsonBackReference
    private PcsCaseEntity pcsCase;

    private String subject;

    @ElementCollection(fetch = LAZY)
    @BatchSize(size = 50)
    @CollectionTable(name = "case_message_category", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "category")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<MessageCategory> categories = EnumSet.noneOf(MessageCategory.class);

    @ManyToOne(fetch = LAZY)
    @JoinTable(name = "case_message_gen_app",
        joinColumns = @JoinColumn(name = "message_id"),
        inverseJoinColumns = @JoinColumn(name = "gen_app_id"))
    private GenAppEntity genApp;

    /** When the thread was closed; it is open until then. */
    private Instant closedAt;

    private String closedByName;

    private String closureNote;

    @OneToMany(mappedBy = "message", fetch = LAZY, cascade = ALL)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<CaseMessageEntryEntity> entries = new ArrayList<>();

    public void addEntry(CaseMessageEntryEntity entry) {
        entries.add(entry);
        entry.setMessage(this);
    }

    public CaseMessageEntryEntity latest() {
        return entries.getLast();
    }

    /** What the message relates to, as the category labels to show. */
    public String categoryLabels() {
        return categories.stream().sorted().map(MessageCategory::getLabel).collect(Collectors.joining(", "));
    }

    public boolean isOpen() {
        return closedAt == null;
    }

    public Instant createdAt() {
        return entries.getFirst().getCreatedAt();
    }

    /** When anyone last wrote in or closed the thread. */
    public Instant updatedAt() {
        return isOpen() ? latest().getCreatedAt() : closedAt;
    }

    /** Urgent if the latest message says so. */
    public boolean isUrgent() {
        return latest().isUrgent();
    }
}
