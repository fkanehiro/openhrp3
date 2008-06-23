package jp.go.aist.hrp.simulator;

import java.util.*;

import javax.vecmath.*;
import javax.media.j3d.Node;
import javax.media.j3d.Group;
import javax.media.j3d.SharedGroup;
import javax.media.j3d.*;


/**
 * BodyStruct class

 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/01/25)
 */
public class BodyStruct{
    private Group group_;// reference to the body
    private Vector<Shape3D> vShape3D_;// polygons
    private Vector<Transform3D> vTransform_; 
    private int shapePtr_=0;  
    private int triPtr_=0;

    private String segmentName_=null;//Segment名

    private LinkInfo_impl mo_;
    private int id_;
    private BodyStruct motherBody_;
    private BodyStruct sisterBody_;
    private BodyStruct daughterBody_;
    private Vector<SensorInfo_impl> vSensor_;//SensorInfo


    public BodyStruct(Group g, BodyStruct motherBody, int id){
        vShape3D_=new Vector<Shape3D>();
        vTransform_=new Vector<Transform3D>();
        vSensor_ = new Vector<SensorInfo_impl>();
        
        g.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        g.setCapability(Group.ALLOW_CHILDREN_READ);
        g.setCapability(Group.ALLOW_CHILDREN_WRITE);
       
        group_=g;
        id_=id;
        motherBody_=motherBody;
        mo_=new LinkInfo_impl(this);
        if(motherBody_!=null){
            mo_.setMother(motherBody_.getId());
        }
    }
    
    public void  addSensor(SensorInfo_impl info){
        vSensor_.add(info);
    }

    public void  setJointParam(String defName, VrmlSceneEx scene){
        mo_.setJointParam(defName,scene);
    }


    public void  setSegmentParam(String defName, VrmlSceneEx scene){
        segmentName_=defName;
        mo_.setSegmentParam(defName,scene);
    }
    
    public void setParamForEnviroment(String name,String segmentName){
        segmentName_=segmentName;
        mo_.setParamForEnviroment(name);
    }

    public String getSegmentName(){
        return segmentName_;
    }
    
    public BodyStruct getSisterBody(){
        return sisterBody_;
    }
    public BodyStruct getDaughterBody(){
        return daughterBody_;
    }
    public BodyStruct getMotherBody(){
        return motherBody_;
    }

    public void setSister(BodyStruct body){
        sisterBody_=body;
        mo_.setSister(sisterBody_.getId());
    }
    public void setDaughter(BodyStruct body){
        daughterBody_=body;
        mo_.setDaughter(daughterBody_.getId());
    }

    public int getId(){
        return id_;
    }

    public LinkInfo_impl getLinkInfo(){
        mo_.sensorInfoSeqImpl_ = new SensorInfo_impl[vSensor_.size()];
        for(int i=0; i<mo_.sensorInfoSeqImpl_.length ; i++) {
            mo_.sensorInfoSeqImpl_[i] = (SensorInfo_impl)vSensor_.get(i);
        }
        return mo_;
    }

    public void addShape3D(Shape3D shape3d,Transform3D transform){
        shape3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        Geometry geometry = shape3d.getGeometry();
                if (geometry instanceof TriangleArray) {
                    TriangleArray ta = (TriangleArray)geometry;
                    ta.setCapability(TriangleArray.ALLOW_COUNT_READ);
                    ta.setCapability(TriangleArray.ALLOW_COORDINATE_READ);
                } 
                else if (geometry instanceof TriangleStripArray) {

                    TriangleStripArray tsa = (TriangleStripArray)geometry;
                    tsa.setCapability(TriangleStripArray.ALLOW_COUNT_READ);
                    tsa.setCapability(TriangleStripArray.ALLOW_COORDINATE_READ);
                    }
                else if (geometry instanceof TriangleFanArray)  {
                    TriangleFanArray tfa = (TriangleFanArray)geometry;
                    tfa.setCapability(TriangleFanArray.ALLOW_COUNT_READ);
                    tfa.setCapability(TriangleFanArray.ALLOW_COORDINATE_READ);
                }
                else if (geometry instanceof QuadArray) {
                    QuadArray qa = (QuadArray)geometry;
                    qa.setCapability(QuadArray.ALLOW_COUNT_READ);
                    qa.setCapability(QuadArray.ALLOW_COORDINATE_READ);
                }
        vShape3D_.add(shape3d);
        vTransform_.add(transform);
        
    }
    
    public void resetPointer(){
        shapePtr_=0;
        triPtr_=0;
    }
    
