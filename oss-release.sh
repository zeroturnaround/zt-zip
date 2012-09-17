#!/bin/bash

# need to automate this, but right now I'm writing this as notes

#mvn javadoc:jar  mvn source:jar

#mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=pom.xml -Dfile=target/zt-zip-1.5.jar

#mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=pom.xml -Dfile=target/zt-zip-1.5-sources.jar -Dclassifier=sources

#mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=pom.xml -Dfile=target/zt-zip-1.5-javadoc.jar -Dclassifier=javadoc
