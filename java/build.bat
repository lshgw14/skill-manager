@echo off

echo Building Java project with Maven...
cd "%~dp0"
mvn clean package

echo Build completed!
echo Run the following command to execute the CSV to JSON converter:
echo java -jar target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar

echo Run the following command to execute the Sync Skills script:
echo java -cp target/skill-manager-1.0-SNAPSHOT-jar-with-dependencies.jar com.skillmanager.SyncSkills

pause