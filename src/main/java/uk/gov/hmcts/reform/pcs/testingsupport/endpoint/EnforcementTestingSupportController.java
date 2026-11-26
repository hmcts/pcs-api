package uk.gov.hmcts.reform.pcs.testingsupport.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.enforcementorder.EnforcementOrderMediator;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

/**
 * Temporary end point to allow testing as it moves a case's active warrant into awaiting scheduling
 * so the schedule eviction event can be tested.
 *
 * <p>TODO HDPI-7344: remove once the warrant journey moves a paid warrant into AWAITING_SCHEDULING.
 */
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class EnforcementTestingSupportController {

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderMediator enforcementOrderMediator;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public EnforcementTestingSupportController(PcsCaseService pcsCaseService,
                                               EnforcementOrderMediator enforcementOrderMediator,
                                               EnforcementOrderRepository enforcementOrderRepository) {
        this.pcsCaseService = pcsCaseService;
        this.enforcementOrderMediator = enforcementOrderMediator;
        this.enforcementOrderRepository = enforcementOrderRepository;
    }

    @Operation(summary = "Move the case's active paid warrant (CASE_PROGRESSION) to AWAITING_SCHEDULING")
    @PostMapping("/case/{caseReference}/warrant/awaiting-scheduling")
    @Transactional
    public ResponseEntity<String> moveWarrantToAwaitingScheduling(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference
    ) {
        return enforcementOrderMediator.getEnforcementOrder(pcsCaseService.loadCase(caseReference))
            .filter(application -> application.getState() == EnforcementState.CASE_PROGRESSION)
            .filter(application -> application.getWarrantDetails() != null)
            .map(application -> {
                application.setState(EnforcementState.AWAITING_SCHEDULING);
                enforcementOrderRepository.save(application);
                return ResponseEntity.ok(application.getId().toString());
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
