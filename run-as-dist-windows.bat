move conf\application.conf conf\tmp
type conf\tmp > conf\application2.conf
type conf\prod.conf >> conf\application2.conf
type conf\application2.conf | findstr /v include > conf\application.conf
CALL sbt dist
del conf\application.conf
del conf\application2.conf
move conf\tmp conf\application.conf 
 
cd dist
jar xf docear-frontend-0.1-SNAPSHOT.zip
cd docear-frontend-0.1-SNAPSHOT
echo java %1 -cp "./lib/*;" play.core.server.NettyServer . -Dlogger.resource=prod-logger.xml > start.bat
start.bat