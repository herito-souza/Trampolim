package com.trampolim.api.query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
/** Additive opt-in pagination. Legacy callers keep their response shape and full list. */
public record ListWindow(Integer limit,int page) {
 public ListWindow { if((limit==null&&page!=0)||(limit!=null&&(limit<1||limit>200))||page<0||page>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use limit entre 1 e 200 e page entre 0 e 1000. Informe limit ao escolher uma página."); }
 public Pageable pageable(){return limit==null?Pageable.unpaged():PageRequest.of(page,limit);}
 public String sql(){return limit==null?"":" LIMIT "+limit+" OFFSET "+((long)page*limit);}
}
