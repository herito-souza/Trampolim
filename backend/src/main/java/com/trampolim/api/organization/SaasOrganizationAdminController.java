package com.trampolim.api.organization;

import com.trampolim.api.security.PlatformAccess;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Platform-admin API: authorization stays in Oportunidades, organization data lives in Empresas. */
@RestController
@RequestMapping("/api/admin/saas/organizations")
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class SaasOrganizationAdminController {
    private final JdbcTemplate jdbc;
    private final PlatformAccess platformAccess;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;

    public SaasOrganizationAdminController(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc, PlatformAccess platformAccess) {
        this.jdbc=jdbc;
        this.platformAccess=platformAccess;
    }

    public record OrganizationView(long id,String name,String type,String status,Long legacyCompanyId,long members,long linkedCompanies,String createdAt) {}
    public record OrganizationInput(String name,String type) {}
    public record StatusInput(String status) {}
    public record CompanyLinkInput(long companyId,String status) {}
    public record MemberInput(String email,String role) {}
    public record LinkedCompany(long companyId,String name,String status) {}
    public record InviteView(String email,String role,String expiresAt,String inviteUrl) {}

    @GetMapping
    public List<OrganizationView> list(@AuthenticationPrincipal String email) {
        requireSuperAdmin(email);
        List<OrganizationView> institutions=jdbc.query("""
                SELECT o.id,o.name,o.organization_type,o.status,NULL::BIGINT,
                    (SELECT COUNT(*) FROM enterprise_organization_members m WHERE m.organization_id=o.id),
                    (SELECT COUNT(*) FROM enterprise_organization_companies r WHERE r.organization_id=o.id AND r.status='ACTIVE'),o.created_at
                FROM enterprise_organizations o
                """,(rs,row)->new OrganizationView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),null,rs.getLong(6),rs.getLong(7),rs.getTimestamp(8).toInstant().toString()));
        List<OrganizationView> companies=jdbc.query("""
                SELECT -c.id,c.trade_name,'COMPANY',c.verification_status,c.id,
                    (SELECT COUNT(*) FROM company_members m WHERE m.company_id=c.id),
                    (SELECT COUNT(*) FROM enterprise_organization_companies r WHERE r.company_id=c.id AND r.status='ACTIVE'),c.created_at
                FROM companies c
                """,(rs,row)->new OrganizationView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getLong(5),rs.getLong(6),rs.getLong(7),rs.getTimestamp(8).toInstant().toString()));
        return java.util.stream.Stream.concat(institutions.stream(),companies.stream()).sorted((a,b)->b.createdAt().compareTo(a.createdAt())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationView create(@AuthenticationPrincipal String email,@RequestBody OrganizationInput input) {
        requireSuperAdmin(email);
        String name=clean(input==null?null:input.name());
        String type=clean(input==null?null:input.type()).toUpperCase(Locale.ROOT);
        if(name.isBlank()||name.length()>180||!Set.of("ASSOCIATION","EDUCATIONAL","QUALIFICATION").contains(type)) bad("Informe nome e tipo de organização válidos.");
        Long id=jdbc.queryForObject("INSERT INTO enterprise_organizations(name,organization_type,status) VALUES (?,?, 'PENDING') RETURNING id",Long.class,name,type);
        audit(id,email,"ORGANIZATION_CREATED","ORGANIZATION",Long.toString(id));
        return byId(id);
    }

    @PutMapping("/{id}/status")
    public OrganizationView status(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody StatusInput input) {
        requireSuperAdmin(email);
        if(input==null||!Set.of("ACTIVE","PENDING","SUSPENDED").contains(input.status())) bad("Status de organização inválido.");
        if(jdbc.update("UPDATE enterprise_organizations SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",input.status(),id)==0) notFound("Organização não encontrada.");
        audit(id,email,"ORGANIZATION_STATUS_CHANGED","ORGANIZATION",Long.toString(id));
        return byId(id);
    }

    @GetMapping("/{id}/companies")
    public List<LinkedCompany> linkedCompanies(@AuthenticationPrincipal String email,@PathVariable long id) {
        requireSuperAdmin(email);
        return jdbc.query("SELECT c.id,c.trade_name,r.status FROM enterprise_organization_companies r JOIN companies c ON c.id=r.company_id WHERE r.organization_id=? AND r.relationship_type='ASSOCIATION_MEMBER' ORDER BY c.trade_name",(rs,row)->new LinkedCompany(rs.getLong(1),rs.getString(2),rs.getString(3)),id);
    }

    @PostMapping("/{id}/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkCompany(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody CompanyLinkInput input) {
        requireSuperAdmin(email);
        if(input==null||input.companyId()<=0||!Set.of("ACTIVE","PENDING").contains(input.status())) bad("Vínculo inválido.");
        if(!exists("SELECT COUNT(*) FROM enterprise_organizations WHERE id=? AND organization_type='ASSOCIATION'",id)||!exists("SELECT COUNT(*) FROM companies WHERE id=?",input.companyId())) notFound("Associação ou empresa não encontrada.");
        jdbc.update("INSERT INTO enterprise_organization_companies(organization_id,company_id,relationship_type,status) VALUES (?,?,'ASSOCIATION_MEMBER',?) ON CONFLICT(organization_id,company_id,relationship_type) DO UPDATE SET status=EXCLUDED.status",id,input.companyId(),input.status());
        audit(id,email,"COMPANY_LINK_UPSERTED","COMPANY",Long.toString(input.companyId()));
    }

    @PutMapping("/{id}/companies/{companyId}")
    public void updateCompanyLink(@AuthenticationPrincipal String email,@PathVariable long id,@PathVariable long companyId,@RequestBody StatusInput input) {
        requireSuperAdmin(email);
        if(input==null||!Set.of("ACTIVE","PENDING","ENDED").contains(input.status())) bad("Status do vínculo inválido.");
        if(jdbc.update("UPDATE enterprise_organization_companies SET status=? WHERE organization_id=? AND company_id=? AND relationship_type='ASSOCIATION_MEMBER'",input.status(),id,companyId)==0) notFound("Vínculo não encontrado.");
        audit(id,email,"COMPANY_LINK_STATUS_CHANGED","COMPANY",Long.toString(companyId));
    }

    @PostMapping("/{id}/members")
    public InviteView inviteMember(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody MemberInput input) {
        requireSuperAdmin(email);
        return createInvite(id,email,input);
    }

    private InviteView createInvite(long id,String actor,MemberInput input) {
        String memberEmail=clean(input==null?null:input.email()).toLowerCase(Locale.ROOT);
        String role=clean(input==null?null:input.role()).toUpperCase(Locale.ROOT);
        if(!validEmail(memberEmail)||!Set.of("ASSOCIATION_ADMIN","ASSOCIATION_MEMBER","EDUCATION_ADMIN","CONTENT_EDITOR","PARTNER_MANAGER").contains(role)) bad("E-mail ou papel incompatível com a organização.");
        String type=jdbc.query("SELECT organization_type FROM enterprise_organizations WHERE id=? AND status='ACTIVE'",(rs,row)->rs.getString(1),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Organização ativa não encontrada."));
        boolean compatible=switch(type){case "ASSOCIATION"->Set.of("ASSOCIATION_ADMIN","ASSOCIATION_MEMBER").contains(role);case "EDUCATIONAL"->Set.of("EDUCATION_ADMIN","CONTENT_EDITOR","PARTNER_MANAGER").contains(role);case "QUALIFICATION"->Set.of("PARTNER_MANAGER","CONTENT_EDITOR").contains(role);default->false;};
        if(!compatible) bad("O papel não é válido para esta organização.");
        String raw=UUID.randomUUID().toString(); Instant expires=Instant.now().plusSeconds(7L*24*60*60);
        jdbc.update("INSERT INTO enterprise_organization_invites(organization_id,email,role,token_hash,expires_at) VALUES (?,?,?,?,?)",id,memberEmail,role,sha256(raw),java.sql.Timestamp.from(expires));
        audit(id,actor,"ORGANIZATION_MEMBER_INVITED","MEMBER",memberEmail);
        return new InviteView(memberEmail,role,expires.toString(),frontendUrl+"/?organizationInvite="+raw);
    }
    private OrganizationView byId(long id){return jdbc.query("SELECT o.id,o.name,o.organization_type,o.status,(SELECT COUNT(*) FROM enterprise_organization_members m WHERE m.organization_id=o.id),(SELECT COUNT(*) FROM enterprise_organization_companies r WHERE r.organization_id=o.id AND r.status='ACTIVE'),o.created_at FROM enterprise_organizations o WHERE o.id=?",(rs,row)->new OrganizationView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),null,rs.getLong(5),rs.getLong(6),rs.getTimestamp(7).toInstant().toString()),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Organização não encontrada."));}
    private boolean exists(String sql,Object...args){Integer value=jdbc.queryForObject(sql,Integer.class,args);return value!=null&&value>0;}
    private void requireSuperAdmin(String email){if(!platformAccess.isSuperAdmin(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso restrito à administração global.");}
    private void audit(Long id,String actor,String event,String type,String ref){jdbc.update("INSERT INTO enterprise_organization_audit_events(organization_id,actor_email,event_type,entity_type,entity_reference) VALUES (?,?,?,?,?)",id,actor,event,type,ref);}
    private String clean(String value){return value==null?"":value.trim();}
    private boolean validEmail(String value){return value!=null&&value.length()<=180&&value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");}
    private String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private void notFound(String message){throw new ResponseStatusException(HttpStatus.NOT_FOUND,message);}
}
