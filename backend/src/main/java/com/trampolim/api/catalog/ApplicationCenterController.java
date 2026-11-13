package com.trampolim.api.catalog;

import com.trampolim.api.radar.RadarBridge;
import com.trampolim.api.user.AppUserRepository;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Candidate-facing lookup; only a verified Oportunidades identity can make the server-to-server request. */
@RestController
@RequestMapping("/api/profile/applications/center")
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class ApplicationCenterController {
    private final AppUserRepository users;private final RadarBridge bridge;
    public ApplicationCenterController(AppUserRepository users,RadarBridge bridge){this.users=users;this.bridge=bridge;}
    public record Filter(String status,String company,String job,String from,String to,int page,int size,boolean confirmedCompanyLookup){}
    @PostMapping public Object list(@AuthenticationPrincipal String email,@RequestBody Filter filter){
        var user=users.findByEmail(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(!user.isEmailVerified()||!"PERSON".equals(user.getAccountType()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Confirme o e-mail da sua conta de Oportunidades para consultar candidaturas enviadas.");
        if(filter==null||!filter.confirmedCompanyLookup())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirme a consulta com o e-mail verificado desta conta.");
        bridge.configured();
        var request=new LinkedHashMap<String,Object>();request.put("verifiedEmail",user.getEmail());request.put("status",filter.status());request.put("company",filter.company());request.put("job",filter.job());request.put("from",filter.from());request.put("to",filter.to());request.put("page",filter.page());request.put("size",filter.size());
        return bridge.post("/integrations/application-center/query",request);
    }
}
