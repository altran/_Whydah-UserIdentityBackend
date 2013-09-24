package net.whydah.iam.service.dataimport;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.whydah.iam.service.dataimport.Organization;
import net.whydah.iam.service.dataimport.OrganizationImporter;

import org.junit.Test;

public class OrganizationImporterTest {

	@Test
	public void parseRoles() {
		String organizationsSource = "testorganizations.csv";
		
		List<Organization> organizations = OrganizationImporter.parseOrganizations(organizationsSource);
		
//		#organizationId, organizationName
//		1, Whydah
//		5, Altran
		
		assertEquals("All organizations must be found.", 2, organizations.size());
		
		Organization organization1 = organizations.get(0);
		assertEquals("organizationId must be set.", "1", organization1.getId());
		assertEquals("organizationName must be set.", "Whydah", organization1.getName());
		
		Organization organization2 = organizations.get(1);
		assertEquals("organizationId must be set.", "5", organization2.getId());
		assertEquals("organizationName must be set.", "Altran", organization2.getName());

	}
}