package com.trampolim.api.radar;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import com.trampolim.api.company.CompanyRadarController;
import com.trampolim.api.education.EducationRadarController;
@RestControllerAdvice(assignableTypes={com.trampolim.api.future.FutureTalentController.class,com.trampolim.api.company.CompanyFutureController.class,RadarController.class,SkillCatalogController.class,CompanyRadarController.class,EducationRadarController.class})
public class RadarErrors {
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ProblemDetail> status(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(ProblemDetail.forStatusAndDetail(error.getStatusCode(),error.getReason()==null?"Não foi possível concluir esta operação.":error.getReason()));}
}
