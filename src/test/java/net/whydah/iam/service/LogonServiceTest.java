package net.whydah.iam.service;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.MediaTypes;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import net.whydah.iam.service.Main;
import net.whydah.iam.service.config.AppConfig;

import net.whydah.iam.service.dataimport.DatabaseHelper;
import net.whydah.iam.service.helper.FileUtils;
import net.whydah.iam.service.ldap.EmbeddedADS;
import net.whydah.iam.service.ldap.LDAPHelper;
import net.whydah.iam.service.ldap.LdapAuthenticatorImpl;
import net.whydah.iam.service.repository.AuditLogRepository;
import net.whydah.iam.service.repository.BackendConfigDataRepository;
import net.whydah.iam.service.repository.UserPropertyAndRoleRepository;
import net.whydah.iam.service.resource.UserAdminHelper;
import net.whydah.iam.service.search.Indexer;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LogonServiceTest {
    private static URI baseUri;
    Client restClient;

    private final static String basepath = "target/WhydahUserResourceTest/";
    private final static String ldappath = basepath + "hsqldb/ldap/";
    private final static String dbpath = basepath + "hsqldb/roles";
    //    private final static int LDAP_PORT = 10937;
    private static String LDAP_URL; // = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";

    private static EmbeddedADS ads;
    private static LDAPHelper ldapHelper;
    private static LdapAuthenticatorImpl ldapAuthenticator;
    private static UserPropertyAndRoleRepository roleRepository;
    private static UserAdminHelper userAdminHelper;
    private static QueryRunner queryRunner;


    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(AppConfig.IAM_MODE_KEY, AppConfig.IAM_MODE_DEV);

        int HTTP_PORT = new Integer(AppConfig.appConfig.getProperty("service.port"));
        int LDAP_PORT = new Integer(AppConfig.appConfig.getProperty("ldap.embedded.port"));
        LDAP_URL = "ldap://localhost:" + LDAP_PORT + "/dc=external,dc=WHYDAH,dc=no";

        FileUtils.deleteDirectory(new File(basepath));

        File ldapdir = new File(ldappath);
        ldapdir.mkdirs();
        ads = new EmbeddedADS(ldappath);
        ads.startServer(LDAP_PORT);
        ldapHelper = new LDAPHelper(LDAP_URL, "uid=admin,ou=system", "secret", "initials");
        ldapAuthenticator = new LdapAuthenticatorImpl(LDAP_URL, "uid=admin,ou=system", "secret", "initials");


        roleRepository = new UserPropertyAndRoleRepository();
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setUrl("jdbc:hsqldb:file:" + dbpath);
        queryRunner = new QueryRunner(dataSource);

        DatabaseHelper databaseHelper = new DatabaseHelper(queryRunner);
        databaseHelper.initDB();

        roleRepository.setQueryRunner(queryRunner);
        BackendConfigDataRepository configDataRepository = new BackendConfigDataRepository();
        configDataRepository.setQueryRunner(queryRunner);
        roleRepository.setBackendConfigDataRepository(configDataRepository);
        AuditLogRepository auditLogRepository = new AuditLogRepository(queryRunner);
        Directory index = new NIOFSDirectory(new File(basepath + "lucene"));
        userAdminHelper = new UserAdminHelper(ldapHelper, new Indexer(index), auditLogRepository, roleRepository);

        baseUri = UriBuilder.fromUri("http://localhost/uib/").port(HTTP_PORT).build();
    }


    @AfterClass
    public static void teardown()  {
        if (ads != null) {
            ads.stopServer();
        }
    }

    @Before
    public void initRun() throws Exception {
        restClient = Client.create();
    }

    @Test
    public void welcome() {
        WebResource webResource = restClient.resource(baseUri);
        String s = webResource.get(String.class);
        assertTrue(s.contains("Whydah"));
        assertTrue(s.contains("<FORM"));
        assertFalse(s.contains("backtrace"));
    }

    /**
     * Test if a WADL document is available at the relative path
     * "application.wadl".
     */
    @Test
    public void testApplicationWadl() {
        WebResource webResource = restClient.resource(baseUri);
        String serviceWadl = webResource.path("application.wadl").
                accept(MediaTypes.WADL).get(String.class);
//        System.out.println("WADL:"+serviceWadl);
        assertTrue(serviceWadl.length() > 60);
    }

    @Test
    public void formLogonOK() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("username", "thomasp");
        formData.add("password", "logMeInPlease");
        ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        String responseBody = response.getEntity(String.class);
        //System.out.println(responseBody);
        //assertTrue(responseBody.contains("Logon ok"));
        assertTrue(responseBody.contains("thomas.pringle@altran.com"));
    }

    @Test
    public void formLogonFail() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("username", "thomasp");
        formData.add("password", "vrangt");
        ClientResponse response = webResource.path("logon").type("application/x-www-form-urlencoded").post(ClientResponse.class, formData);
        String responseBody = response.getEntity(String.class);
        //System.out.println(responseBody);

        assertTrue(responseBody.contains("failed"));
        assertFalse(responseBody.contains("freecodeUser"));
    }

    @Test
    public void XMLLogonOK() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>thomasp</username><coffee>yes please</coffee><password>logMeInPlease</password></user></auth></authgreier>";
        ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        String responseXML = response.getEntity(String.class);
        //System.out.println(responseXML);
        assertTrue(responseXML.contains("thomasp"));

    }

    @Test
    public void XMLLogonFail() throws IOException {
        WebResource webResource = restClient.resource(baseUri);
        String payload = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?><authgreier><auth><dilldall>dilldall</dilldall><user><username>thomasp</username><coffee>yes please</coffee><password>vrangt</password></user></auth></authgreier>";
        ClientResponse response = webResource.path("logon").type("application/xml").post(ClientResponse.class, payload);
        String responseXML = response.getEntity(String.class);
        //System.out.println(responseXML);
        assertTrue(responseXML.contains("logonFailed"));
        assertFalse(responseXML.contains("thomasp"));
    }

}