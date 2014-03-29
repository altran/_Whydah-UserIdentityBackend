package net.whydah.identity.dataimport;

import com.google.inject.Inject;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * @author <a href="mailto:erik.drolshammer@altran.com">Erik Drolshammer</a>
 * @since 10/19/12
 */
public class DatabaseHelper {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);
    private final QueryRunner queryRunner;

    public enum DB_DIALECT {HSSQL, MYSQL}

    @Inject
    public DatabaseHelper(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public void initDB(DB_DIALECT dialect) {
        createDatabaseTables(dialect);
    }

    private void createDatabaseTables(DB_DIALECT dialect) {
        logger.info("Creating UserPropertyAndRole tables.");
        try {
            if(dialect == DB_DIALECT.HSSQL) {
                queryRunner.update("CREATE TABLE UserRoles (" +
                        "  ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY," +
                        "  UserID char(36) NOT NULL," +
                        "  AppID varchar(32)," +
                        "  OrganizationId varchar(32)," +
                        "  RoleName varchar(32)," +
                        "  RoleValues varchar(256)" +
                        ")");
            }else if(dialect == DB_DIALECT.MYSQL) {
                queryRunner.update("CREATE TABLE UserRoles (\n" +
                        " ID MEDIUMINT AUTO_INCREMENT,\n" +
                        "   UserID char(36) NOT NULL,\n" +
                        "   AppID varchar(32),\n" +
                        "   OrganizationId varchar(32),\n" +
                        "   RoleName varchar(32),\n" +
                        "   RoleValues varchar(256),\n" +
                        "    PRIMARY KEY (ID)\n" +
                        " )\n");
            }
            queryRunner.update("CREATE TABLE Applications (" +
                    "  ID varchar(32)," +
                    "  Name varchar(128)," +
                    "  DefaultRole varchar(30) default null," +
                    "  DefaultOrgid varchar(30) default null" +
                    ")");
            queryRunner.update("CREATE TABLE Organization (" +
                    "  ID varchar(32)," +
                    "  Name varchar(128)" +
                    ")");
            queryRunner.update("CREATE TABLE Roles (" +
                    "  ID char(32)," +
                    "  Name varchar(128)" +
                    ")");
            if(dialect == DB_DIALECT.HSSQL) {
                queryRunner.update("CREATE TABLE AUDITLOG (" +
                        "  ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY," +
                        "  userid varchar(36)," +
                        "  timestamp varchar(20)," +
                        "  action varchar(32)," +
                        "  field varchar(32)," +
                        "  value varchar(256)" +
                        ")");
            }
            else if(dialect == DB_DIALECT.MYSQL) {
                queryRunner.update("CREATE TABLE AUDITLOG (\n" +
                        " ID MEDIUMINT,\n" +
                        " userid varchar(36),\n" +
                        " timestamp varchar(20),\n" +
                        " action varchar(32),\n" +
                        " field varchar(32),\n" +
                        " value varchar(256),\n" +
                        " PRIMARY KEY(ID)\n" +
                        ")");
            }
        } catch (SQLException e) {
            logger.info("Error creating tables", e);
        }
    }
}