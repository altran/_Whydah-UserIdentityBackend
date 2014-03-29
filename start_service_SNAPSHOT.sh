#!/bin/sh

export IAM_MODE=TEST

A=UserIdentityBackend
V=LATEST
JARFILE=$A-$V.jar

pkill -f $A

wget --user=altran --password=l1nkSys -O $JARFILE "http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=snapshots&g=net.whydah.identity&a=$A&v=$V&p=jar"
nohup java -jar -DIAM_CONFIG=useridentitybackend.TEST.properties $JARFILE &

tail -f nohup.out
