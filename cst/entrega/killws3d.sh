#! /bin/bash
echo "Killing WS3D"
pkill -9 -f "WorldServer3D.jar"
pkill -9 -f "DemoLIDA.jar"
rm -f *.so
