package com.trampolim.api.organization;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Association and partner operations are backed only by the Empresas database and Empresas JWT. */
@RestController
@RequestMapping("/api/empresas")
@ConditionalOnProperty(name = "trampolim.datasource.empresas.enabled", havingValue = "true")
public class EnterpriseOrganizationController {
    private final JdbcTemplate jdbc;
    private final ObjectProvider<com.trampolim.api.education.PublicLearningCatalog> capacitaProvider;

    public EnterpriseOrganizationController(@Qualifier("empresasJdbcTemplate") JdbcTemplate jdbc,
            ObjectProvider<com.trampolim.api.education.PublicLearningCatalog> capacitaProvider) {
        this.jdbc = jdbc;
        this.capacitaProvider = capacitaProvider;
    }

    public record OrganizationView(long id, String name, String type, String status, Long legacyCompanyId,
            long members, long linkedCompanies, String createdAt) {}
    public record OrganizationInput(String name, String type) {}
    public record StatusInput(String status) {}
    public record CompanyLinkInput(long companyId, String status) {}
    public record LinkedCompany(long companyId, String name, String status) {}
    public record CompanyOption(long companyId, String name, String city, String state, String industry) {}
    public record MemberInput(String email, String role) {}
    public record OrganizationMember(long userId, String name, String email, String role, String createdAt) {}
    public record AssociationDashboard(long organizationId, String name, long companies, long activeCompanies,
            long openJobs, long openInternships, long applications, Long publicLearningContents) {}
    public record InviteView(String email, String role, String expiresAt, String inviteUrl) {}
    public record InviteToken(String token) {}

