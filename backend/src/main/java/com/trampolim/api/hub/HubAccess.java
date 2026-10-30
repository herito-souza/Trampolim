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
 public boolean require(String email,long id,boolean edit){if(email==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);var roles=jdbc.queryForList("SELECT m.role FROM enterprise_organizations o JOIN enterprise_organization_members m ON m.organization_id=o.id JOIN company_users u ON LOWER(u.email)=LOWER(m.email) WHERE o.id=? AND o.organization_type='ASSOCIATION' AND o.status='ACTIVE' AND LOWER(u.email)=LOWER(?) AND u.email_verified=TRUE AND u.account_status='ACTIVE' AND m.role IN ('ASSOCIATION_ADMIN','ASSOCIATION_MEMBER')",String.class,id,email);boolean manager=roles.contains("ASSOCIATION_ADMIN");if(roles.isEmpty()||edit&&!manager)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso não autorizado a esta associação.");return manager;}
 public void additionalWrite(long id){Boolean blocked=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM hub_contracts WHERE organization_id=? AND (status='SUSPENDED' OR status='ACTIVE' AND NOT CURRENT_DATE BETWEEN starts_on AND ends_on))",Boolean.class,id);if(Boolean.TRUE.equals(blocked))throw new ResponseStatusException(HttpStatus.CONFLICT,"O contrato institucional precisa de revisão administrativa para alterar recursos adicionais do Hub. A consulta ao histórico permanece disponível.");}
}
