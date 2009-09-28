package com.generalrobotix.ui.depends.rtm;

import java.util.Vector;

import jp.go.aist.hrp.simulator.ClockGeneratorPOA;
import OpenRTM.ExtTrigExecutionContextService;

import com.generalrobotix.ui.depends.ISwitchDependVer;

public abstract class ClockGeneratorRtm100_impl extends ClockGeneratorPOA
    implements ISwitchDependVer
{
    protected Vector<ExecutionContext> ecs_;

    /**
     * @brief This class manages execution timing of execution context
     */
    protected class ExecutionContext {
        public ExtTrigExecutionContextService ec_;
        private double period_;
        private double nextExecutionTime_;
        
        /**
         * @brief constructor
         * @param ec execution context
         * @param period ExtTrigExecutionContextService.tick() is called with this period
         */
        ExecutionContext(ExtTrigExecutionContextService ec, double period){
            ec_ = ec;
            period_ = period;
            reset();
        }
        
        /**
         * @param t current time in the simulation world
         * @return true if executed successfully, false otherwise
         */
        boolean execute(double t){
            if (t >= nextExecutionTime_){
                try{
                    ec_.tick();
                    nextExecutionTime_ += period_;
                }catch(Exception ex){
                    return false;
                }
            }
            return true;
        }

        /**
         * @brief reset
         */
        void reset(){
            nextExecutionTime_ = 0;
        }
    }
    
    /**
     * @brief constructor
     */
    ClockGeneratorRtm100_impl(){
        ecs_ = new Vector<ExecutionContext>();
    }
    
    /**
     * @brief register an execution context with its execution period
     * @param ec execution context
     * @param period execution period
     */
    public void subscribe(ExtTrigExecutionContextService ec, double period) {
        System.out.println("ClockGenerator::subscribe("+ec+", "+period);
        ecs_.add(new ExecutionContext(ec, period));
    }

    /**
     * @brief remove execution context from execution list
     * @param ec execution context
     */
    public void unsubscribe(ExtTrigExecutionContextService ec) {
        System.out.println("ClockGenerator::unsubscribe("+ec+")");
        for (int i=0; i<ecs_.size(); i++){
            ExecutionContext ec2 = ecs_.get(i);
            if (ec._is_equivalent(ec2.ec_)){
                ecs_.remove(ec2);
                return;
            }
        }
    }
    
    public String getDependencyModuleVersion()
    {
        return new String("1.0.0-RC1");
    }
}
