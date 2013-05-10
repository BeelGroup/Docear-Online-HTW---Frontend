set -x

export xsbt="java -Xmx1024M -Xss10M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=1024M  -jar sbtwrapper/sbt-launch.jar -Dsbt.log.noformat=true"
xvfb-run $xsbt clean test
