package jp.go.aist.hrp.simulator;

// CORBA
import org.omg.CORBA.*;

import javax.vecmath.*;

// VRML97
import com.sun.j3d.loaders.vrml97.impl.*;

/**
 * LinkInfo_impl class
 * @author Ichitaro Kohara (Kernel Co.,Ltd.)
 * @version 1.01 (2000/07/10)
 * @modify Keisuke Saito (Kernel Co.,Ltd.)
 * @version 1.10 (2002/02/19)
 */
public class LinkInfo_impl
  extends LinkInfoPOA {
    
    double mass_;
    double[] centerOfMass_ = new double[3];

    double[] rotation_ = new double[9];
    double[] translation_ = new double[3];
    
    double[] inertia_ = new double[9];
    double[] ulimit_ = new double[1];
    double[] llimit_ = new double[1];
    double[] uvlimit_ = new double[1];
    double[] lvlimit_ = new double[1];

    // for rotor inertia of servomotor  '01 Jun.29 s.kajita
    double rotorInertia_ = 0.0; // [kg.m^2]
    double rotorResistor_= 0.0; // [ohm]
    double gearRatio_    = 1.0; // [-]
    double torqueConst_  = 1.0; // [Nm/A]
    double encoderPulse_ = 1.0; // [pulse/rot]
    double equivalentInertia_ = 0.0; // [kg.m^2]

    String name_;
    String jointType_;
    int jointId_ = -1;
    double[/*3*/] jointAxis_ = new double[3];
    
    // for sensor  '02 Feb.25 k.saito(KernelInc)
    SensorInfo[] sensors_ = new SensorInfo[0];
    SensorInfo_impl[] sensorInfoSeqImpl_ = new SensorInfo_impl[0];

    double[/*3*/] bboxCenter_ = new double[3];
    double[/*3*/] bboxSize_ = new double[3];

    int mother_ = -1;
    int sister_ = -1;
    int daughter_ = -1;

    private boolean setSegmentParmFlag_=false;

    BodyStruct bs_ = null;

    /**
     * Constructor
     */
    public LinkInfo_impl(BodyStruct bs) {
        bs_ = bs;
        //System.out.println("LinkInfo: LinkInfo()");
        //absAttitude_[0]=1;
        //absAttitude_[4]=1;
        //absAttitude_[8]=1;
        //relAttitude_[0]=1;
        //relAttitude_[4]=1;
        //relAttitude_[8]=1;
        //inertia_[0]=1;
        //inertia_[4]=1;
        // inertia_[8]=1;
    }

    // Attitudes
    public double mass() { return mass_; };
    public double[] centerOfMass() { return centerOfMass_; }
    public double[] translation() { return translation_; }
    public double[] rotation()    { return rotation_; }
     
    public double[/*9*/] inertia() { return inertia_; }
    public double[/*1*/] ulimit() { return ulimit_; }
    public double[/*1*/] llimit() { return llimit_; }
    public double[/*1*/] uvlimit() { return uvlimit_; }
    public double[/*1*/] lvlimit() { return lvlimit_; }

    // for rotor inertia of servomotor  '01 Jun.29 s.kajita
    public double rotorInertia() { return rotorInertia_; }
    public double rotorResistor() { return rotorResistor_; }
    public double gearRatio() { return gearRatio_; }
    public double torqueConst() { return torqueConst_; }
    public double encoderPulse() { return encoderPulse_; }
    public double equivalentInertia () { return equivalentInertia_; }

    public double[/*3*/] bboxCenter() { return bboxCenter_; }
    public double[/*3*/] bboxSize() { return bboxSize_; }

    public String name() { return name_; }
    public String jointType() { return jointType_; }
    public int jointId() { return jointId_; }
    public double[/*3*/] jointAxis() { return jointAxis_; }
    

    public SensorInfo[] sensors(){ return sensors_; }
    public SensorInfo_impl[] getSensorInfoSeqImpl(){ return sensorInfoSeqImpl_; }
    
    public int mother() { return mother_; }
    public int sister() { return sister_; }
    public int daughter() { return daughter_; }
    
    

    /**
     * 自分自身にdefNameで指定されたJointの情報をsceneから読み出し格納
     *
     * @param  defName       Joint名
     * @param  scene         VrmlSceneEx
     */
    void setJointParam(String defName, VrmlSceneEx scene)
    {
        double[] p = new double[3];
        p = ProtoFieldGettor.getVectorValue(defName, scene, "translation", p);
        
	translation_[0] = p[0];
	translation_[1] = p[1];
	translation_[2] = p[2];
	
        double[] defAxis = {0,0,1,0};
        AxisAngle4d axisangle4d = new AxisAngle4d(ProtoFieldGettor.getRotationValue(defName, scene, "rotation", defAxis));

        //Quat4d quat4d = new Quat4d();
        //quat4d.set(axisangle4d);

	Matrix4d matrix4d = new Matrix4d();
        matrix4d.set(axisangle4d);
        rotation_[0] = matrix4d.m00;
        rotation_[1] = matrix4d.m01;
        rotation_[2] = matrix4d.m02;
        rotation_[3] = matrix4d.m10;
        rotation_[4] = matrix4d.m11;
        rotation_[5] = matrix4d.m12;
        rotation_[6] = matrix4d.m20;
        rotation_[7] = matrix4d.m21;
        rotation_[8] = matrix4d.m22;

        jointType_ = ProtoFieldGettor.getStringValue(defName, scene, "jointType",jointType_);
        
        if(jointType_.equals("rotate"))
        {
            double ad[] = ProtoFieldGettor.getRotationValue(defName, scene, "rotation",defAxis);
            //jointValue_ = ad[3];
        }
        ulimit_ = ProtoFieldGettor.getDoubleArray(defName, scene, "ulimit",ulimit_);
        llimit_ = ProtoFieldGettor.getDoubleArray(defName, scene, "llimit",llimit_);
        uvlimit_ = ProtoFieldGettor.getDoubleArray(defName, scene, "uvlimit",uvlimit_);
        lvlimit_ = ProtoFieldGettor.getDoubleArray(defName, scene, "lvlimit",lvlimit_);
        rotorInertia_ = ProtoFieldGettor.getDoubleValue(defName, scene, "rotorInertia",rotorInertia_);
        rotorResistor_ = ProtoFieldGettor.getDoubleValue(defName, scene, "rotorResistor",rotorResistor_);
        gearRatio_ = ProtoFieldGettor.getDoubleValue(defName, scene, "gearRatio",gearRatio_);
        torqueConst_ = ProtoFieldGettor.getDoubleValue(defName, scene, "torqueConst",torqueConst_);
        encoderPulse_ = ProtoFieldGettor.getDoubleValue(defName, scene, "encoderPulse",encoderPulse_);
        equivalentInertia_ = ProtoFieldGettor.getDoubleValue(defName, scene, "equivalentInertia", equivalentInertia_);
        jointId_ = ProtoFieldGettor.getIntValue(defName, scene, "jointId",jointId_);
        Field fld = scene.getField(defName, "jointAxis");
        try{
            if (fld instanceof SFString){
                String axis = ((SFString)fld).getValue();
                jointAxis_[0] = jointAxis_[1] = jointAxis_[2] = 0;
                if (axis.equals("X")){
                    jointAxis_[0] = 1;
                }else if (axis.equals("Y")){
                    jointAxis_[1] = 1;
                }else if (axis.equals("Z")){
                    jointAxis_[2] = 1;
                }else if (axis.equals("-X")){
                    jointAxis_[0] = -1;
                }else if (axis.equals("-Y")){
                    jointAxis_[1] = -1;
                }else if (axis.equals("-Z")){
                    jointAxis_[2] = -1;
                }else{
                    System.out.println("unknown jointAxis("+axis+")");
                }
            }else if(fld instanceof SFVec3f){
                SFVec3f v3 = (SFVec3f)fld;
                float fv3[] = new float[3];
                ((SFVec3f)fld).getValue(fv3);
                jointAxis_[0] = fv3[0];
                jointAxis_[1] = fv3[1];
                jointAxis_[2] = fv3[2];
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        //jointAxis_ = ProtoFieldGettor.getStringValue(defName, scene, "jointAxis",jointAxis_);
        name_ = defName;
    }
    
    /**
     * 自分自身にdefNameで指定されたSegmentの情報をsceneから読み出し格納
     *
     * @param  defName       Segment名
     * @param  scene         VrmlSceneEx
     */
    void setSegmentParam(String defName, VrmlSceneEx scene)
    {
        mass_ = ProtoFieldGettor.getDoubleValue(defName, scene, "mass",mass_);
        centerOfMass_ = ProtoFieldGettor.getVectorValue(defName, scene, "centerOfMass", centerOfMass_);
        inertia_ = ProtoFieldGettor.getDoubleArray(defName, scene, "momentsOfInertia",inertia_);
        bboxSize_ = ProtoFieldGettor.getVectorValue(defName, scene, "bboxSize",bboxSize_);
        bboxCenter_ = ProtoFieldGettor.getVectorValue(defName, scene, "bboxCenter",bboxCenter_);
        
        setSegmentParmFlag_ = true;
    }
    
    void setParamForEnviroment(String name){
        name_=name;
        jointType_="fixed";
    }
    
    /*
    * すでにsetSegmentParam()されているか否かの返す。
    *
    * `return すでにセットされていたらTrue
    */
    boolean isSetSegmentParam(){
        return setSegmentParmFlag_;
    }
    
    //以下の３つのメソッドは
    //mother,sister,doughterを設定する
    void setMother(int p){
        mother_=p;
    }
    void setSister(int p){
        sister_=p;
    }
    void setDaughter(int p){
        daughter_=p;
    }

    public double[] readTriangles(int start, int count){
        return bs_.readTri(start, count);
    }
}
