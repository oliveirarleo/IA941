#! /bin/bash
echo "Running WS3D"
java -jar "./ws3d/WorldServer3D.jar"&

sleep 3

echo "Running ClarionApp"
./ClarionApp/ClarionApp.exe