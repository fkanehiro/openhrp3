// -*- Java -*-
/*!
 * @file  PathConsumerImpl.java
 * @brief Path Planner Component
 * @date  $Date$
 *
 * $Id$
 */

package jp.go.aist.hrp.simulator;

import com.generalrobotix.ui.item.GrxPathPlanningAlgorithmItem;

import RTC.ComponentProfile;
import RTC.ConnectorProfile;
import RTC.ConnectorProfileHolder;
import RTC.ExecutionContext;
import RTC.LifeCycleState;
import RTC.PortService;
import RTC.RTObject;
import RTC.ReturnCode_t;
import jp.go.aist.hrp.simulator.PathPlanner;
import jp.go.aist.rtm.RTC.DataFlowComponentBase;
import jp.go.aist.rtm.RTC.Manager;
import jp.go.aist.rtm.RTC.port.ConnectionCallback;
import jp.go.aist.rtm.RTC.port.CorbaConsumer;
import jp.go.aist.rtm.RTC.port.CorbaPort;

public class PathConsumerImpl extends DataFlowComponentBase{

	public PathConsumerImpl(Manager manager) {  
        super(manager);
        // <rtc-template block="initializer">
        m_PathPlannerPort = new CorbaPort("PathPlanner");
        // </rtc-template>

        // Registration: InPort/OutPort/Service
        // <rtc-template block="registration">
        // Set InPort buffers
        
        // Set OutPort buffer
        
        // Set service provider to Ports
        
        // Set service consumers to Ports
        m_PathPlannerPort.registerConsumer("Path", "PathPlanner", m_PathBase);
        m_PathPlannerPort.setOnConnected( new ConnectionCallback() {
			public void run(ConnectorProfileHolder arg0) {
				ConnectorProfile[] connectorProfiles = m_PathPlannerPort.get_connector_profiles();
				for(ConnectorProfile connectorProfile : connectorProfiles){
					PortService[] portServices = connectorProfile.ports;
					for(PortService portService : portServices){
						RTObject rtObject = portService.get_port_profile().owner;
						String typeName = rtObject.get_component_profile().type_name;
						if(typeName.equals("Path")){
							ExecutionContext[] executionContexts = rtObject.get_owned_contexts();
							for(ExecutionContext executionContext : executionContexts){
								if(executionContext.get_component_state(rtObject) != LifeCycleState.ACTIVE_STATE){
									if(executionContext.activate_component(rtObject) == ReturnCode_t.RTC_OK)
										;
									else
										item_.connectedCallback(false);
								}
								item_.connectedCallback(true);
								return;
							}

						}
					}
				}
				item_.connectedCallback(false);
			}
        });
        m_PathPlannerPort.setOnDisconnected( new ConnectionCallback() {
			public void run(ConnectorProfileHolder arg0) {
				item_.connectedCallback(false);
			}
        });
        
        // Set CORBA Service Ports
        registerPort(m_PathPlannerPort);
        
        // </rtc-template>
    }

    // The initialize action (on CREATED->ALIVE transition)
    // formaer rtc_init_entry() 
//    @Override
//    protected ReturnCode_t onInitialize() {
//        return super.onInitialize();
//    }
    // The finalize action (on ALIVE->END transition)
    // formaer rtc_exiting_entry()
//    @Override
//    protected ReturnCode_t onFinalize() {
//        return super.onFinalize();
//    }
    //
    // The startup action when ExecutionContext startup
    // former rtc_starting_entry()
//    @Override
//    protected ReturnCode_t onStartup(int ec_id) {
//        return super.onStartup(ec_id);
//    }
    //
    // The shutdown action when ExecutionContext stop
    // former rtc_stopping_entry()
//    @Override
//    protected ReturnCode_t onShutdown(int ec_id) {
//        return super.onShutdown(ec_id);
//    }
    //
    // The activated action (Active state entry action)
    // former rtc_active_entry()
//    @Override
//    protected ReturnCode_t onActivated(int ec_id) {
//        return super.onActivated(ec_id);
//    }
    //
    // The deactivated action (Active state exit action)
    // former rtc_active_exit()
//    @Override
//    protected ReturnCode_t onDeactivated(int ec_id) {
//        return super.onDeactivated(ec_id);
//    }
    //
    // The execution action that is invoked periodically
    // former rtc_active_do()
//    @Override
//    protected ReturnCode_t onExecute(int ec_id) {
//        return super.onExecute(ec_id);
//    }
    //
    // The aborting action when main logic error occurred.
    // former rtc_aborting_entry()
//  @Override
//  public ReturnCode_t onAborting(int ec_id) {
//      return super.onAborting(ec_id);
//  }
    //
    // The error action in ERROR state
    // former rtc_error_do()
//    @Override
//    public ReturnCode_t onError(int ec_id) {
//        return super.onError(ec_id);
//    }
    //
    // The reset action that is invoked resetting
    // This is same but different the former rtc_init_entry()
//    @Override
//    protected ReturnCode_t onReset(int ec_id) {
//        return super.onReset(ec_id);
//    }
    //
    // The state update action that is invoked after onExecute() action
    // no corresponding operation exists in OpenRTm-aist-0.2.0
//    @Override
//    protected ReturnCode_t onStateUpdate(int ec_id) {
//        return super.onStateUpdate(ec_id);
//    }
    //
    // The action that is invoked when execution context's rate is changed
    // no corresponding operation exists in OpenRTm-aist-0.2.0
//    @Override
//    protected ReturnCode_t onRateChanged(int ec_id) {
//        return super.onRateChanged(ec_id);
//    }
//
    // DataInPort declaration
    // <rtc-template block="inport_declare">
    
    // </rtc-template>

    // DataOutPort declaration
    // <rtc-template block="outport_declare">
    
    // </rtc-template>

    // CORBA Port declaration
    // <rtc-template block="corbaport_declare">
    protected CorbaPort m_PathPlannerPort;
    
    // </rtc-template>

    // Service declaration
    // <rtc-template block="service_declare">
    
    // </rtc-template>

    // Consumer declaration
    // <rtc-template block="consumer_declare">
    protected CorbaConsumer<PathPlanner> m_PathBase = new CorbaConsumer<PathPlanner>(PathPlanner.class);
    protected PathPlanner m_Path;
  
    private GrxPathPlanningAlgorithmItem item_ = null;

	public void setConnectedCallback(GrxPathPlanningAlgorithmItem item) {
		item_ = item;
	}
}
