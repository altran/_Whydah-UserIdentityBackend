package net.whydah.identity.user.authentication;

import net.whydah.identity.application.ApplicationService;
import net.whydah.sso.application.types.Application;
import net.whydah.sso.application.types.ApplicationCredential;
import net.whydah.sso.commands.userauth.CommandGetUsertokenByUsertokenId;
import net.whydah.sso.session.WhydahApplicationSession;
import net.whydah.sso.user.mappers.UserTokenMapper;
import net.whydah.sso.user.types.UserToken;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class SecurityTokenServiceClient {
    private static final Logger log = LoggerFactory.getLogger(SecurityTokenServiceClient.class);

    private static final String MY_APPLICATION_ID="2210";
    private static WhydahApplicationSession was = null;
    private String sts;

    @Autowired
    @Configure
    public SecurityTokenServiceClient(@Configuration("securitytokenservice") String usertokenservice) {
        this.sts=usertokenservice;
    }

    public String getActiveUibApplicationTokenId(){
        if (was==null){
            was=WhydahApplicationSession.getInstance(sts,getAppCredentialForApplicationId(MY_APPLICATION_ID));
        }
        return was.getActiveApplicationTokenId();
    }

    public UserToken getUserToken(String usertokenid){
        if (was==null){
            was= WhydahApplicationSession.getInstance(sts,getAppCredentialForApplicationId(MY_APPLICATION_ID));
        }
        String userToken = new CommandGetUsertokenByUsertokenId(URI.create(was.getSTS()),was.getActiveApplicationTokenId(),was.getActiveApplicationTokenXML(),usertokenid).execute();
        if (userToken!=null && userToken.length()>10) {
            log.debug("usertoken: {}", userToken);
            return UserTokenMapper.fromUserTokenXml(userToken);
        }
        log.error("getUserToken failed - URI:{}, usertokenid:{}",sts, usertokenid);
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