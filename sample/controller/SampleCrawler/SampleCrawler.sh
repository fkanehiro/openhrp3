#!/bin/sh

openhrp-controller-bridge \
--server-name SampleCrawlerController \
--in-port torque:JOINT_TORQUE \
--connection torque:SampleCrawler0:torque
