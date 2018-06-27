#! /bin/bash
echo "Running"
java -jar "./WorldServer3D.jar" &
sleep 3
java -jar "./DemoLIDA.jar"
