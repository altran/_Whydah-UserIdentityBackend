package net.whydah.identity.user.authentication;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;
import com.sun.jersey.api.view.Viewable;
import net.whydah.identity.audit.AuditLogRepository;
import net.whydah.identity.user.WhydahUser;
import net.whydah.identity.user.identity.UserAuthenticationService;
import net.whydah.identity.user.identity.WhydahUserIdentity;
import net.whydah.identity.user.resource.UserAdminHelper;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.identity.user.role.UserPropertyAndRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Service for authorization of users and finding WhydahUser with corresponding applications, organizations and roles.
 * This not a RESTful endpoint. This is a http RPC endpoint.
 */
@Path("/{applicationTokenId}/authenticate/user")
public class UserAuthenticationEndpoint {
    private static final Logger log = LoggerFactory.getLogger(UserAuthenticationEndpoint.class);

    private final UserPropertyAndRoleRepository roleRepository;
    private final UserAdminHelper userAdminHelper;
    private final UserAuthenticationService userAuthenticationService;
    private final String hostname;

    @Inject
    private AuditLogRepository auditLogRepository;

    @Inject
    public UserAuthenticationEndpoint(UserPropertyAndRoleRepository roleRepository, UserAdminHelper userAdminHelper,
                                      UserAuthenticationService userAuthenticationService) {
        this.roleRepository = roleRepository;
        this.userAdminHelper = userAdminHelper;
        this.userAuthenticationService = userAuthenticationService;
        this.hostname = getLocalhostName();
    }

