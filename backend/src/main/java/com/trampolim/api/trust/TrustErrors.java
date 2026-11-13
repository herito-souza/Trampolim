package com.trampolim.api.trust;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(basePackages="com.trampolim.api.trust")public class TrustErrors {
 @ExceptionHandler(ResponseStatusException.class)public ResponseEntity<ProblemDetail> error(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"Operação indisponível.":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)public ResponseEntity<ProblemDetail> conflict(){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"O registro ou sua revisão mudou. Consulte o estado atual."));}
}
