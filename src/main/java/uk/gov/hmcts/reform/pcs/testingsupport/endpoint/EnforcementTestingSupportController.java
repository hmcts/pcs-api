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
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.Comparator;

/**
 * Temporary end point to allow testing as it moves a case's latest warrant into awaiting scheduling
 * so the schedule eviction event can be tested.
 *
 */
@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(name = "testing-support.enabled", havingValue = "true")
@Tag(name = "Testing Support")
public class EnforcementTestingSupportController {

    private final PcsCaseService pcsCaseService;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public EnforcementTestingSupportController(PcsCaseService pcsCaseService,
                                               EnforcementOrderRepository enforcementOrderRepository) {
        this.pcsCaseService = pcsCaseService;
        this.enforcementOrderRepository = enforcementOrderRepository;
    }

    @Operation(summary = "Move the case's latest paid warrant (CASE_PROGRESSION) to AWAITING_SCHEDULING")
    @PostMapping("/case/{caseReference}/warrant/awaiting-scheduling")
    @Transactional
    public ResponseEntity<String> moveWarrantToAwaitingScheduling(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @PathVariable long caseReference
    ) {
        return pcsCaseService.loadCase(caseReference).getEnforcementOrders().stream()
            .filter(application -> application.getState() == EnforcementState.CASE_PROGRESSION)
            .filter(application -> application.getWarrantDetails() != null)
            .max(Comparator.comparing(application -> application.getWarrantDetails().getCreated()))
            .map(application -> {
                application.setState(EnforcementState.AWAITING_SCHEDULING);
                enforcementOrderRepository.save(application);
                return ResponseEntity.ok(application.getId().toString());
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
