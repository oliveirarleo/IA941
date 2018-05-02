#!/bin/sh
java -jar ws3d/WorldServer3D.jar &
sleep 2
java -jar DemoJSOAR/dist/DemoJSOAR.jar
