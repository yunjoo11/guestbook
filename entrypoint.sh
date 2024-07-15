#!/bin/bash

if [ -z $SCOUTER_IP ] ; then
	exec java -jar guestbook.jar
else
	exec java -javaagent:/scouter/agent.java/scouter.agent.jar -Dnet_collector_ip=$SCOUTER_IP -Dobj_type=tomcat -jar guestbook.jar
fi