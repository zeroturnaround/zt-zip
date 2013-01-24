if [[ "$1" = "-h" ]] || [[ "$1" = "--help" ]]; then
  echo "Usage: './get_release_commands.sh VERSION_TO_RELEASE'"
  echo "Prints out commands to run to release zt-zip to Maven Central. VERSION_TO_RELEASE must be a number (integer or floating)"
  exit 0
fi

if ! [[ "$1" =~ ^[0-9]*\.?[0-9]*$ ]] || [[ "$1z" = "z" ]]; then
  echo "Not a valid version number given, use -h to get usage options"
  exit 1
fi

echo "To release zt-zip run these commands:"

echo "mvn javadoc:jar;  mvn source:jar"
echo "mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=pom.xml -Dfile=target/zt-zip-$1.jar"
echo "mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=pom.xml -Dfile=target/zt-zip-$1-sources.jar -Dclassifier=sources"
echo "mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=pom.xml -Dfile=target/zt-zip-$1-javadoc.jar -Dclassifier=javadoc"
