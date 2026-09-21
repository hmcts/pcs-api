package uk.gov.hmcts.reform.pcs.ccd.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "user_name")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private UUID idamId;

    @OneToMany(mappedBy = "user", fetch = LAZY, cascade = ALL)
    @Builder.Default
    @JsonManagedReference
    private List<JudicialNoteEntity> judicialNotes = new ArrayList<>();

    public void addJudicialNote(JudicialNoteEntity judicialNote) {
        judicialNotes.add(judicialNote);
        judicialNote.setUser(this);
    }

}
