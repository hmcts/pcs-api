package uk.gov.hmcts.reform.pcs.ccd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.pcs.ccd.domain.order.OrderState;
import uk.gov.hmcts.reform.pcs.ccd.entity.OrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    /** A judge's draft for one of the case's hearings, or with a null hearing, their draft made without one. */
    Optional<OrderEntity> findByPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateAndHearingId(
        long caseReference, String authorIdamUserId, OrderState state, Integer hearingId);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndAuthorIdamUserIdAndStateIn(
        UUID id, long caseReference, String authorIdamUserId, Collection<OrderState> states);

    Optional<OrderEntity> findByIdAndPcsCaseCaseReferenceAndState(UUID id, long caseReference, OrderState state);

    boolean existsByIdAndDocumentIsNotNull(UUID id);

    /** Whether an issued order has granted, dismissed or struck out the application. */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM order_gen_app WHERE gen_app_id = :genAppId "
        + "AND decision IN ('GRANT', 'DISMISS', 'STRIKE_OUT'))", nativeQuery = true)
    boolean isGenAppDecided(UUID genAppId);

    /**
     * The recipients of each generated without-notice order among the documents, by document id, in one query;
     * uploaded documents have no linked order, so none.
     */
    default Map<UUID, List<PartyEntity>> findServedPartiesByDocumentIds(Collection<UUID> documentIds) {
        return documentIds.isEmpty() ? Map.of() : findServedPartyRows(documentIds).stream()
            .collect(Collectors.groupingBy(row -> (UUID) row[0],
                Collectors.mapping(row -> (PartyEntity) row[1], Collectors.toList())));
    }

    @Query("select o.document.id, party from OrderEntity o join o.servedParties party "
        + "where o.document.id in :documentIds")
    List<Object[]> findServedPartyRows(Collection<UUID> documentIds);
}
