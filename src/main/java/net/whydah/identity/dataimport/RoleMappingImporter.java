package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RoleMappingImporter {

	private static final Logger logger = LoggerFactory.getLogger(RoleMappingImporter.class);
	
	private static final int REQUIRED_NUMBER_OF_FIELDS = 6;

	private static final int USERID = 0;
	private static final int APPLICATIONID = 1;
	private static final int APPLICATIONNAME = 2;
	private static final int ORGANIZATIONNAME = 3;
	private static final int ROLENAME = 4;
	private static final int ROLEVALUE = 5;
	
    private UserPropertyAndRoleRepository roleMappingRepository;
    
    @Inject
	public RoleMappingImporter(UserPropertyAndRoleRepository roleMappingRepository) {
		this.roleMappingRepository = roleMappingRepository;
	}

    public List<UserPropertyAndRole> importRoleMapping(String roleMappingSource) {
    	List<UserPropertyAndRole> roles = parseRoleMapping(roleMappingSource);
    	saveRoleMapping(roles);
    	return roles;
    }
    
	private void saveRoleMapping(List<UserPropertyAndRole> roles) {
		for(UserPropertyAndRole userPropertyAndRole : roles) {
			roleMappingRepository.addUserPropertyAndRole(userPropertyAndRole);	
		}
	}

	protected static List<UserPropertyAndRole> parseRoleMapping(String roleMappingSource) {

		BufferedReader reader = null;
		try {
			List<UserPropertyAndRole> roleMappings = new ArrayList<>();
			logger.info("Importing data from {}", roleMappingSource);
	        InputStream classpathStream = RoleMappingImporter.class.getClassLoader().getResourceAsStream(roleMappingSource);
	        reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
	        String line = null; 
	        while (null != (line = reader.readLine())) {
	        	boolean isComment = line.startsWith("#");
				if (isComment) {
	        		continue;
	        	}
				
	        	String[] lineArray = line.split(",");
	        	validateLine(line, lineArray);
	        	
	            UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();

	            userPropertyAndRole.setUid(cleanString(lineArray[USERID]));
	            userPropertyAndRole.setApplicationId(cleanString(lineArray[APPLICATIONID]));
	            userPropertyAndRole.setApplicationName(cleanString(lineArray[APPLICATIONNAME]));
	        	
	            userPropertyAndRole.setOrganizationName(cleanString(lineArray[ORGANIZATIONNAME]));
	            userPropertyAndRole.setApplicationRoleName(cleanString(lineArray[ROLENAME]));
	            userPropertyAndRole.setApplicationRoleValue(cleanString(lineArray[ROLEVALUE]));
	            
	            roleMappings.add(userPropertyAndRole);
	        }
			return roleMappings;
		
		} catch (IOException ioe) {
			logger.error("Unable to read file {}", roleMappingSource);
			throw new RuntimeException("Unable to import Role Mappings from file: " + roleMappingSource);
		} finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing stream", e);
                }
            }
        }
	}

	private static String cleanString(String string) {
		return string==null ? string : string.trim();
	}

	private static void validateLine(String line, String[] lineArray) {
		if (lineArray.length < REQUIRED_NUMBER_OF_FIELDS) {
			throw new RuntimeException("Role Mapping parsing error. Incorrect format of Line. It does not contain all required fields. Line: " + line);
		}
	}
}
