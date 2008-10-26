package com.generalrobotix.ui.util;

import java.io.BufferedWriter;
import java.io.FileWriter;

import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;

import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSensorItem;
import com.generalrobotix.ui.item.GrxShapeItem;

public class GrxVrmlExporter {
	public static boolean export(GrxModelItem model, String fPath){
		// TODO check existence of fPath
		BufferedWriter writer = null;
	    try {
	        writer = new BufferedWriter(new FileWriter(fPath));
	        
	        writer.write("#VRML V2.0 utf8\n");                                              
	        writer.write("\n");                                                             
	        writer.write("PROTO Joint [\n");                                                
	        writer.write("  exposedField     SFVec3f      center              0 0 0\n");    
	        writer.write("  exposedField     MFNode       children            []\n");       
	        writer.write("  exposedField     MFFloat      llimit              []\n");       
	        writer.write("  exposedField     MFFloat      lvlimit             []\n");       
	        writer.write("  exposedField     SFRotation   limitOrientation    0 0 1 0\n");  
	        writer.write("  exposedField     SFString     name                \"\"\n");       
	        writer.write("  exposedField     SFRotation   rotation            0 0 1 0\n");  
	        writer.write("  exposedField     SFVec3f      scale               1 1 1\n");    
	        writer.write("  exposedField     SFRotation   scaleOrientation    0 0 1 0\n");  
	        writer.write("  exposedField     MFFloat      stiffness           [ 0 0 0 ]\n");                                                                               
	        writer.write("  exposedField     SFVec3f      translation         0 0 0\n");    
	        writer.write("  exposedField     MFFloat      ulimit              []\n");       
	        writer.write("  exposedField     MFFloat      uvlimit             []\n");       
	        writer.write("  exposedField     SFString     jointType           \"\"\n");       
	        writer.write("  exposedField     SFInt32      jointId             -1\n");       
	        writer.write("  exposedField     SFString     jointAxis           \"Z\"\n");      
	        writer.write("\n");                                                             
	        writer.write("  exposedField     SFFloat      gearRatio           1\n");        
	        writer.write("  exposedField     SFFloat      rotorInertia        0\n");        
	        writer.write("  exposedField     SFFloat      rotorResistor       0\n");        
	        writer.write("  exposedField     SFFloat      torqueConst         1\n");        
	        writer.write("  exposedField     SFFloat      encoderPulse        1\n");        
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    center           IS center\n");                               
	        writer.write("    children         IS children\n");                             
	        writer.write("    rotation         IS rotation\n");                             
	        writer.write("    scale            IS scale\n");                                
	        writer.write("    scaleOrientation IS scaleOrientation\n");                     
	        writer.write("    translation      IS translation\n");                          
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO Segment [\n");                                              
	        writer.write("  field           SFVec3f     bboxCenter        0 0 0\n");        
	        writer.write("  field           SFVec3f     bboxSize          -1 -1 -1\n");     
	        writer.write("  exposedField    SFVec3f     centerOfMass      0 0 0\n");        
	        writer.write("  exposedField    MFNode      children          [ ]\n");          
	        writer.write("  exposedField    SFNode      coord             NULL\n");         
	        writer.write("  exposedField    MFNode      displacers        [ ]\n");          
	        writer.write("  exposedField    SFFloat     mass              0 \n");           
	        writer.write("  exposedField    MFFloat     momentsOfInertia  [ 0 0 0 0 0 0 0 0 0 ]\n");                                                                       
	        writer.write("  exposedField    SFString    name              \"\"\n");           
	        writer.write("  eventIn         MFNode      addChildren\n");                    
	        writer.write("  eventIn         MFNode      removeChildren\n");                 
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Group {\n");                                                    
	        writer.write("    addChildren    IS addChildren\n");                            
	        writer.write("    bboxCenter     IS bboxCenter\n");                             
	        writer.write("    bboxSize       IS bboxSize\n");                               
	        writer.write("    children       IS children\n");                               
	        writer.write("    removeChildren IS removeChildren\n");                         
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO Humanoid [\n");                                             
	        writer.write("  field           SFVec3f    bboxCenter            0 0 0\n");     
	        writer.write("  field           SFVec3f    bboxSize              -1 -1 -1\n");  
	        writer.write("  exposedField    SFVec3f    center                0 0 0\n");     
	        writer.write("  exposedField    MFNode     humanoidBody          [ ]\n");       
	        writer.write("  exposedField    MFString   info                  [ ]\n");       
	        writer.write("  exposedField    MFNode     joints                [ ]\n");       
	        writer.write("  exposedField    SFString   name                  \"\"\n");        
	        writer.write("  exposedField    SFRotation rotation              0 0 1 0\n");   
	        writer.write("  exposedField    SFVec3f    scale                 1 1 1\n");     
	        writer.write("  exposedField    SFRotation scaleOrientation      0 0 1 0\n");   
	        writer.write("  exposedField    MFNode     segments              [ ]\n");       
	        writer.write("  exposedField    MFNode     sites                 [ ]\n");       
	        writer.write("  exposedField    SFVec3f    translation           0 0 0\n");     
	        writer.write("  exposedField    SFString   version               \"1.1\"\n");     
	        writer.write("  exposedField    MFNode     viewpoints            [ ]\n");       
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    bboxCenter       IS bboxCenter\n");                           
	        writer.write("    bboxSize         IS bboxSize\n");                             
	        writer.write("    center           IS center\n");                               
	        writer.write("    rotation         IS rotation\n");                             
	        writer.write("    scale            IS scale\n");                                
	        writer.write("    scaleOrientation IS scaleOrientation\n");                     
	        writer.write("    translation      IS translation\n");                          
	        writer.write("    children [\n");                                               
	        writer.write("      Group {\n");                                                
	        writer.write("        children IS viewpoints\n");                               
	        writer.write("      }\n");                                                      
	        writer.write("      Group {\n");                                                
	        writer.write("        children IS humanoidBody \n");                            
	        writer.write("      }\n");                                                      
	        writer.write("    ]\n");                                                        
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO VisionSensor [\n");                                         
	        writer.write("  exposedField SFVec3f    translation       0 0 0\n");            
	        writer.write("  exposedField SFRotation rotation          0 0 1 0\n");          
	        writer.write("  exposedField MFNode     children          [ ]\n");              
	        writer.write("  exposedField SFFloat    fieldOfView       0.785398\n");         
	        writer.write("  exposedField SFString   name              \"\"\n");               
	        writer.write("  exposedField SFFloat    frontClipDistance 0.01\n");             
	        writer.write("  exposedField SFFloat    backClipDistance  10.0\n");             
	        writer.write("  exposedField SFString   type              \"NONE\"\n");           
	        writer.write("  exposedField SFInt32    sensorId          -1\n");               
	        writer.write("  exposedField SFInt32    width             320\n");              
	        writer.write("  exposedField SFInt32    height            240\n");              
	        writer.write("  exposedField SFFloat    frameRate         30\n");               
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    rotation         IS rotation\n");                             
	        writer.write("    translation      IS translation\n");                          
	        writer.write("    children         IS children\n");                             
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("\n");                                                             
	        writer.write("PROTO ForceSensor [  \n");                                        
	        writer.write("  exposedField SFVec3f    maxForce    -1 -1 -1\n");               
	        writer.write("  exposedField SFVec3f    maxTorque   -1 -1 -1\n");               
	        writer.write("  exposedField SFVec3f    translation 0 0 0\n");                  
	        writer.write("  exposedField SFRotation rotation    0 0 1 0\n");                
	        writer.write("  exposedField SFInt32    sensorId    -1\n");                     
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    translation IS translation\n");                               
	        writer.write("    rotation    IS rotation\n");                                  
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO Gyro [\n");                                                 
	        writer.write("  exposedField SFVec3f    maxAngularVelocity -1 -1 -1\n");        
	        writer.write("  exposedField SFVec3f    translation        0 0 0\n");           
	        writer.write("  exposedField SFRotation rotation           0 0 1 0\n");         
	        writer.write("  exposedField SFInt32    sensorId           -1\n");              
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    translation IS translation\n");                               
	        writer.write("    rotation    IS rotation\n");                                  
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO AccelerationSensor [\n");                                   
	        writer.write("  exposedField SFVec3f    maxAcceleration -1 -1 -1\n");           
	        writer.write("  exposedField SFVec3f    translation     0 0 0\n");              
	        writer.write("  exposedField SFRotation rotation        0 0 1 0\n");            
	        writer.write("  exposedField SFInt32    sensorId        -1\n");                 
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    translation IS translation\n");                               
	        writer.write("    rotation    IS rotation\n");                                  
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO PressureSensor [\n");                                       
	        writer.write("  exposedField SFFloat    maxPressure -1\n");                     
	        writer.write("  exposedField SFVec3f    translation 0 0 0\n");                  
	        writer.write("  exposedField SFRotation rotation    0 0 1 0\n");                
	        writer.write("  exposedField SFInt32    sensorId    -1\n");                     
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    translation IS translation\n");                               
	        writer.write("    rotation    IS rotation\n");                                  
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO PhotoInterrupter [\n");                                     
	        writer.write("  exposedField SFVec3f transmitter 0 0 0\n");                     
	        writer.write("  exposedField SFVec3f receiver    0 0 0\n");                     
	        writer.write("  exposedField SFInt32 sensorId    -1\n");                        
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform{\n");                                                 
	        writer.write("    children [\n");                                               
	        writer.write("      Transform{\n");                                             
	        writer.write("        translation IS transmitter\n");                           
	        writer.write("      }\n");                                                      
	        writer.write("      Transform{\n");                                             
	        writer.write("        translation IS receiver\n");                              
	        writer.write("      }\n");                                                      
	        writer.write("    ]\n");                                                        
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("NavigationInfo {\n");                                             
	        writer.write("  avatarSize    0.5\n");                                          
	        writer.write("  headlight     TRUE\n");                                         
	        writer.write("  type  [\"EXAMINE\", \"ANY\"]\n");                                   
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("Background {\n");                                                 
	        writer.write("  skyColor 0.4 0.6 0.4\n");                                       
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("Viewpoint {\n");                                                  
	        writer.write("  position    3 0 0.835\n");                                      
	        writer.write("  orientation 0.5770 0.5775 0.5775 2.0935\n");                    
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        
	        writer.write("DEF "+model.getName()+" Humanoid{\n");
	        writer.write("  humanoidBody [\n");
	        exportLink(writer, model.rootLink(), "    ");
	        writer.write("  ]\n");
	        writer.write("  joints [\n");
	        for (int i=0; i<model.links_.size(); i++){
	        	writer.write("    USE "+model.links_.get(i).getName()+",\n");
	        }
	        writer.write("  ]\n");
	        writer.write("  segments [\n");
	        for (int i=0; i<model.links_.size(); i++){
	        	writer.write("    USE "+model.links_.get(i).getName()+"_Link,\n");
	        }
	        writer.write("  ]\n");
	        writer.write("}\n");
	        return true;
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (writer != null) {
	                writer.flush();
	                writer.close();
	            }
	        } catch (Exception e) {
	        }
	    }
	    return false;
	}

	public static void exportLink(BufferedWriter writer, GrxLinkItem link, String indent){
		try{
			writer.write(indent+"DEF "+link.getName()+" Joint {\n");
			writer.write(indent+"  jointType \""+link.jointType()+"\"\n");
			if (link.jointId() != -1) writer.write(indent+"  jointId "+link.jointId()+"\n");
			if (link.jointType().equals("rotate") || link.jointType().equals("slide")){
				writer.write(indent+"  jointAxis "+link.getProperty("jointAxis")+"\n");
			}
			if (!link.getProperty("translation").equals("0.0 0.0 0.0 ")){
				writer.write(indent+"  translation "+link.getProperty("translation")+"\n");
			}
			if (!link.getProperty("rotation").equals("0.0 0.0 1.0 0.0 ")){
				writer.write(indent+"  rotation "+link.getProperty("rotation")+"\n");
			}
			if (!link.getProperty("ulimit").equals("")) writer.write(indent+"  ulimit ["+link.getProperty("ulimit")+"]\n");
			if (!link.getProperty("llimit").equals("")) writer.write(indent+"  llimit ["+link.getProperty("llimit")+"]\n");
			if (!link.getProperty("uvlimit").equals("")) writer.write(indent+"  uvlimit ["+link.getProperty("uvlimit")+"]\n");
			if (!link.getProperty("lvlimit").equals("")) writer.write(indent+"  lvlimit ["+link.getProperty("lvlimit")+"]\n");
			if (link.gearRatio() != 1.0) writer.write(indent+"  gearRatio "+link.gearRatio()+"\n");
			if (link.rotorInertia() != 0.0) writer.write(indent+"  rotorInertia "+link.rotorInertia()+"\n");
			if (link.rotorResister() != 0.0) writer.write(indent+"  rotorResister "+link.rotorResister()+"\n");
			if (link.torqueConst() != 1.0) writer.write(indent+"  torqueConst "+link.torqueConst()+"\n");
			if (link.encoderPulse() != 1.0) writer.write(indent+"  encoderPulse "+link.encoderPulse()+"\n");
			writer.write(indent+"  children[\n");
			for (int i=0; i<link.children_.size(); i++){
				if (link.children_.get(i) instanceof GrxSensorItem){
				exportSensor(writer, (GrxSensorItem)link.children_.get(i), indent+"    ");
				}
			}
			writer.write(indent+"    DEF "+link.getName()+"_Link Segment{\n");
			writer.write(indent+"      centerOfMass "+link.getProperty("centerOfMass")+"\n");
			writer.write(indent+"      mass "+link.mass()+"\n");
			writer.write(indent+"      momentsOfInertia [ "+link.getProperty("inertia")+"]\n");
			writer.write(indent+"      children[\n");
			for (int i=0; i<link.children_.size(); i++){
				if (link.children_.get(i) instanceof GrxShapeItem){
					exportShape(writer, (GrxShapeItem)link.children_.get(i), indent+"        ");
				}
			}
			writer.write(indent+"      ]\n");
			writer.write(indent+"    }\n");
			for (int i=0; i<link.children_.size(); i++){
				if (link.children_.get(i) instanceof GrxLinkItem){
					exportLink(writer, (GrxLinkItem)link.children_.get(i), indent+"    ");
				}
			}
			writer.write(indent+"  ]\n");
			writer.write(indent+"}\n");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	public static void exportSensor(BufferedWriter writer, GrxSensorItem sensor, String indent){
		try{
			String nodeType="unknown";
			if (sensor.type().equals("Force")){
				nodeType = "ForceSensor";
			}else if(sensor.type().equals("RateGyro")){
				nodeType = "Gyro"; 
			}else if(sensor.type().equals("Acceleration")){
				nodeType = "AccelerationSensor";
			}else if(sensor.type().equals("Vision")){
				nodeType = "VisionSensor";
			}
			writer.write(indent+"DEF "+sensor.getName()+" "+nodeType+" {\n");
			writer.write(indent+"  sensorId "+sensor.id()+"\n");
			if (!sensor.getProperty("translation").equals("0.0 0.0 0.0 ")){
				writer.write(indent+"  translation "+sensor.getProperty("translation")+"\n");
			}
			if (!sensor.getProperty("rotation").equals("0.0 0.0 1.0 0.0 ")){
				writer.write(indent+"  rotation "+sensor.getProperty("rotation")+"\n");
			}
			// sensor specific parameters
			if (nodeType.equals("VisionSensor")){
				if (!sensor.getProperty("fieldOfView").equals("0.785398")){
					writer.write(indent+"  fieldOfView "+sensor.getProperty("fieldOfView")+"\n");
				}
				if (!sensor.getProperty("frontClipDistance").equals("0.01")){
					writer.write(indent+"  frontClipDistance "+sensor.getProperty("frontClipDistance")+"\n");
				}
				if (!sensor.getProperty("backClipDistance").equals("10.0")){
					writer.write(indent+"  backClipDistance "+sensor.getProperty("backClipDistance")+"\n");
				}
				if (!sensor.getProperty("width").equals("320")){
					writer.write(indent+"  width "+sensor.getProperty("width")+"\n");
				}
				if (!sensor.getProperty("height").equals("240")){
					writer.write(indent+"  height "+sensor.getProperty("height")+"\n");
				}
				if (!sensor.getProperty("frameRate").equals("30.0")){
					writer.write(indent+"  frameRate "+sensor.getProperty("frameRate")+"\n");
				}
				if (!sensor.getProperty("cameraType").equals("NONE")){
					writer.write(indent+"  type "+sensor.getProperty("cameraType")+"\n");
				}
			}else if(nodeType.equals("ForceSensor")){
				String maxf = sensor.getProperty("maxForce");
				if (!maxf.equals("-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxForce "+maxf+"\n");
				}
				String maxt = sensor.getProperty("maxTorque");
				if (!maxt.equals("-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxTorque "+maxt+"\n");
				}
			}else if(nodeType.equals("Gyro")){
				String max = sensor.getProperty("maxAngularVelocity");
				if (!max.equals("-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxAngularVelocity "+max+"\n");
				}
			}else if(nodeType.equals("AccelerationSensor")){
				String max = sensor.getProperty("maxAcceleration");
				if (!max.equals("-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxAcceleration "+max+"\n");
				}
			}
			writer.write(indent+"}\n");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	public static void exportShape(BufferedWriter writer, GrxShapeItem shape, String indent){
		try{
			if (shape.getURL(false) == null){
				writer.write(indent+"Transform {\n");
				if (!shape.getProperty("translation").equals("0.0 0.0 0.0 ")){
					writer.write(indent+"  translation "+shape.getProperty("translation")+"\n");
				}
				if (!shape.getProperty("rotation").equals("0.0 1.0 0.0 0.0 ")){
					writer.write(indent+"  rotation "+shape.getProperty("rotation")+"\n");
				}
				writer.write(indent+"  chlidren[\n");
				writer.write(indent+"    Shape{\n");
				exportGeometry(writer, shape.shapeInfo_, indent+"      ");
				if (shape.appearanceInfo_ != null){
					writer.write(indent+"      appearance Appearance{\n");
					if (shape.materialInfo_ != null){
						MaterialInfo mat = shape.materialInfo_;
						writer.write(indent+"        material Material{\n");
						writer.write(indent+"          diffuseColor "
								+mat.diffuseColor[0]+" "+mat.diffuseColor[1]+" "+mat.diffuseColor[2]+"\n");
						writer.write(indent+"        }\n");
					}
					writer.write(indent+"      }\n");
				}
				writer.write(indent+"    }\n");
				writer.write(indent+"  ]\n");
				writer.write(indent+"}\n");
			}else{
				writer.write(indent+"Inline { url \""+shape.getURL(false)+"\" }\n");
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static void exportGeometry(BufferedWriter writer, ShapeInfo info, String indent){
		try{
			ShapePrimitiveType ptype = info.primitiveType;
			float [] pparams = info.primitiveParameters;
			
			if (ptype == ShapePrimitiveType.SP_MESH){
			}else if (ptype == ShapePrimitiveType.SP_BOX){
				writer.write(indent+"geometry Box{\n");
				writer.write(indent+"  size "
						+pparams[0]+" "
						+pparams[1]+" "
						+pparams[2]+"\n");
				writer.write(indent+"}\n");
			}else if (ptype == ShapePrimitiveType.SP_CYLINDER){
				writer.write(indent+"geometry Cylinder{\n");
				writer.write(indent+"  radius "+pparams[0]+"\n");
				writer.write(indent+"  height "+pparams[1]+"\n");
				if (pparams[2]==0){
					writer.write(indent+"  top FALSE\n");
				}
				if (pparams[3]==0){
					writer.write(indent+"  bottom FALSE\n");
				}
				if (pparams[4]==0){
					writer.write(indent+" side FALSE\n");
				}
				writer.write(indent+"}\n");					
			}else if (ptype == ShapePrimitiveType.SP_CONE){
				writer.write(indent+"geometry Cone{\n");
				writer.write(indent+"  radius "+pparams[0]+"\n");
				writer.write(indent+"  height "+pparams[1]+"\n");
				if (pparams[2]==0){
					writer.write(indent+"  bottom FALSE\n");
				}
				if (pparams[3]==0){
					writer.write(indent+"  side FALSE\n");
				}
				writer.write(indent+"}\n");					
			}else if (ptype == ShapePrimitiveType.SP_SPHERE){
				writer.write(indent+"geometry Sphere{\n");
				writer.write(indent+"  radius "+pparams[0]+"\n");
				writer.write(indent+"}\n");					
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}