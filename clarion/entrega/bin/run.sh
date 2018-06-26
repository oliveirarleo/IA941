#! /bin/bash
echo "Running WS3D"
java -jar "./ws3d/WorldServer3D.jar"&

sleep 3
java -jar "jSoar/DemoJSOAR.jar" &

sleep 5
echo "Running ClarionApp"
./ClarionApp/ClarionApp.exe 
