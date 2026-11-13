package com.trampolim.api.hub;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
@ConditionalOnProperty(name="trampolim.datasource.empresas.enabled",havingValue="true")
public class HubAccess {
 private final JdbcTemplate jdbc;
 public HubAccess(@Qualifier("empresasJdbcTemplate")JdbcTemplate jdbc){this.jdbc=jdbc;}
 public boolean require(String email,long id,boolean edit){
  if(email==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  var access=jdbc.query("""
   SELECT o.organization_type,m.role FROM enterprise_organizations o
   JOIN enterprise_organization_members m ON m.organization_id=o.id
   JOIN company_users u ON LOWER(u.email)=LOWER(m.email)
   WHERE o.id=? AND o.status='ACTIVE'
     AND (o.organization_type='ASSOCIATION' OR (o.organization_type='EDUCATIONAL' AND o.verification_status='VERIFIED'))
     AND LOWER(u.email)=LOWER(?) AND u.email_verified=TRUE AND u.account_status='ACTIVE'
     AND ((o.organization_type='ASSOCIATION' AND m.role IN ('ASSOCIATION_ADMIN','ASSOCIATION_MEMBER'))
       OR (o.organization_type='EDUCATIONAL' AND m.role IN ('EDUCATION_ADMIN','CONTENT_EDITOR','PARTNER_MANAGER')))
   """,(rs,n)->new String[]{rs.getString(1),rs.getString(2)},id,email);
  boolean manager=access.stream().anyMatch(row->row[1].equals(row[0].equals("ASSOCIATION")?"ASSOCIATION_ADMIN":"EDUCATION_ADMIN"));
  boolean editor=manager||access.stream().anyMatch(row->row[1].equals("CONTENT_EDITOR"));
  if(access.isEmpty()||edit&&!editor)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso não autorizado a esta organização.");
  return manager;
 }
 public void requireAdmin(String email,long id){if(!require(email,id,true))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Somente a administração da organização pode realizar esta ação.");}
 public boolean canEdit(String email,long id){if(email==null)return false;return Boolean.TRUE.equals(jdbc.queryForObject("""
  SELECT EXISTS(SELECT 1 FROM enterprise_organizations o JOIN enterprise_organization_members m ON m.organization_id=o.id JOIN company_users u ON LOWER(u.email)=LOWER(m.email)
   WHERE o.id=? AND o.status='ACTIVE' AND (o.organization_type='ASSOCIATION' OR (o.organization_type='EDUCATIONAL' AND o.verification_status='VERIFIED'))
    AND LOWER(u.email)=LOWER(?) AND u.email_verified=TRUE AND u.account_status='ACTIVE'
    AND m.role IN ('ASSOCIATION_ADMIN','EDUCATION_ADMIN','CONTENT_EDITOR'))
  """,Boolean.class,id,email));}
 public void additionalWrite(long id){Boolean blocked=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM hub_contracts WHERE organization_id=? AND (status='SUSPENDED' OR status='ACTIVE' AND NOT CURRENT_DATE BETWEEN starts_on AND ends_on))",Boolean.class,id);if(Boolean.TRUE.equals(blocked))throw new ResponseStatusException(HttpStatus.CONFLICT,"O contrato institucional precisa de revisão administrativa para alterar recursos adicionais do Hub. A consulta ao histórico permanece disponível.");}
}