    public double[] readTri(int start, int count){
        double[] tris = new double[(int)(count*9)];
        int nTri = 0, istart, iwrite=0;
        for (int ishape=0; ishape <vShape3D_.size() && iwrite < count;ishape++){
            Shape3D s3d = (Shape3D)vShape3D_.elementAt(ishape);
            Transform3D t3d = (Transform3D)vTransform_.elementAt(ishape);
            Geometry geometry = s3d.getGeometry();

            // ケース 1 : TriangleArray
            if (geometry instanceof TriangleArray) {
                TriangleArray ta = (TriangleArray)geometry;
                double[] v1 = new double[3];
                double[] v2 = new double[3];
                double[] v3 = new double[3];
                if (start >= nTri){
                    istart = start - nTri;
                }else{
                    istart = 0;
                }
                for (int i = istart*3; i < ta.getVertexCount(); i += 3) {
                    
                    ta.getCoordinate(i    , v1);
                    ta.getCoordinate(i + 1, v2);
                    ta.getCoordinate(i + 2, v3);
                    
                    _addTriangle(tris,iwrite++,v1, v2, v3,t3d);
                    if(iwrite == count) break;
                }
                nTri += ta.getVertexCount()/3;
            } else if (geometry instanceof TriangleStripArray) {
                TriangleStripArray tsa = (TriangleStripArray)geometry;
                int numStrips = tsa.getNumStrips();
                int[] stripVertexCounts = new int[numStrips];
                tsa.getStripVertexCounts(stripVertexCounts);
                
                double[] v1 = new double[3];
                double[] v2 = new double[3];
                double[] v3 = new double[3];
                int ind = 0;
                for (int i = 0; i < numStrips && iwrite < count; i++) {
                    if (start >= nTri){
                        istart = start - nTri;
                    }else{
                        istart = 0;
                    }
                    for (int j = istart; j < stripVertexCounts[i] - 2; j++) {
                        
                        if ((j % 2) == 0) {
                            tsa.getCoordinate(ind    , v1);
                            tsa.getCoordinate(ind + 1, v2);
                            tsa.getCoordinate(ind + 2, v3);
                        }
                        else {
                            tsa.getCoordinate(ind + 2, v1);
                            tsa.getCoordinate(ind + 1, v2);
                            tsa.getCoordinate(ind    , v3);
                        }
                        
                        _addTriangle(tris,iwrite++,v1, v2, v3,t3d);
                        if(iwrite == count) break;
                        
                        ind++;
                    }
                    nTri += stripVertexCounts[i] - 2;
                    ind += 2;
                }
            } else if (geometry instanceof TriangleFanArray)  {
                TriangleFanArray tfa = (TriangleFanArray)geometry;
                int numStrips = tfa.getNumStrips();
                int[] stripVertexCounts = new int[numStrips];
                tfa.getStripVertexCounts(stripVertexCounts);
                
                double[] v1 = new double[3];
                double[] v2 = new double[3];
                double[] v3 = new double[3];
                int ind = 0;
                for (int i = 0; i < numStrips && iwrite < count; i++) {
                    
                    tfa.getCoordinate(ind, v1);
                    if (start >= nTri){
                        istart = start - nTri;
                    }else{
                        istart = 0;
                    }
                    for (int j = istart; j < (stripVertexCounts[i] - 2); j++) {
                        
                        tfa.getCoordinate(ind + j + 1, v2);
                        tfa.getCoordinate(ind + j + 2, v3);
                        
                        _addTriangle(tris,iwrite++,v1, v2, v3,t3d);
                        if(iwrite==count) break;
                    }
                    nTri += stripVertexCounts[i] -2;
                    ind += stripVertexCounts[i];
                }
            }else if (geometry instanceof QuadArray) {
                QuadArray qa = (QuadArray)geometry;
                int vertexCount = qa.getVertexCount();
                
                double[] v1 = new double[3];
                double[] v2 = new double[3];
                double[] v3 = new double[3];
                if (start >= nTri){
                    istart = start - nTri;
                }else{
                    istart = 0;
                }
                for (int i = istart*2; i < vertexCount; i += 4) {
                    
                    qa.getCoordinate(i,     v1);
                    qa.getCoordinate(i + 1, v2);
                    qa.getCoordinate(i + 2, v3);
                    
                    _addTriangle(tris,iwrite++,v1, v2, v3,t3d);
                    if(iwrite==count) break;
                    
                    qa.getCoordinate(i + 2, v1);
                    qa.getCoordinate(i + 3, v2);
                    qa.getCoordinate(i,     v3);
                    
                    _addTriangle(tris,iwrite++,v1, v2, v3,t3d);
                    if(iwrite == count) break;
                }
                nTri += vertexCount / 2;
            }else {
                System.out.println("* Shape3D Type " + geometry.toString() + " is not supported.");
            }
        }
    
        //System.out.println("getTris:iwrite="+iwrite);
        //System.out.println("getTris:triPtr_="+triPtr_);

        if(iwrite == count){
            return tris;
        }else{
            double[] tris2=new double[(int)iwrite*9];
            
            for(int i=0 ;i<iwrite*9;i++){
                tris2[i]=tris[i];
            }
            return tris2;
        }
    }

    private void _addTriangle(
        double[] tris,
        long triCounter,
        double[] v1,
        double[] v2,
        double[] v3,
        Transform3D transform
    ){
        if(triCounter>=0){
            
            Point3d vec;
            vec=new Point3d(v1);
            transform.transform(vec);
            tris[(int)triCounter*9+0]=vec.x;
            tris[(int)triCounter*9+1]=vec.y;
            tris[(int)triCounter*9+2]=vec.z;

            vec=new Point3d(v2);
            transform.transform(vec);
            tris[(int)triCounter*9+3]=vec.x;
            tris[(int)triCounter*9+4]=vec.y;
            tris[(int)triCounter*9+5]=vec.z;

            vec=new Point3d(v3);
            transform.transform(vec);
            tris[(int)triCounter*9+6]=vec.x;
            tris[(int)triCounter*9+7]=vec.y;
            tris[(int)triCounter*9+8]=vec.z;
        }
        
    }
    
}
