package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import net.whydah.identity.config.AppConfig;
import net.whydah.identity.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class IamDataImporter {
    private static final Logger log = LoggerFactory.getLogger(IamDataImporter.class);
    public static final String CHARSET_NAME = "ISO-8859-1";
	private ApplicationImporter applicationImporter;
	private OrganizationImporter organizationImporter;
	private WhydahUserIdentityImporter userImporter;
	private RoleMappingImporter roleMappingImporter;
	
	@Inject
	public IamDataImporter(ApplicationImporter applicationImporter, OrganizationImporter organizationImporter,
                           WhydahUserIdentityImporter userImporter, RoleMappingImporter roleMappingImporter)  {
		this.applicationImporter = applicationImporter;
		this.organizationImporter = organizationImporter;
		this.userImporter = userImporter;
		this.roleMappingImporter = roleMappingImporter;
	}
	
	public void importIamData() {
        //Database migrations should already have been performed before import.

        InputStream ais = null;
        InputStream ois = null;
        InputStream uis = null;
        InputStream rmis = null;
        try {
            String applicationsImportSource = AppConfig.appConfig.getProperty("import.applicationssource");
            ais = openInputStream("Applications", applicationsImportSource);
            applicationImporter.importApplications(ais);

            String organizationsImportSource = AppConfig.appConfig.getProperty("import.organizationssource");
            ois = openInputStream("Organizations", organizationsImportSource);
            organizationImporter.importOrganizations(ois);

            String userImportSource = AppConfig.appConfig.getProperty("import.usersource");
            uis = openInputStream("Users", userImportSource);
            userImporter.importUsers(uis);

            String roleMappingImportSource = AppConfig.appConfig.getProperty("import.rolemappingsource");
            rmis = openInputStream("RoleMappings", roleMappingImportSource);
            roleMappingImporter.importRoleMapping(rmis);
        } finally {
            FileUtils.close(ais);
            FileUtils.close(ois);
            FileUtils.close(uis);
            FileUtils.close(rmis);
        }
    }

    InputStream openInputStream(String tableName, String importSource) {
        InputStream is;
        if (FileUtils.localFileExist(importSource)) {
            log.info("Importing {} from local config override. {}", tableName,importSource);
            is = FileUtils.openLocalFile(importSource);
        } else {
            log.info("Import {} from classpath {}", tableName, importSource);
            is = FileUtils.openFileOnClasspath(importSource);
        }
        return is;
    }
}
