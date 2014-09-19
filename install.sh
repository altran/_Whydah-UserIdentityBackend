#/bin/bash!
#Erik - find one of the more advanced scripts :)

export IAM_MODE=TEST

A=UserIdentityBackend
V=0.4-SNAPSHOT
JARFILE=$A-$V.jar

pkill -f $A

wget  -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=altran-snapshots&g=net.whydah.identity&a=$A&v=$V&p=jar"
java -jar -DIAM_CONFIG=/var/whydah/config/useridentitybackend.TEST.properties $JARFILE &