    private String getLocalhostName()  {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("", e);
        }
        return "unknown host";
    }

    /*
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response info() {
        Map<String, String> welcomeModel = new HashMap<>(1);
        welcomeModel.put("hostname", hostname);
        return Response.ok(new Viewable("/welcome", welcomeModel)).build();
    }
    */

    /**
     * Authentication using XML. XML must contain an element with name username, and an element with name password.
     * @param input XML input stream.
     * @return XML-encoded identity and role information, or a LogonFailed element if authentication failed.
     */
    @GET
    //@Path("/")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response authenticateUser(InputStream input) {
        log.trace("authenticateUser from XML InputStream");
        UserAuthenticationCredentialDTO dto;
        try {
            dto = UserAuthenticationCredentialDTO.fromXml(input);
        } catch (ParserConfigurationException e) {
            log.error("authenticateUser failed due to internal server error. Returning {}", Response.Status.INTERNAL_SERVER_ERROR, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        } catch (IOException|SAXException |XPathExpressionException e) {
            log.info("authenticateUser failed due to invald client request. Returning {}", Response.Status.BAD_REQUEST, e);
            return Response.status(Response.Status.BAD_REQUEST).entity("<error>Bad request, check client request</error>").build();
        }

        String passwordCredential = dto.getPasswordCredential();
        if (passwordCredential == null) {
            log.trace("Neither password nor facebookId is set. Returning " + Response.Status.FORBIDDEN);
            Viewable entity = new Viewable("/logonFailed.xml.ftl");
            return Response.status(Response.Status.FORBIDDEN).entity(entity).build();
        }
        return authenticateUser(dto.getUsername(), passwordCredential);
    }

    private Response authenticateUser(String username, String password) {
        WhydahUserIdentity id = userAuthenticationService.authenticate(username, password);
        if (id == null)  {
            log.trace("Authentication failed for user with username={}. Returning {}", username, Response.Status.FORBIDDEN.toString());
            Viewable entity = new Viewable("/logonFailed.xml.ftl");
            return Response.status(Response.Status.FORBIDDEN).entity(entity).build();
        }

        List<UserPropertyAndRole> roles = roleRepository.getUserPropertyAndRoles(id.getUid());
        WhydahUser whydahUser = new WhydahUser(id, roles);
        log.info("Authentication ok for user with username={}", username);
        log.debug("Returning WhydahUser TODO ADD XML here!");
        Viewable entity = new Viewable("/user.xml.ftl", whydahUser);
        Response response = Response.ok(entity).build();
        return response;
    }


    /*
     * Form/html-based authentication.
     * @param username Username to be authenticated.
     * @param password Users password.
     * @return XML-encoded identity and role information, or a LogonFailed element if authentication failed.
     */
    /*
    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response authenticateUserForm(@FormParam("username") String username, @FormParam("password") String password) {
        log.debug("authenticateUserForm: user=" + username + ", password=" + password);
        WhydahUserIdentity id = null;
        if (username != null && password != null) {
            id = userAuthenticationService.auth(username, password);
//            if(id == null) {
//                System.out.println("Prøver intern ldap");
//                id = internalLdapAuthenticator.auth(username, password);
//            }
        } else {
            log.warn("Missing user or password");
        }
        if (id == null) {
            return Response.ok(new Viewable("/logonFailed.ftl")).build();
        }
        WhydahUser whydahUser = new WhydahUser(id, roleRepository.getUserPropertyAndRoles(id.getUid()));
        return Response.ok(new Viewable("/user.ftl", whydahUser)).build();
    }
    */

    /*
    //TODO Move to UserResource
    @GET
    @Path("users/{username}/resetpassword")
    public Response resetPassword(@PathParam("username") String username) {
        log.info("Reset password for user {}", username);
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);

            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            userAuthenticationService.resetPassword(username, user.getUid(), user.getEmail());
            return Response.ok().build();
        } catch (Exception e) {
            log.error("resetPassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    //TODO Move to UserResource
    //Copy of changePasswordForUser in UserResource
    @POST
    @Path("users/{username}/newpassword/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changePassword(@PathParam("username") String username, @PathParam("token") String token, String passwordJson) {
        log.info("Changing password for {}", username);
        try {
            WhydahUserIdentity user = userAuthenticationService.getUserinfo(username);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"user not found\"}'").build();
            }

            boolean ok = userAuthenticationService.authenticateWithTemporaryPassword(username, token);

            if (!ok) {
                log.info("Authentication failed while changing password for user {}", username);
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            try {
                JSONObject jsonobj = new JSONObject(passwordJson);
                String newpassword = jsonobj.getString("newpassword");
                userAuthenticationService.changePassword(username, user.getUid(), newpassword);
            } catch (JSONException e) {
                log.error("Bad json", e);
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            log.error("changePassword failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    */

    //TODO Move to UserAdminService (the separate application)
    @POST
    @Path("createandlogon")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response createAndAuthenticateUser(InputStream input) {
        log.trace("createAndAuthenticateUser");

        Document fbUserDoc = parse(input);
        if (fbUserDoc == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
        }

        WhydahUserIdentity userIdentity = UserAdminHelper.createWhydahUserIdentity(fbUserDoc);

        if (userIdentity == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, could not parse input.</error>").build();
        }


        String facebookUserAsString = getFacebookDataAsXmlString(fbUserDoc);
        //String facebookUserAsString = getFacebookDataAsXmlString(input);
        return createAndAuthenticateUser(userIdentity, facebookUserAsString,true);
    }

    //TODO Move to UserAdminService (the separate application)
    static String getFacebookDataAsXmlString(Document fbUserDoc) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.transform(new DOMSource(fbUserDoc), new StreamResult(buffer));
            String original = buffer.toString();

            // Wrap everything in CDATA
            return "<![CDATA[" + original + "]]>";
        } catch (Exception e) {
            log.error("Could not convert Document to string.", e);
            return null;
        }
    }

    static String getFacebookDataAsString(InputStream input) {
        String facebookUserAsString = null;
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(input, Charsets.UTF_8);
            facebookUserAsString = CharStreams.toString(reader);
        } catch (IOException e) {
            log.warn("Error parsing inputStream as string.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.info("Could not close reader.");
                }
            }
        }
        log.debug("facebookUserAsString=" + facebookUserAsString);
        return facebookUserAsString;
    }

    private Document parse(InputStream input) {
        Document fbUserDoc;
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            fbUserDoc = builder.parse(input);
        } catch (Exception e) {
            log.error("Error when creating WhydahUserIdentity from incoming xml stream.", e);
            return null;
        }
        return fbUserDoc;
    }


    //TODO Move to UserAdminService (the separate application)
    Response createAndAuthenticateUser(WhydahUserIdentity userIdentity, String roleValue, boolean reuse) {
        try {
            Response response = userAdminHelper.addUser(userIdentity);
            if (!reuse && response.getStatus() != Response.Status.OK.getStatusCode()) {
                return response;
            }
            if (userIdentity!= null){
                userAdminHelper.addFacebookDataRole(userIdentity, roleValue);
            }

            return authenticateUser(userIdentity.getUsername(), userIdentity.getPassword());
        } catch (Exception e) {
            log.error("createAndAuthenticateUser failed " + userIdentity.toString(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("<error>Server error, check error logs</error>").build();
        }
    }
}