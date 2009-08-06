// -*- Java -*-
/*!
 * @file PathConsumer.java
 * @date $Date$
 *
 * $Id$
 */
package jp.go.aist.hrp.simulator;

import jp.go.aist.rtm.RTC.Manager;
import jp.go.aist.rtm.RTC.RTObject_impl;
import jp.go.aist.rtm.RTC.RtcDeleteFunc;
import jp.go.aist.rtm.RTC.RtcNewFunc;

public class PathConsumer implements RtcNewFunc, RtcDeleteFunc {

//  Module specification
//  <rtc-template block="module_spec">
    public static String component_conf[] = {
    	    "implementation_id", "PathConsumer",
    	    "type_name",         "PathConsumer",
    	    "description",       "Path Planner Component",
    	    "version",           "0.9",
    	    "vendor",            "S-cubed, Inc.",
    	    "category",          "Generic",
    	    "activity_type",     "DataFlowComponent",
    	    "max_instance",      "10",
    	    "language",          "Java",
    	    "lang_type",         "compile",
    	    ""
            };
//  </rtc-template>

    public RTObject_impl createRtc(Manager mgr) {
        return new PathConsumerImpl(mgr);
    }

    public void deleteRtc(RTObject_impl rtcBase) {
        rtcBase = null;
    }
}
