package com.trampolim.api.company;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.trampolim.api.BackendApplication;
@SpringBootTest(classes = BackendApplication.class)
class CompanyAccountPostgresTest {
    @Autowired CompanyAccountController controller;
    @Autowired PublicCompanyController publicCompanies;
    @Autowired CompanyAccountDeletionController deletion;
    @Autowired CompanyController corporate;
    @Autowired CompanyPaymentController payments;
    @Autowired @Qualifier("empresasJdbcTemplate") JdbcTemplate companies;
    @Test @Transactional(transactionManager="empresasTransactionManager")
    void companyCannotAccessForeignListingsApplicationsOrCheckout() {
        String first="tenant-a-"+UUID.randomUUID()+"@example.test";
        String second="tenant-b-"+UUID.randomUUID()+"@example.test";
        Long targetCompany=null;
        for(String email:List.of(first,second)) {
            Long owner=companies.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,'fixture','Tenant fixture',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",Long.class,email);
            String cnpj=Long.toString(Math.abs(UUID.randomUUID().getMostSignificantBits()%100000000000000L));
            targetCompany=companies.queryForObject("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state,verification_status) VALUES (?,'Tenant fixture','Tenant fixture',?,'fixture','fixture','fixture','fixture','SP','APPROVED') RETURNING id",Long.class,owner,cnpj);
        }
        Long listing=companies.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,is_internship,payment_status,publication_status) VALUES (?,'job','Private fixture','fixture','fixture','fixture','fixture','Remoto','https://example.test',?,TRUE,'PENDING','AWAITING_PAYMENT') RETURNING id",Long.class,targetCompany,second);
        Long application=companies.queryForObject("INSERT INTO company_applications(listing_id,candidate_name,candidate_email) VALUES (?,'Private candidate','private@example.test') RETURNING id",Long.class,listing);
        assertTrue(corporate.listings(first).isEmpty());
        assertTrue(corporate.applications(first).isEmpty());
        assertEquals(1,corporate.listings(second).size());
        assertEquals(1,corporate.applications(second).size());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->corporate.updateStatus(first,listing,new CompanyController.StatusInput("CLOSED")));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->corporate.applicationStatus(first,application,new CompanyController.StatusInput("ACCEPTED")));
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->payments.createCheckout(first,listing)).getStatusCode().value());
        assertEquals("AWAITING_PAYMENT",companies.queryForObject("SELECT publication_status FROM company_listings WHERE id=?",String.class,listing));
        assertEquals("RECEIVED",companies.queryForObject("SELECT status FROM company_applications WHERE id=?",String.class,application));
        assertEquals(0,companies.queryForObject("SELECT count(*) FROM company_listing_payments WHERE listing_id=?",Integer.class,listing));
    }
    @Test @Transactional(transactionManager="empresasTransactionManager")
    void responsibleAccountsCannotDeleteOrganizationsByDeletingTheirLogin() {
        String email="protected-"+UUID.randomUUID()+"@example.test";
        var encoder=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        Long owner=companies.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,?,'Protected fixture',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id",Long.class,email,encoder.encode("FixturePassword!"));
        Long organization=companies.queryForObject("INSERT INTO enterprise_organizations(name,organization_type,status) VALUES ('Protected fixture','ASSOCIATION','ACTIVE') RETURNING id",Long.class);
        companies.update("INSERT INTO enterprise_organization_members(organization_id,email,full_name,role) VALUES (?,?,'Protected fixture','ASSOCIATION_ADMIN')",organization,email);
        var confirmation=new CompanyAccountDeletionController.Confirmation("FixturePassword!");
        var denied=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->deletion.delete(email,confirmation));
        assertEquals(409,denied.getStatusCode().value());
        assertEquals(1,companies.queryForObject("SELECT count(*) FROM company_users WHERE id=?",Integer.class,owner));
        companies.update("DELETE FROM enterprise_organization_members WHERE organization_id=?",organization);
        String cnpj=Long.toString(Math.abs(UUID.randomUUID().getMostSignificantBits()%100000000000000L));
        Long company=companies.queryForObject("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state,verification_status) VALUES (?,'Protected fixture','Protected fixture',?,'fixture','fixture','fixture','fixture','SP','PENDING') RETURNING id",Long.class,owner,cnpj);
        assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->deletion.delete(email,confirmation)).getStatusCode().value());
        assertEquals(1,companies.queryForObject("SELECT count(*) FROM companies WHERE id=?",Integer.class,company));
    }
    @Test @Transactional(transactionManager="empresasTransactionManager")
    void personalCorporateAccountDeletionRequiresPasswordAndPreservesOtherAccounts() {
        String email="delete-"+UUID.randomUUID()+"@example.test";
        String other="other-"+UUID.randomUUID()+"@example.test";
        var encoder=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        for(String address:List.of(email,other)) companies.update("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,?,'Deletion fixture',TRUE,'ACTIVE',CURRENT_TIMESTAMP)",address,encoder.encode("FixturePassword!"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->deletion.delete(email,new CompanyAccountDeletionController.Confirmation("wrong")));
        assertEquals(1,companies.queryForObject("SELECT count(*) FROM company_users WHERE email=?",Integer.class,email));
        deletion.delete(email,new CompanyAccountDeletionController.Confirmation("FixturePassword!"));
        assertEquals(0,companies.queryForObject("SELECT count(*) FROM company_users WHERE email=?",Integer.class,email));
        assertEquals(1,companies.queryForObject("SELECT count(*) FROM company_users WHERE email=?",Integer.class,other));
    }
    @Test @Transactional(transactionManager = "empresasTransactionManager")
    void exportContainsOnlyOwnAccountAndNoSecrets() {
        String first = "export-" + UUID.randomUUID() + "@example.test";
        String second = "export-" + UUID.randomUUID() + "@example.test";
        for (String email : List.of(first,second)) companies.update("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,'private-test-hash','Corporate test',TRUE,'ACTIVE',CURRENT_TIMESTAMP)",email);
        Long organization = companies.queryForObject("INSERT INTO enterprise_organizations(name,organization_type,status) VALUES ('Export fixture','ASSOCIATION','ACTIVE') RETURNING id", Long.class);
        companies.update("INSERT INTO enterprise_organization_members(organization_id,email,full_name,role) VALUES (?,?,'First','ASSOCIATION_ADMIN'),(?,?,'Second','ASSOCIATION_MEMBER')", organization, first, organization, second);
        var result = controller.exportAccount(first);
        assertEquals("empresas",result.get("trail"));
        var account = (Map<?,?>) result.get("account");
        assertEquals(first,account.get("email"));
        assertFalse(account.containsValue(second));
        assertEquals(java.util.Set.of("id","email","full_name","email_verified","account_status","terms_accepted_at","created_at","last_login_at"),account.keySet());
        assertEquals(List.of(),result.get("companyMemberships"));
        List<?> memberships = (List<?>) result.get("organizationMemberships");
        assertEquals(1,memberships.size());
        Map<?,?> ownMembership = (Map<?,?>) memberships.get(0);
        assertEquals(organization,ownMembership.get("organization_id"));
        assertEquals("ASSOCIATION_ADMIN",ownMembership.get("role"));
        assertEquals(java.util.Set.of("organization_id","role","created_at"),ownMembership.keySet());
        var secondMembership = (Map<?,?>) ((List<?>) controller.exportAccount(second).get("organizationMemberships")).get(0);
        assertEquals("ASSOCIATION_MEMBER",secondMembership.get("role"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.exportAccount(null));
        var access = org.mockito.Mockito.mock(com.trampolim.api.security.PlatformAccess.class);
        org.mockito.Mockito.when(access.isSuperAdmin("admin@example.test")).thenReturn(true);
        var admin = new com.trampolim.api.organization.TrailAccountAdminController(access, provider(companies), provider(null));
        var organizationsAdmin=new com.trampolim.api.organization.SaasOrganizationAdminController(companies,access);
        org.springframework.test.util.ReflectionTestUtils.setField(organizationsAdmin,"frontendUrl","https://example.test");
        String invitedEmail="invite-"+UUID.randomUUID()+"@example.test";
        var invite=organizationsAdmin.inviteMember("admin@example.test",organization,new com.trampolim.api.organization.SaasOrganizationAdminController.MemberInput(invitedEmail,"ASSOCIATION_MEMBER"));
        assertTrue(invite.inviteUrl().startsWith("https://example.test/?organizationInvite="));
        assertTrue(companies.queryForObject("SELECT expires_at>CURRENT_TIMESTAMP FROM enterprise_organization_invites WHERE email=?",Boolean.class,invitedEmail));
        String inviteHash=companies.queryForObject("SELECT token_hash FROM enterprise_organization_invites WHERE email=?",String.class,invitedEmail);
        assertEquals(64,inviteHash.length());
        assertFalse(invite.inviteUrl().contains(inviteHash));
        var association=new com.trampolim.api.organization.EnterpriseOrganizationController(companies,provider(null));
        assertNull(association.dashboard(first,organization).publicLearningContents());
        com.trampolim.api.education.PublicLearningCatalog unavailableCatalog=()->new com.trampolim.api.education.PublicLearningCatalog.Summary(null);
        var degradedAssociation=new com.trampolim.api.organization.EnterpriseOrganizationController(companies,provider(unavailableCatalog));
        var dashboard=degradedAssociation.dashboard(first,organization);
        assertEquals(organization,dashboard.organizationId());
        assertEquals(0,dashboard.companies());
        assertNull(dashboard.publicLearningContents());
        com.trampolim.api.education.PublicLearningCatalog emptyCatalog=()->new com.trampolim.api.education.PublicLearningCatalog.Summary(0L);
        assertEquals(0L,new com.trampolim.api.organization.EnterpriseOrganizationController(companies,provider(emptyCatalog)).dashboard(first,organization).publicLearningContents());
        String associationInviteEmail="association-invite-"+UUID.randomUUID()+"@example.test";
        var associationInvite=association.inviteMember(first,organization,new com.trampolim.api.organization.EnterpriseOrganizationController.MemberInput(associationInviteEmail,"ASSOCIATION_MEMBER"));
        assertTrue(associationInvite.inviteUrl().contains("organizationInvite="));
        assertTrue(companies.queryForObject("SELECT expires_at>CURRENT_TIMESTAMP FROM enterprise_organization_invites WHERE email=?",Boolean.class,associationInviteEmail));
        Long otherOrganization=companies.queryForObject("INSERT INTO enterprise_organizations(name,organization_type,status) VALUES ('Other fixture','ASSOCIATION','ACTIVE') RETURNING id",Long.class);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->association.inviteMember(first,otherOrganization,new com.trampolim.api.organization.EnterpriseOrganizationController.MemberInput(associationInviteEmail,"ASSOCIATION_MEMBER")));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->association.inviteMember(second,organization,new com.trampolim.api.organization.EnterpriseOrganizationController.MemberInput(associationInviteEmail,"ASSOCIATION_MEMBER")));
        assertEquals(0L,companies.queryForObject("SELECT count(*) FROM enterprise_organization_invites WHERE organization_id=?",Long.class,otherOrganization));
        long id = ((Number) account.get("id")).longValue();
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> admin.changeStatus("unauthorized@example.test","empresas",id,new com.trampolim.api.organization.TrailAccountAdminController.StatusChange("SUSPENDED","Test")));
        admin.changeStatus("admin@example.test","empresas",id,new com.trampolim.api.organization.TrailAccountAdminController.StatusChange("SUSPENDED","Automated test"));
        assertEquals("SUSPENDED",companies.queryForObject("SELECT account_status FROM company_users WHERE id=?",String.class,id));
        assertEquals(1,companies.queryForObject("SELECT token_version FROM company_users WHERE id=?",Integer.class,id));
        assertEquals(1L,companies.queryForObject("SELECT count(*) FROM account_admin_audit WHERE target_user_id=? AND previous_status='ACTIVE' AND new_status='SUSPENDED' AND actor_email='admin@example.test'",Long.class,id));
        admin.changeStatus("admin@example.test","empresas",id,new com.trampolim.api.organization.TrailAccountAdminController.StatusChange("SUSPENDED","Repeated test"));
        assertEquals(1L,companies.queryForObject("SELECT count(*) FROM account_admin_audit WHERE target_user_id=?",Long.class,id));
        assertEquals(1,companies.queryForObject("SELECT token_version FROM company_users WHERE id=?",Integer.class,id));
        assertEquals("ACTIVE",companies.queryForObject("SELECT account_status FROM company_users WHERE email=?",String.class,second));
        var audit = admin.audit("admin@example.test","empresas",id,0,50);
        assertEquals(1,audit.size());
        assertEquals(id,audit.get(0).userId());
        assertEquals("Automated test",audit.get(0).reason());
        assertEquals("ACTIVE",audit.get(0).previousStatus());
        assertEquals("SUSPENDED",audit.get(0).newStatus());
        assertTrue(admin.audit("admin@example.test","empresas",id,1,50).isEmpty());
        assertTrue(admin.audit("admin@example.test","empresas",((Number) ((Map<?,?>) controller.exportAccount(second).get("account")).get("id")).longValue(),0,50).isEmpty());
    }
    @Test @Transactional(transactionManager = "empresasTransactionManager")
    void publicIntegrationRejectsExpiredDeadlineAcrossFeedProfileAndInteractions() {
        String email = "deadline-" + UUID.randomUUID() + "@example.test";
        Long owner = companies.queryForObject("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,'fixture','Fixture',TRUE,'ACTIVE',CURRENT_TIMESTAMP) RETURNING id", Long.class,email);
        String cnpj = Long.toString(Math.abs(UUID.randomUUID().getMostSignificantBits() % 100000000000000L));
        Long company = companies.queryForObject("INSERT INTO companies(owner_user_id,legal_name,trade_name,cnpj,phone,industry,address,city,state,verification_status) VALUES (?,'Fixture','Fixture',?,'fixture','fixture','fixture','fixture','SP','APPROVED') RETURNING id",Long.class,owner,cnpj);
        String slug = companies.queryForObject("SELECT public_slug FROM companies WHERE id=?",String.class,company);
        Long listing = companies.queryForObject("INSERT INTO company_listings(company_id,kind,title,area,description,requirements,location,modality,url,contact_email,is_internship,payment_status,publication_status,application_deadline,expires_at) VALUES (?,'job','Fixture','fixture','Fixture','Fixture','Fixture','Remoto','https://example.test',?,TRUE,'PAID','PUBLISHED',CURRENT_DATE-1,CURRENT_DATE+10) RETURNING id",Long.class,company,email);
        assertTrue(publicCompanies.profile(slug).listings().isEmpty());
        assertTrue(publicCompanies.internships(null,null,null,null).stream().noneMatch(row -> row.id()==listing));
        assertTrue(publicCompanies.opportunities().stream().noneMatch(row -> row.id()==listing));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> publicCompanies.view(slug,listing));
        companies.update("UPDATE company_listings SET application_deadline=CURRENT_DATE+1 WHERE id=?",listing);
        assertEquals(listing,publicCompanies.profile(slug).listings().get(0).id());
        assertTrue(publicCompanies.internships(null,null,null,null).stream().anyMatch(row -> row.id()==listing));
        assertTrue(publicCompanies.opportunities().stream().anyMatch(row -> row.id()==listing));
        assertNotNull(publicCompanies.view(slug,listing));
        var applicationRequest=new org.springframework.mock.web.MockHttpServletRequest();
        applicationRequest.setRemoteAddr("127.0.0.81");
        String applicant="applicant-"+UUID.randomUUID()+"@example.test";
        var withoutConsent=new PublicCompanyController.ApplicationInput("Explicit applicant",applicant,null,"Only submitted data",false);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> publicCompanies.apply(slug,listing,withoutConsent,applicationRequest));
        assertEquals(0,companies.queryForObject("SELECT count(*) FROM company_applications WHERE listing_id=?",Integer.class,listing));
        publicCompanies.apply(slug,listing,new PublicCompanyController.ApplicationInput("Explicit applicant",applicant,null,"Only submitted data",true),applicationRequest);
        var submitted=companies.queryForMap("SELECT candidate_name,candidate_email,candidate_phone,message,consent_at FROM company_applications WHERE listing_id=?",listing);
        assertEquals("Explicit applicant",submitted.get("candidate_name"));
        assertEquals(applicant,submitted.get("candidate_email"));
        assertEquals("",submitted.get("candidate_phone"));
        assertEquals("Only submitted data",submitted.get("message"));
        assertNotNull(submitted.get("consent_at"));
        companies.update("UPDATE company_listings SET payment_status='PENDING' WHERE id=?",listing);
        assertTrue(publicCompanies.profile(slug).listings().isEmpty());
        assertTrue(publicCompanies.internships(null,null,null,null).stream().noneMatch(row -> row.id()==listing));
        assertTrue(publicCompanies.opportunities().stream().noneMatch(row -> row.id()==listing));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> publicCompanies.internship(listing));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> publicCompanies.view(slug,listing));
        companies.update("UPDATE company_listings SET payment_status='PAID',publication_status='AWAITING_PAYMENT' WHERE id=?",listing);
        assertTrue(publicCompanies.profile(slug).listings().isEmpty());
        assertTrue(publicCompanies.internships(null,null,null,null).stream().noneMatch(row -> row.id()==listing));
        assertTrue(publicCompanies.opportunities().stream().noneMatch(row -> row.id()==listing));
        companies.update("UPDATE company_listings SET publication_status='PUBLISHED' WHERE id=?",listing);
        companies.update("UPDATE companies SET verification_status='SUSPENDED' WHERE id=?",company);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> publicCompanies.profile(slug));
        assertTrue(publicCompanies.internships(null,null,null,null).stream().noneMatch(row -> row.id()==listing));
        assertTrue(publicCompanies.opportunities().stream().noneMatch(row -> row.id()==listing));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> publicCompanies.internship(listing));
    }
    @SuppressWarnings("unchecked")
    private <T> org.springframework.beans.factory.ObjectProvider<T> provider(T value) {
        var provider = org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
    @Test @Transactional(transactionManager = "empresasTransactionManager")
    void corporatePasswordResetUsesOwnDatabaseAndSingleUseLink() {
        String email="reset-company-"+UUID.randomUUID()+"@example.test";
        var passwords=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        companies.update("INSERT INTO company_users(email,password_hash,full_name,email_verified,account_status,terms_accepted_at) VALUES (?,?,'Corporate reset fixture',TRUE,'ACTIVE',CURRENT_TIMESTAMP)",email,passwords.encode("old-password-2026"));
        var mail=org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        var authentication=new com.trampolim.api.auth.IndependentTrailAuthController(provider(companies),provider(null),passwords,new com.trampolim.api.security.TrailTokenService(),provider(mail));
        org.springframework.test.util.ReflectionTestUtils.setField(authentication,"mailEnabled",true);
        org.springframework.test.util.ReflectionTestUtils.setField(authentication,"frontendUrl","https://example.test");
        var request=new org.springframework.mock.web.MockHttpServletRequest("POST","/api/empresas/auth/password/reset-request");
        request.setRemoteAddr("192.0.2.44");
        authentication.resetCompanyRequest(new com.trampolim.api.auth.IndependentTrailAuthController.PasswordResetRequest(email),request);
        var message=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
        org.mockito.Mockito.verify(mail).send(message.capture());
        String text=message.getValue().getText();
        assertNotNull(text);
        assertTrue(text.contains("/#empresas/reset?token="));
        String token=text.substring(text.indexOf("?token=")+7);
        assertNotEquals(token,companies.queryForObject("SELECT password_reset_token_hash FROM company_users WHERE email=?",String.class,email));
        authentication.resetCompany(new com.trampolim.api.auth.IndependentTrailAuthController.PasswordReset(token,"new-password-2026"));
        assertTrue(passwords.matches("new-password-2026",companies.queryForObject("SELECT password_hash FROM company_users WHERE email=?",String.class,email)));
        assertEquals(1,companies.queryForObject("SELECT token_version FROM company_users WHERE email=?",Integer.class,email));
        assertNull(companies.queryForObject("SELECT password_reset_token_hash FROM company_users WHERE email=?",String.class,email));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->authentication.resetCompany(new com.trampolim.api.auth.IndependentTrailAuthController.PasswordReset(token,"another-password-2026")));
        org.mockito.Mockito.verifyNoMoreInteractions(mail);
    }
}
