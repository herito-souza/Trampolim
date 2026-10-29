package com.trampolim.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.boot.test.context.SpringBootTest;
import com.trampolim.api.BackendApplication;
import com.trampolim.api.auth.AuthController;
import com.trampolim.api.company.CompanyVerificationController;
import com.trampolim.api.company.CompanyController;
import com.trampolim.api.company.CompanyPaymentController;
import com.trampolim.api.company.LegacyCompanyMigrationController;
import com.trampolim.api.company.PublicCompanyController;
import com.trampolim.api.education.CapacitaController;
import com.trampolim.api.education.LegacyEducationMigrationController;
import com.trampolim.api.organization.EnterpriseOrganizationController;
import com.trampolim.api.organization.SaasOrganizationAdminController;

@SpringBootTest(classes = BackendApplication.class)
class BackendApplicationTests {
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private AuthController authController;

	@Autowired
	private CompanyVerificationController companyVerificationController;

	@Autowired
	private CompanyController companyController;

	@Autowired
	private CompanyPaymentController companyPaymentController;

	@Autowired
	private PublicCompanyController publicCompanyController;

	@Autowired
	private LegacyCompanyMigrationController legacyCompanyMigrationController;

	@Autowired
	private CapacitaController capacitaController;

	@Autowired
	private LegacyEducationMigrationController legacyEducationMigrationController;

	@Autowired
	private EnterpriseOrganizationController enterpriseOrganizationController;

	@Autowired
	private SaasOrganizationAdminController saasOrganizationAdminController;

	@Test
	void contextLoads() {
		assertNotNull(transactionManager);
		assertNotNull(companyVerificationController);
		assertNotNull(companyController);
		assertNotNull(companyPaymentController);
		assertNotNull(publicCompanyController);
		assertNotNull(legacyCompanyMigrationController);
		assertNotNull(capacitaController);
		assertNotNull(legacyEducationMigrationController);
		assertNotNull(enterpriseOrganizationController);
		assertNotNull(saasOrganizationAdminController);
	}

	@Test
	@Transactional
	void opportunitiesRegistrationUsesThePrimaryJpaAndJdbcTransaction() {
		String email = "transaction-test-" + UUID.randomUUID() + "@example.test";
		HttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/cadastro");
		var response = authController.register(
				new AuthController.Registration("Transaction Test", email, "Transaction-Test-2026!", true), request);

		assertNotNull(response.token());
		assertEquals("PERSON", response.accountType());
	}

}
