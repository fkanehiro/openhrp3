#!/bin/sh

rtc-template -bpython \
    --module-name=TkJoyStick --module-type='DataFlowComponent' \
    --module-desc='Sample component for MobileRobotCanvas component' \
    --module-version=1.0 --module-vendor='Noriaki Ando and Shinji Kurihara' \
    --module-category=example \
    --module-comp-type=DataFlowComponent --module-act-type=SPORADIC \
    --module-max-inst=10 \
    --outport=pos:TimedFloatSeq \
    --outport=vel:TimedFloatSeq

