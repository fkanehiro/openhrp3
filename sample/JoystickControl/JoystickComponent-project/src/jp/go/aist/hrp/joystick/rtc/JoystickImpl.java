// -*- Java -*-
/*!
 * @file  TkJoystickImpl.java
 * @brief Sample component for MobileRobotCanvas component
 * @date  $Date$
 *
 * $Id$
 */

package jp.go.aist.hrp.joystick.rtc;

import org.eclipse.swt.graphics.Point;

import jp.go.aist.hrp.joystick.views.joystickView;
import jp.go.aist.rtm.RTC.DataFlowComponentBase;
import jp.go.aist.rtm.RTC.Manager;
import jp.go.aist.rtm.RTC.port.OutPort;
import jp.go.aist.rtm.RTC.util.DataRef;
import RTC.ReturnCode_t;
import RTC.TimedFloatSeq;
import RTC.Time;


/*!
 * @class JoystickImpl
 * @brief Sample component for MobileRobotCanvas component
 *
 */
public class JoystickImpl extends DataFlowComponentBase {

  /*!
   * @brief constructor
   * @param manager Maneger Object
   */
	public JoystickImpl(Manager manager) {  
        super(manager);
    }

    /**
     *
     * The initialize action (on CREATED->ALIVE transition)
     * formaer rtc_init_entry() 
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onInitialize() {
    	
        // <rtc-template block="initializer">
        m_pos_val = new TimedFloatSeq(new Time(0,0), new float[2]);
        m_pos = new DataRef<TimedFloatSeq>(m_pos_val);
        m_posOut = new OutPort<TimedFloatSeq>("pos", m_pos);
        m_vel_val = new TimedFloatSeq(new Time(0,0), new float[2]);
        m_vel = new DataRef<TimedFloatSeq>(m_vel_val);
        m_velOut = new OutPort<TimedFloatSeq>("vel", m_vel);
        // </rtc-template>

        // Registration: InPort/OutPort/Service
        // <rtc-template block="registration">
        // Set InPort buffers
        
        // Set OutPort buffer
        try {
			registerOutPort(TimedFloatSeq.class, "pos", m_posOut);
			registerOutPort(TimedFloatSeq.class, "vel", m_velOut);
        } catch (Exception e) {
			e.printStackTrace();
		}
       
        // Set service provider to Ports
        
        // Set service consumers to Ports
        
        // Set CORBA Service Ports
        
        // </rtc-template>
        
        return ReturnCode_t.RTC_OK;
    }

    /***
     *
     * The finalize action (on ALIVE->END transition)
     * formaer rtc_exiting_entry()
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onFinalize() {
    	System.out.println("Joystick Finalized");
        return super.onFinalize();
    }

    /***
     *
     * The startup action when ExecutionContext startup
     * former rtc_starting_entry()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onStartup(int ec_id) {
    	System.out.println("Joystick Started");
        return super.onStartup(ec_id);
    }

    /***
     *
     * The shutdown action when ExecutionContext stop
     * former rtc_stopping_entry()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onShutdown(int ec_id) {
    	System.out.println("Joystick Shutting down");
        return super.onShutdown(ec_id);
    }

    /***
     *
     * The activated action (Active state entry action)
     * former rtc_active_entry()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onActivated(int ec_id) {
    	System.out.println("Joystick Activated");
        return super.onActivated(ec_id);
    }

    /***
     *
     * The deactivated action (Active state exit action)
     * former rtc_active_exit()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onDeactivated(int ec_id) {
    	System.out.println("Joystick Deactivated");
        return super.onDeactivated(ec_id);
    }

    /***
     *
     * The execution action that is invoked periodically
     * former rtc_active_do()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onExecute(int ec_id) {
    	
    	Point p = joystickView.getJoystickPosition();
    	float[] pos = {p.x,p.y};
    	float[] vel = this.convert(p);
    	m_pos_val.data = pos;
    	m_vel_val.data = vel;
    	m_posOut.write();
    	m_velOut.write();
    	
//    	System.out.println("("+p.x+", "+p.y+"), ("+vel[0]+", "+vel[1]+")");
    	
        return ReturnCode_t.RTC_OK;
    }

    
    /**
     *  Calculating linear and angular output velocity 
     *  
     * @param 
     * @return Array that contains velocity factors
     */
    protected float[] convert(Point p) {
    	double k=1.0;
    	double _th = Math.atan2(p.y,p.x);
    	double _v  = k * Math.hypot(p.x, p.y);
    	double _vl = _v * Math.cos(_th - (Math.PI/4.0));
    	double _vr = _v * Math.sin(_th - (Math.PI/4.0));
    	if(_vr==-0.0) _vr*=-1;
    	float[] v = {(float) _vl, (float) _vr};
    	return v;
    }

    /***
     *
     * The aborting action when main logic error occurred.
     * former rtc_aborting_entry()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
  @Override
  public ReturnCode_t onAborting(int ec_id) {
	  System.out.println("Joystick Aborted");
      return super.onAborting(ec_id);
  }

    /***
     *
     * The error action in ERROR state
     * former rtc_error_do()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    public ReturnCode_t onError(int ec_id) {
    	System.out.println("Joystick Error : "+ec_id);
        return super.onError(ec_id);
    }

    /***
     *
     * The reset action that is invoked resetting
     * This is same but different the former rtc_init_entry()
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onReset(int ec_id) {
    	System.out.println("Joystick Reset");
        return super.onReset(ec_id);
    }

    /***
     *
     * The state update action that is invoked after onExecute() action
     * no corresponding operation exists in OpenRTm-aist-0.2.0
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onStateUpdate(int ec_id) {
        return super.onStateUpdate(ec_id);
    }

    /***
     *
     * The action that is invoked when execution context's rate is changed
     * no corresponding operation exists in OpenRTm-aist-0.2.0
     *
     * @param ec_id target ExecutionContext Id
     *
     * @return RTC::ReturnCode_t
     * 
     * 
     */
    @Override
    protected ReturnCode_t onRateChanged(int ec_id) {
        return super.onRateChanged(ec_id);
    }

    // DataInPort declaration
    // <rtc-template block="inport_declare">
    
    // </rtc-template>

    // DataOutPort declaration
    // <rtc-template block="outport_declare">
    
    protected TimedFloatSeq m_pos_val;
    protected DataRef<TimedFloatSeq> m_pos;
    /*!
     * ジョイスティックコンポーネントの出力先
     * - Type: TimedFloatSeq
     * - Semantics: ジョイスティックコンポーネントの座標
     */
    protected OutPort<TimedFloatSeq> m_posOut;

    protected TimedFloatSeq m_vel_val;
    protected DataRef<TimedFloatSeq> m_vel;
    /*!
     * ジョイスティックコンポーネントの出力先
     * - Type: TimedFloatSeq
     * - Semantics: ジョイスティックコンポーネントの速度
     */
    protected OutPort<TimedFloatSeq> m_velOut;
    
    // </rtc-template>

    // CORBA Port declaration
    // <rtc-template block="corbaport_declare">
    
    // </rtc-template>

    // Service declaration
    // <rtc-template block="service_declare">
    
    // </rtc-template>

    // Consumer declaration
    // <rtc-template block="consumer_declare">
    
    // </rtc-template>


}
