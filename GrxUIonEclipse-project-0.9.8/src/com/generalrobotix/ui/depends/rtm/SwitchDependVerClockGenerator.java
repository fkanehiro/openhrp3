package com.generalrobotix.ui.depends.rtm;

import com.generalrobotix.ui.depends.rtm.ClockGeneratorRtm_impl;

public class SwitchDependVerClockGenerator extends ClockGeneratorRtm_impl{
    public String getDependencyModuleName()
    {
        return new String("OpenRTM");
    }
    
    /**
     * reset execution contexts(clock receivers)
     */
    public void resetClockReceivers(){
        for (int i=0; i<ecs_.size();){
            ExecutionContext ec = ecs_.get(i);
            try{
                System.out.println(i+": rate = "+ec.ec_.get_rate());
                ec.reset();
                i++;
            }catch(Exception ex){
                ecs_.remove(ec);
            }
        }
    }
    
    public void updateExecutionContext(double simTime)
    {
        for (int i=0; i<ecs_.size();){
            ExecutionContext ec = ecs_.get(i);
            if (ec.execute(simTime)){
                i++;
            }else{
                ecs_.remove(ec);
            }
        }
    }
}
