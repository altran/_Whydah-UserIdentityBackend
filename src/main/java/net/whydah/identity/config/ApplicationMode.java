package net.whydah.identity.config;


/**
 * Mange Constretto configuration tags and get application mode from os environment or system property.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-05-28
 */
public class ApplicationMode {
    public static final String DEV_MODE = "dev";
    public static final String NO_SECURITY_FILTER = "noSecurityFilter";
    private static final String CONSTRETTO_TAGS = "CONSTRETTO_TAGS";

    public static boolean skipSecurityFilter() {
        String tags = System.getenv(CONSTRETTO_TAGS);
        if (tags == null || tags.isEmpty()) {
            tags = System.getProperty(CONSTRETTO_TAGS);
        }
        return tags != null && tags.contains(NO_SECURITY_FILTER);
    }

    public static void setDevMode() {
        setTags(DEV_MODE);
    }

    public static void setTags(String... tags) {
        String tagsAsString = String.join(",", tags);
        System.setProperty(CONSTRETTO_TAGS, tagsAsString);
    }

    public static void clearTags() {
        System.clearProperty(CONSTRETTO_TAGS);
    }

    /*
    public final static String IAM_MODE_KEY = "IAM_MODE";
    public final static String PROD = "PROD";
    public final static String TEST = "TEST";
    public final static String TEST_L = "TEST_LOCALHOST";
    public final static String DEV = "DEV";


    public static String getApplicationMode() {
        String appMode = System.getenv(IAM_MODE_KEY);
        if(appMode == null) {
            appMode = System.getProperty(IAM_MODE_KEY);
        }
        if(appMode == null) {
            System.err.println(IAM_MODE_KEY + " not defined. Must be one of PROD, TEST, DEV.");
            System.exit(4);
        }
        if(!Arrays.asList(PROD, TEST, TEST_L, DEV).contains(appMode)) {
            System.err.println("Unknown " + IAM_MODE_KEY + ": " + appMode);
            System.exit(5);
        }
        //System.out.println(String.format("Running in %s mode", appMode));
        return appMode;
    }
    */
}
