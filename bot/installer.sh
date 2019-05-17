add-apt-repository ppa:webupd8team/java
apt-get update -y
apt-get install oracle-java8-installer
wget http://www-eu.apache.org/dist/maven/maven-3/3.6.1/binaries/apache-maven-3.6.1-bin.tar.gz
tar -xvzf apache-maven-3.6.1-bin.tar.gz
rm apache-maven-3.6.1-bin.tar.gz
mv apache-maven-3.6.1 /opt/maven
printf "export M2_HOME=/opt/maven" >> /etc/profile.d/mavenenv.sh
printf "export PATH=\${M2_HOME}/bin:\${PATH}" >> /etc/profile.d/mavenenv.sh
chmod +x /etc/profile.d/mavenenv.sh
source /etc/profile.d/mavenenv.sh

mvn package

java -jar target/discord-bot*.jar
