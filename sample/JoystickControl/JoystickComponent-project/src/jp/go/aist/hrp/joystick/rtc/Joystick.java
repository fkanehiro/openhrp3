// -*- Java -*-
/*!
 * @file TkJoystick.java
 * @date $Date$
 *
 * $Id$
 */

package jp.go.aist.hrp.joystick.rtc;

import jp.go.aist.rtm.RTC.Manager;
import jp.go.aist.rtm.RTC.RTObject_impl;
import jp.go.aist.rtm.RTC.RtcDeleteFunc;
import jp.go.aist.rtm.RTC.RtcNewFunc;
import jp.go.aist.rtm.RTC.RegisterModuleFunc;
import jp.go.aist.rtm.RTC.util.Properties;

/*!
 * @class Joystick
 * @brief Sample component for MobileRobotCanvas component
 */
public class Joystick implements RtcNewFunc, RtcDeleteFunc, RegisterModuleFunc {

//  Module specification
//  <rtc-template block="module_spec">
    public static String component_conf[] = {
    	    "implementation_id", "Joystick",
    	    "type_name",         "Joystick",
    	    "description",       "Sample component for MobileRobotCanvas component",
    	    "version",           "1.0.0",
    	    "vendor",            "HRG team",
    	    "category",          "Sample",
    	    "activity_type",     "DataFlowComponent",
    	    "max_instance",      "0",
    	    "language",          "Java",
    	    "lang_type",         "compile",
    	    "exec_cxt.periodic.rate", "1000.0",
    	    ""
            };
//  </rtc-template>

    public RTObject_impl createRtc(Manager mgr) {
        return new JoystickImpl(mgr);
    }

    public void deleteRtc(RTObject_impl rtcBase) {
        rtcBase = null;
    }
    public void registerModule() {
        Properties prop = new Properties(component_conf);
        final Manager manager = Manager.instance();
        manager.registerFactory(prop, new Joystick(), new Joystick());
    }
}
