#! /bin/bash
echo "Killing WS3D"
pkill -9 -f "./ws3d/dist/WorldServer3D.jar"
rm -f *.so