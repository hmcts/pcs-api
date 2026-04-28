package uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "regular_income")
public class RegularIncomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "hc_id")
    @JsonBackReference
    private HouseholdCircumstancesEntity householdCircumstances;

    private String otherIncomeDetails;

    @OneToMany(mappedBy = "regularIncome", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    private List<RegularIncomeItemEntity> items = new ArrayList<>();

    public void addItem(RegularIncomeItemEntity item) {
        items.add(item);
        item.setRegularIncome(this);
    }
}
