package com.trampolim.api.education;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes={AdaptivePassportController.class,PassportVerificationController.class,com.trampolim.api.company.CompanyPassportController.class,com.trampolim.api.radar.RadarInvalidationController.class})
public class AdaptivePassportErrors {
 @ExceptionHandler(ResponseStatusException.class)public ResponseEntity<ProblemDetail> status(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(ProblemDetail.forStatusAndDetail(error.getStatusCode(),error.getReason()==null?"Operação indisponível.":error.getReason()));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)public ResponseEntity<ProblemDetail> integrity(){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Os registros mudaram ou o contexto não é permitido. Atualize a página antes de tentar novamente."));}
}