    @GetMapping("/association/organizations")
    public List<OrganizationView> myAssociations(@AuthenticationPrincipal String email) {
        requireCompanyAccount(email);
        return jdbc.query("""
                SELECT o.id,o.name,o.organization_type,o.status,NULL::BIGINT,
                    (SELECT COUNT(*) FROM enterprise_organization_members m WHERE m.organization_id=o.id),
                    (SELECT COUNT(*) FROM enterprise_organization_companies r WHERE r.organization_id=o.id AND r.status='ACTIVE'),o.created_at
                FROM enterprise_organizations o JOIN enterprise_organization_members m ON m.organization_id=o.id
                JOIN company_users u ON LOWER(u.email)=LOWER(m.email)
                WHERE o.organization_type='ASSOCIATION' AND o.status='ACTIVE' AND LOWER(u.email)=LOWER(?) AND u.email_verified=TRUE AND u.account_status='ACTIVE'
                ORDER BY o.name
                """, (rs,row)->new OrganizationView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),null,rs.getLong(6),rs.getLong(7),rs.getTimestamp(8).toInstant().toString()),email);
    }

    @PostMapping("/organizations/invites/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptOrganizationInvite(@AuthenticationPrincipal String email,@RequestBody InviteToken request) {
        requireCompanyAccount(email);
        String token=clean(request==null?null:request.token());
        List<Object[]> invites=jdbc.query("SELECT i.id,i.organization_id,i.role FROM enterprise_organization_invites i JOIN enterprise_organizations o ON o.id=i.organization_id AND o.status='ACTIVE' WHERE i.token_hash=? AND LOWER(i.email)=LOWER(?) AND i.accepted_at IS NULL AND i.expires_at>CURRENT_TIMESTAMP",(rs,row)->new Object[]{rs.getLong(1),rs.getLong(2),rs.getString(3)},sha256(token),email);
        if(invites.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"O convite expirou, já foi usado ou pertence a outro e-mail.");
        Object[] invite=invites.get(0);
        jdbc.update("INSERT INTO enterprise_organization_members(organization_id,email,full_name,role) SELECT ?,LOWER(u.email),u.full_name,? FROM company_users u WHERE LOWER(u.email)=LOWER(?) AND u.email_verified=TRUE AND u.account_status='ACTIVE' ON CONFLICT DO NOTHING",invite[1],invite[2],email);
        jdbc.update("UPDATE enterprise_organization_invites SET accepted_at=CURRENT_TIMESTAMP WHERE id=?",invite[0]);
    }

    @GetMapping("/association/organizations/{id}/dashboard")
    public AssociationDashboard dashboard(@AuthenticationPrincipal String email,@PathVariable long id) {
        requireAssociationMember(email,id);
        List<AssociationDashboard> found=jdbc.query("""
                SELECT o.id,o.name,
                    (SELECT COUNT(*) FROM enterprise_organization_companies r WHERE r.organization_id=o.id AND r.status='ACTIVE'),
                    (SELECT COUNT(*) FROM enterprise_organization_companies r JOIN companies c ON c.id=r.company_id WHERE r.organization_id=o.id AND r.status='ACTIVE' AND c.verification_status='APPROVED'),
                    (SELECT COUNT(*) FROM company_listings l JOIN enterprise_organization_companies r ON r.company_id=l.company_id WHERE r.organization_id=o.id AND r.status='ACTIVE' AND l.kind='job' AND l.is_internship=FALSE AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE)),
                    (SELECT COUNT(*) FROM company_listings l JOIN enterprise_organization_companies r ON r.company_id=l.company_id WHERE r.organization_id=o.id AND r.status='ACTIVE' AND l.is_internship=TRUE AND l.payment_status='PAID' AND l.publication_status='PUBLISHED' AND (l.expires_at IS NULL OR l.expires_at>=CURRENT_DATE)),
                    (SELECT COUNT(*) FROM company_applications a JOIN company_listings l ON l.id=a.listing_id JOIN enterprise_organization_companies r ON r.company_id=l.company_id WHERE r.organization_id=o.id AND r.status='ACTIVE')
                FROM enterprise_organizations o WHERE o.id=? AND o.organization_type='ASSOCIATION' AND o.status='ACTIVE'
                """,(rs,row)->new AssociationDashboard(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),rs.getLong(6),rs.getLong(7),null),id);
        if(found.isEmpty()) notFound("Associação não encontrada ou inativa.");
        var catalog=capacitaProvider.getIfAvailable();
        Long contents=catalog==null?null:catalog.summary().publishedContents();
        AssociationDashboard value=found.get(0);
        return new AssociationDashboard(value.organizationId(),value.name(),value.companies(),value.activeCompanies(),value.openJobs(),value.openInternships(),value.applications(),contents);
    }

    @GetMapping("/association/organizations/{id}/available-companies")
    public List<CompanyOption> availableCompanies(@AuthenticationPrincipal String email,@PathVariable long id) {
        requireAssociationManager(email,id);
        return jdbc.query("""
                SELECT c.id,c.trade_name,c.city,c.state,c.industry FROM companies c
                WHERE c.verification_status='APPROVED' AND NOT EXISTS(SELECT 1 FROM enterprise_organization_companies r WHERE r.organization_id=? AND r.company_id=c.id AND r.relationship_type='ASSOCIATION_MEMBER' AND r.status IN ('ACTIVE','PENDING'))
                ORDER BY c.trade_name
                """,(rs,row)->new CompanyOption(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5)),id);
    }

    @PostMapping("/association/organizations/{id}/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public void requestCompanyLink(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody CompanyLinkInput input) {
        requireAssociationManager(email,id);
        if(input==null || input.status()==null || !"PENDING".equals(input.status()) || input.companyId()<=0) bad("A solicitação de vínculo precisa ficar pendente.");
        if(!exists("SELECT COUNT(*) FROM companies WHERE id=? AND verification_status='APPROVED'",input.companyId())) notFound("Empresa aprovada não encontrada.");
        upsertCompanyLink(id,input.companyId(),"PENDING");
        audit(id,email,"COMPANY_LINK_REQUESTED","COMPANY",Long.toString(input.companyId()));
    }

    @PutMapping("/association/organizations/{id}/companies/{companyId}")
    public void associationSetLink(@AuthenticationPrincipal String email,@PathVariable long id,@PathVariable long companyId,@RequestBody StatusInput input) {
        requireAssociationManager(email,id);
        if(input==null || !Set.of("PENDING","ENDED").contains(input.status())) bad("A associação só pode solicitar ou encerrar vínculos.");
        setCompanyLink(id,companyId,input);
        audit(id,email,"COMPANY_LINK_UPDATED","COMPANY",Long.toString(companyId));
    }

    @GetMapping("/association/organizations/{id}/companies")
    public List<LinkedCompany> associationCompanies(@AuthenticationPrincipal String email,@PathVariable long id) {
        requireAssociationMember(email,id);
        return linkedCompanies(id);
    }

    @GetMapping("/association/organizations/{id}/members")
    public List<OrganizationMember> members(@AuthenticationPrincipal String email,@PathVariable long id) {
        requireAssociationManager(email,id);
        return jdbc.query("SELECT m.id,m.full_name,m.email,m.role,m.created_at FROM enterprise_organization_members m WHERE m.organization_id=? ORDER BY CASE WHEN m.role='ASSOCIATION_ADMIN' THEN 0 ELSE 1 END,m.full_name",(rs,row)->new OrganizationMember(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getTimestamp(5).toInstant().toString()),id);
    }

    @PostMapping("/association/organizations/{id}/members/invites")
    public InviteView inviteMember(@AuthenticationPrincipal String email,@PathVariable long id,@RequestBody MemberInput input) {
        requireAssociationManager(email,id);
        return inviteMember(id,email,input);
    }

    @DeleteMapping("/association/organizations/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal String email,@PathVariable long id,@PathVariable long memberId) {
        requireAssociationManager(email,id);
        String role=jdbc.query("SELECT role FROM enterprise_organization_members WHERE organization_id=? AND id=?",(rs,row)->rs.getString(1),id,memberId).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Membro não encontrado."));
        if("ASSOCIATION_ADMIN".equals(role) && count(jdbc,"SELECT COUNT(*) FROM enterprise_organization_members WHERE organization_id=? AND role='ASSOCIATION_ADMIN'",id)<=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"A associação precisa manter pelo menos uma pessoa administradora.");
        jdbc.update("DELETE FROM enterprise_organization_members WHERE organization_id=? AND id=?",id,memberId);
        audit(id,email,"ORGANIZATION_MEMBER_REMOVED","MEMBER",Long.toString(memberId));
    }

    @GetMapping("/association/organizations/{id}/invites")
    public List<InviteView> invites(@AuthenticationPrincipal String email,@PathVariable long id) {
        requireAssociationManager(email,id);
        return jdbc.query("SELECT email,role,expires_at FROM enterprise_organization_invites WHERE organization_id=? AND accepted_at IS NULL AND expires_at>CURRENT_TIMESTAMP ORDER BY created_at DESC",(rs,row)->new InviteView(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant().toString(),""),id);
    }

    private InviteView inviteMember(long organizationId,String actorEmail,MemberInput input) {
        String memberEmail=clean(input==null?null:input.email()).toLowerCase(Locale.ROOT);
        String role=clean(input==null?null:input.role()).toUpperCase(Locale.ROOT);
        if(!validEmail(memberEmail) || !Set.of("ASSOCIATION_ADMIN","ASSOCIATION_MEMBER").contains(role)) bad("E-mail ou papel inválido para a associação.");
        if(!exists("SELECT COUNT(*) FROM enterprise_organizations WHERE id=? AND organization_type='ASSOCIATION' AND status='ACTIVE'",organizationId)) notFound("Associação ativa não encontrada.");
        String rawToken=UUID.randomUUID().toString();
        Instant expires=Instant.now().plusSeconds(7L*24*60*60);
        jdbc.update("INSERT INTO enterprise_organization_invites(organization_id,email,role,token_hash,expires_at) VALUES (?,?,?,?,?)",organizationId,memberEmail,role,sha256(rawToken),java.sql.Timestamp.from(expires));
        audit(organizationId,actorEmail,"ORGANIZATION_MEMBER_INVITED","MEMBER",memberEmail);
        String url=frontendUrl()+"/?organizationInvite="+rawToken;
        return new InviteView(memberEmail,role,expires.toString(),url);
    }

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:5173}") private String appFrontendUrl;
    private String frontendUrl(){return appFrontendUrl;}
    private void setCompanyLink(long organizationId,long companyId,StatusInput input) {
        if(input==null || !Set.of("ACTIVE","PENDING","ENDED").contains(input.status())) bad("Status de vínculo inválido.");
        int changed=jdbc.update("UPDATE enterprise_organization_companies SET status=? WHERE organization_id=? AND company_id=? AND relationship_type='ASSOCIATION_MEMBER'",input.status(),organizationId,companyId);
        if(changed==0) notFound("Vínculo não encontrado.");
    }
    private void upsertCompanyLink(long organizationId,long companyId,String status) {
        jdbc.update("INSERT INTO enterprise_organization_companies(organization_id,company_id,relationship_type,status) VALUES (?,?,'ASSOCIATION_MEMBER',?) ON CONFLICT(organization_id,company_id,relationship_type) DO UPDATE SET status=EXCLUDED.status",organizationId,companyId,status);
    }
    private List<LinkedCompany> linkedCompanies(long organizationId) {
        return jdbc.query("SELECT c.id,c.trade_name,r.status FROM enterprise_organization_companies r JOIN companies c ON c.id=r.company_id WHERE r.organization_id=? AND r.relationship_type='ASSOCIATION_MEMBER' ORDER BY c.trade_name",(rs,row)->new LinkedCompany(rs.getLong(1),rs.getString(2),rs.getString(3)),organizationId);
    }
    private OrganizationView organization(long id) {
        return jdbc.query("SELECT o.id,o.name,o.organization_type,o.status,(SELECT COUNT(*) FROM enterprise_organization_members m WHERE m.organization_id=o.id),(SELECT COUNT(*) FROM enterprise_organization_companies r WHERE r.organization_id=o.id AND r.status='ACTIVE'),o.created_at FROM enterprise_organizations o WHERE o.id=?",(rs,row)->new OrganizationView(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),null,rs.getLong(5),rs.getLong(6),rs.getTimestamp(7).toInstant().toString()),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Organização não encontrada."));
    }
    private void requireAssociationManager(String email,long id) {
        if(!activeOrganizationUser(email,id,"ASSOCIATION_ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seu perfil não pode administrar esta associação.");
    }
    private void requireAssociationMember(String email,long id) {
        if(!activeOrganizationUser(email,id,"ASSOCIATION_MEMBER","ASSOCIATION_ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seu perfil não pode acessar este painel institucional.");
    }
    private boolean activeOrganizationUser(String email,long id,String... roles) {
        if(email==null || email.isBlank()) return false;
        String placeholders=String.join(",",java.util.Collections.nCopies(roles.length,"?"));
        Object[] args=new Object[2+roles.length]; args[0]=id;args[1]=email;
        System.arraycopy(roles,0,args,2,roles.length);
        Integer allowed=jdbc.queryForObject("SELECT COUNT(*) FROM enterprise_organizations o JOIN enterprise_organization_members m ON m.organization_id=o.id JOIN company_users u ON LOWER(u.email)=LOWER(m.email) WHERE o.id=? AND o.organization_type='ASSOCIATION' AND o.status='ACTIVE' AND LOWER(u.email)=LOWER(?) AND u.email_verified=TRUE AND u.account_status='ACTIVE' AND m.role IN ("+placeholders+")",Integer.class,args);
        return allowed!=null && allowed>0;
    }
    private void requireCompanyAccount(String email) {
        if(email==null || !exists("SELECT COUNT(*) FROM company_users WHERE LOWER(email)=LOWER(?) AND account_status='ACTIVE' AND email_verified=TRUE",email))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão empresarial inválida.");
    }
    private boolean exists(String sql,Object... args) { return count(jdbc,sql,args)>0; }
    private long count(JdbcTemplate source,String sql,Object... args) { Long value=source.queryForObject(sql,Long.class,args); return value==null?0:value; }
    private void audit(Long organizationId,String email,String event,String type,String ref) { jdbc.update("INSERT INTO enterprise_organization_audit_events(organization_id,actor_email,event_type,entity_type,entity_reference) VALUES (?,?,?,?,?)",organizationId,email,event,type,ref); }
    private String clean(String value){return value==null?"":value.trim();}
    private boolean validEmail(String value){return value!=null&&value.length()<=180&&value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");}
    private String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private void bad(String message){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private void notFound(String message){throw new ResponseStatusException(HttpStatus.NOT_FOUND,message);}
}
