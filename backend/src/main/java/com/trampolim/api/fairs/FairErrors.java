package com.trampolim.api.fairs;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice(assignableTypes={FairManagementController.class,FairCompanyController.class,FairPublicController.class,FairRegistrationApi.class,FairCountsController.class})
public class FairErrors {
 @ExceptionHandler(ResponseStatusException.class)public ResponseEntity<ProblemDetail> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"Operação indisponível.":e.getReason()));}
 @ExceptionHandler(DataIntegrityViolationException.class)public ResponseEntity<ProblemDetail> constraint(){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Revise os vínculos, consentimentos e limites do evento. Nenhuma entidade de outra organização foi reutilizada."));}
}
