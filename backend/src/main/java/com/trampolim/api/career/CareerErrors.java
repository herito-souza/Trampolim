package com.trampolim.api.career;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(basePackages="com.trampolim.api.career") public class CareerErrors {
 @ExceptionHandler(ResponseStatusException.class)public ResponseEntity<ProblemDetail> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"Operação indisponível.":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)public ResponseEntity<ProblemDetail> conflict(){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Registro incompatível ou repetido. Reabra os dados antes de tentar novamente."));}
}
