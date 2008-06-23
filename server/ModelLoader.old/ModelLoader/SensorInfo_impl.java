package jp.go.aist.hrp.simulator;

import javax.vecmath.*;

public final class SensorInfo_impl
	extends SensorInfoPOA
{
    public String name_; //to do seting
    public SensorType type_; //to do seting
    public int id_;
    public double[] maxValue_ = new double[0];
    
    public double[] translation_ = new double[3];
    public double[] rotation_ = new double[9];
    
    public String name() { return name_; }
    public SensorType type() { return type_; }
    public int id() { return id_; }
    public double[] maxValue() { return maxValue_; }
    
    public double[] translation() { return translation_; }
    public double[] rotation() { return rotation_; }

}
