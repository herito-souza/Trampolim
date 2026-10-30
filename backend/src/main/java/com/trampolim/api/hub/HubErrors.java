package com.trampolim.api.hub;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice(assignableTypes={com.trampolim.api.organization.EnterpriseOrganizationController.class,com.trampolim.api.organization.SaasOrganizationAdminController.class,HubController.class,HubAdminController.class,HubPublicController.class,HubCompanyInvites.class,HubCandidateController.class,com.trampolim.api.education.HubLearningController.class})
public class HubErrors {
 @ExceptionHandler(ResponseStatusException.class)public ResponseEntity<ProblemDetail> status(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(ProblemDetail.forStatusAndDetail(error.getStatusCode(),error.getReason()==null?"Operação indisponível neste ambiente.":error.getReason()));}
 @ExceptionHandler(DataIntegrityViolationException.class)public ResponseEntity<ProblemDetail> constraint(){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Revise o endereço público, código do plano, contrato existente ou limite de utilização. Nenhum registro de outro ambiente pode ser reutilizado."));}
}
