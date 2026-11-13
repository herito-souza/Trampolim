package com.trampolim.api.privacy;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes={PrivacyController.class,PrivacyAdminController.class,com.trampolim.api.user.ProfileController.class,com.trampolim.api.education.StudentAccountController.class,com.trampolim.api.company.CompanyAccountDeletionController.class})
public class PrivacyErrors {
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
 public ResponseEntity<ProblemDetail> conflict(org.springframework.dao.DataIntegrityViolationException error){boolean privacy=false;for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof java.sql.SQLException sql&&"23514".equals(sql.getSQLState())&&(ObjectsSafe.message(sql).contains("privacy_account_review")||ObjectsSafe.message(sql).contains("Account deletion requires privacy review")))privacy=true;return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,privacy?"Sua solicitação está em análise ou retenção. Consulte o protocolo de privacidade antes de excluir a conta.":"A conta possui registros relacionados. Solicite análise antes de excluir."));}
 @ExceptionHandler(ResponseStatusException.class)public ResponseEntity<ProblemDetail> response(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(ProblemDetail.forStatusAndDetail(error.getStatusCode(),error.getReason()==null?"Não foi possível concluir a operação.":error.getReason()));}
 private static class ObjectsSafe {static String message(Throwable error){return error.getMessage()==null?"":error.getMessage();}}
}
