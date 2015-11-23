package net.whydah.identity.user.authentication;

import net.whydah.identity.application.ApplicationDao;
import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.mappers.ApplicationCredentialMapper;
import net.whydah.sso.application.mappers.ApplicationTokenMapper;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.application.types.ApplicationToken;
import net.whydah.sso.commands.appauth.CommandLogonApplication;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

@Component
public class SecurityTokenServiceHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceHelper.class);
    private Client client = ClientBuilder.newClient();

    private final WebTarget tokenServiceResource;
    private ApplicationCredential uibAppCredential;
    private static ApplicationToken uibApplicationToken;

    @Autowired
    @Configure
    public SecurityTokenServiceHelper(@Configuration("securitytokenservice") String usertokenserviceUri) {
        tokenServiceResource = client.target(usertokenserviceUri);
    }

    public String getActiveUibApplicationTokenId(){
        if (uibApplicationToken!=null){
            return uibApplicationToken.getApplicationTokenId();
        }
        return null;
    }
    public UserToken getUserToken(String appTokenId, String usertokenid){
        if (uibApplicationToken==null){
            // TODO - get the real values here
            uibAppCredential =getAppCredentialForApplicationId("2210");
            log.debug("SecurityTokenServiceHelper CommandLogonApplication( {}, {} )",tokenServiceResource.getUri(), ApplicationCredentialMapper.toXML(uibAppCredential));
            uibApplicationToken = ApplicationTokenMapper.fromXml(new CommandLogonApplication(tokenServiceResource.getUri(), uibAppCredential).execute());
            if (uibApplicationToken!=null){
                log.info("STS session started, applicationTokenID="+uibApplicationToken.getApplicationTokenId());
                log.warn("###  getUserToken CommandGetUsertokenByUsertokenId( {}, {}, {}, {} )",tokenServiceResource.getUri(),  uibApplicationToken.getApplicationTokenId(),ApplicationTokenMapper.toXML(uibApplicationToken), usertokenid);
                String userToken = new CommandGetUsertokenByUsertokenId(tokenServiceResource.getUri(),  uibApplicationToken.getApplicationTokenId(),ApplicationTokenMapper.toXML(uibApplicationToken), usertokenid).execute();
                if (userToken!=null && userToken.length()>10) {
                    log.debug("usertoken: {}", userToken);
                    return new UserToken(userToken);
                } else {
                    uibApplicationToken=null;  // Reset UAS application session
                }
            } else {
                uibApplicationToken=null;  // Reset UAS application session
                log.warn("STS session start failed");
            }
        } else {
            // Re-use application session
            log.warn("#  getUserToken CommandGetUsertokenByUsertokenId( {}, {}, {}, {} )",tokenServiceResource.getUri(),  uibApplicationToken.getApplicationTokenId(),ApplicationTokenMapper.toXML(uibApplicationToken), usertokenid);
            String userToken = new CommandGetUsertokenByUsertokenId(tokenServiceResource.getUri(),  uibApplicationToken.getApplicationTokenId(),ApplicationTokenMapper.toXML(uibApplicationToken), usertokenid).execute();
            if (userToken!=null && userToken.length()>10) {
                log.debug("usertoken: {}", userToken);
                return new UserToken(userToken);
            } else {
                uibApplicationToken=null;  // Reset UAS application session
            }

        }
        uibApplicationToken=null;  // Reset UAS application session
        log.error("getUserToken failed - resetting uas application session - URI:{}, usertokenid:{}",tokenServiceResource.getUri(), usertokenid);
        return null;
    }

    private ApplicationCredential getAppCredentialForApplicationId(String appNo){
        ApplicationService applicationService = ApplicationService.getApplicationService();
        Application app = applicationService.getApplication(appNo);
        String appid = app.getId();
        String appname=app.getName();
        String secret=app.getSecurity().getSecret();
        return new ApplicationCredential(appid,appname,secret);
    }
}
