---------------------------------------------------------------
-- Utillity script to create initial database.             ---
-- Master will be in DatabaseHelper class.     ---
---------------------------------------------------------------

-- UserRoles --
CREATE TABLE UserRoles (
  RoleID char(255) AS PRIMARY KEY,
  UserID char(255) NOT NULL,
  AppID varchar(255),
  OrganizationName varchar(255),
  RoleName varchar(255),
  RoleValues varchar(256)
);

-- Applications --
CREATE TABLE Applications (
  ID varchar(32) AS PRIMARY KEY,
  Name varchar(255),
  DefaultRoleName varchar(255) default null,
  DefaultOrgName varchar(255) default null,
  ApplicationSecret varchar(2048) default null
);

-- Organization
CREATE TABLE Organization (
  ID varchar(32),
  Name varchar(255)
);

-- Roles --
CREATE TABLE Roles (
  ID varchar(32),
  Name varchar(255)
);

-- AuditLog --
CREATE TABLE AUDITLOG (
  ID INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  userid varchar(255),
  timestamp varchar(20),
  action varchar(32),
  field varchar(255),
  value varchar(4096)
);





