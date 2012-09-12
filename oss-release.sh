#!/bin/bash

# need to automate this, but right now I'm writing this as notes

#$ mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=ossrh-test-1.2.pom -Dfile=ossrh-test-1.2.jar
#$ mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=ossrh-test-1.2.pom -Dfile=ossrh-test-1.2-sources.jar -Dclassifier=sources
#$ mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=ossrh-test-1.2.pom -Dfile=ossrh-test-1.2-javadoc.jar -Dclassifier=javadoc
