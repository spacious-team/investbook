# Задать путь к распакованному архиву с Java
#export JAVA_HOME=/opt/jdk-15

[ -n "$JAVA_HOME" ] && export PATH=$JAVA_HOME/bin:$PATH
java -jar *.jar