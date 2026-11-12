package com.trampolim.api.observatory;
import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(basePackages="com.trampolim.api.observatory")public class ObservatoryErrors {
 @ExceptionHandler(ResponseStatusException.class)public ResponseEntity<ProblemDetail> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).cacheControl(CacheControl.noStore()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()==null?"Indicador indisponível.":e.getReason()));}
 @ExceptionHandler(org.springframework.dao.DataAccessException.class)public ResponseEntity<ProblemDetail> unavailable(){return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,"Indicadores temporariamente indisponíveis; nenhum valor foi estimado."));}
}
