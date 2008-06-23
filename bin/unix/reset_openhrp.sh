#!/bin/sh

for P in  `ps ax --columns 200 | grep ModelLoader | cut -f 1 -d ' '`; do echo $P; kill -9 $P; done
#killall -9 java
killall -9 omniNames
killall -9 DynamicsSimulator
killall -9 CollisionDetector
killall -9 hrpsys
