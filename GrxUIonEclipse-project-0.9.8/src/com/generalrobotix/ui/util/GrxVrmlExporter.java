package com.generalrobotix.ui.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Vector;
import java.util.regex.*;

import jp.go.aist.hrp.simulator.AppearanceInfo;
import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;
import jp.go.aist.hrp.simulator.TextureInfo;

import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
import com.generalrobotix.ui.item.GrxSegmentItem;
import com.generalrobotix.ui.item.GrxSensorItem;
import com.generalrobotix.ui.item.GrxShapeItem;

public class GrxVrmlExporter {
	/**
	 * @brief export model item to a VRML97 file
	 * @param model model item to be exported
	 * @param exportPath path of VRML97 file
	 * @return true exported successfully, false otherwise
	 */
	public static boolean export(GrxModelItem model, String exportPath){
		// TODO check existence of exportPath
		int idx = exportPath.lastIndexOf(File.separatorChar);
		if(idx==-1)
			idx = exportPath.lastIndexOf('/');
		String exportDir = exportPath.substring(0, idx);

		BufferedWriter writer = null;
	    try {
	        writer = new BufferedWriter(new FileWriter(exportPath));
	        
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
	        writer.write("  exposedField     SFVec3f      jointAxis           0 0 1\n");      
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
	        writer.write("  exposedField MFNode     children          [ ]\n");              
	        writer.write("  exposedField SFInt32    sensorId    -1\n");                     
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    translation IS translation\n");                               
	        writer.write("    rotation    IS rotation\n");                                  
	        writer.write("    children    IS children\n");                             
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO Gyro [\n");                                                 
	        writer.write("  exposedField SFVec3f    maxAngularVelocity -1 -1 -1\n");        
	        writer.write("  exposedField SFVec3f    translation        0 0 0\n");           
	        writer.write("  exposedField SFRotation rotation           0 0 1 0\n");         
	        writer.write("  exposedField MFNode     children          [ ]\n");              
	        writer.write("  exposedField SFInt32    sensorId           -1\n");              
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    translation IS translation\n");                               
	        writer.write("    rotation    IS rotation\n");                                  
	        writer.write("    children    IS children\n");                             
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                                                             
	        writer.write("PROTO AccelerationSensor [\n");                                   
	        writer.write("  exposedField SFVec3f    maxAcceleration -1 -1 -1\n");           
	        writer.write("  exposedField SFVec3f    translation     0 0 0\n");              
	        writer.write("  exposedField SFRotation rotation        0 0 1 0\n");            
	        writer.write("  exposedField MFNode     children          [ ]\n");              
	        writer.write("  exposedField SFInt32    sensorId        -1\n");                 
	        writer.write("]\n");                                                            
	        writer.write("{\n");                                                            
	        writer.write("  Transform {\n");                                                
	        writer.write("    translation IS translation\n");                               
	        writer.write("    rotation    IS rotation\n");                                  
	        writer.write("    children    IS children\n");                             
	        writer.write("  }\n");                                                          
	        writer.write("}\n");                                                            
	        writer.write("\n");                         
	        writer.write("PROTO Plane [\n");
	        writer.write("  exposedField SFVec3f size 10 10 0\n");
	        writer.write("]\n");
	        writer.write("{\n");
	        writer.write("  Box {\n");
	        writer.write("    size IS size\n");
	        writer.write("  }\n");
	        writer.write("}\n");
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
	        Vector<GrxLinkItem> links = exportLink(writer, model.rootLink(), "    ", exportDir, model.getURL(false));
	        writer.write("  ]\n");
	        writer.write("  joints [\n");
	        for (int i=0; i<links.size(); i++){
	        	writer.write("    USE "+links.get(i).getName()+",\n");
	        }
	        writer.write("  ]\n");
	        writer.write("  segments [\n");
	        for (int i=0; i<links.size(); i++){
	        	GrxLinkItem link = links.get(i);
	        	for (int j=0; j<link.children_.size(); j++){
					if (link.children_.get(j) instanceof GrxSegmentItem){
						writer.write("    USE "+link.children_.get(j).getName()+",\n");
					}
				}
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

	/**
	 * @brief export link information
	 * @param writer output stream
	 * @param link link to be exported
	 * @param indent indent string
	 * @param exportDir directory where VRML97 is exported
	 * @param mainPath path of the main file
	 * @return vector of links which include this link and child links
	 */
	public static Vector<GrxLinkItem> exportLink(BufferedWriter writer, GrxLinkItem link, String indent, 
			String exportDir, String mainPath){
		Vector<GrxLinkItem> links = new Vector<GrxLinkItem>();
		links.add(link);
		try{
			writer.write(indent+"DEF "+link.getName()+" Joint {\n");
			writer.write(indent+"  jointType \""+link.jointType_+"\"\n");
			if (link.jointId_ != -1) writer.write(indent+"  jointId "+link.jointId_+"\n");
			if (link.jointType_.equals("rotate") || link.jointType_.equals("slide")){
				writer.write(indent+"  jointAxis "+link.getProperty("jointAxis")+"\n");
			}
			if (!valueEquals(link.getProperty("translation"),"0.0 0.0 0.0 ")){
				writer.write(indent+"  translation "+link.getProperty("translation")+"\n");
			}
			if (!isZero(link.getProperty("rotation"),3)){
				writer.write(indent+"  rotation "+link.getProperty("rotation")+"\n");
			}
			if (!link.getProperty("ulimit").equals("")) writer.write(indent+"  ulimit ["+link.getProperty("ulimit")+"]\n");
			if (!link.getProperty("llimit").equals("")) writer.write(indent+"  llimit ["+link.getProperty("llimit")+"]\n");
			if (!link.getProperty("uvlimit").equals("")) writer.write(indent+"  uvlimit ["+link.getProperty("uvlimit")+"]\n");
			if (!link.getProperty("lvlimit").equals("")) writer.write(indent+"  lvlimit ["+link.getProperty("lvlimit")+"]\n");
			if (link.gearRatio_ != 1.0) writer.write(indent+"  gearRatio "+link.gearRatio_+"\n");
			if (link.rotorInertia_ != 0.0) writer.write(indent+"  rotorInertia "+link.rotorInertia_+"\n");
			if (link.rotorResistor_ != 0.0) writer.write(indent+"  rotorResistor "+link.rotorResistor_+"\n");
			if (link.torqueConst_ != 1.0) writer.write(indent+"  torqueConst "+link.torqueConst_+"\n");
			if (link.encoderPulse_ != 1.0) writer.write(indent+"  encoderPulse "+link.encoderPulse_+"\n");
			writer.write(indent+"  children[\n");
			for (int i=0; i<link.children_.size(); i++){
				if (link.children_.get(i) instanceof GrxSensorItem){
				exportSensor(writer, (GrxSensorItem)link.children_.get(i), indent+"    ", exportDir, mainPath);
				}
			}
			for (int i=0; i<link.children_.size(); i++){
				if (link.children_.get(i) instanceof GrxSegmentItem){
				exportSegment(writer, (GrxSegmentItem)link.children_.get(i), indent+"    ", exportDir, mainPath);
				}
			}
			for (int i=0; i<link.children_.size(); i++){
				if (link.children_.get(i) instanceof GrxLinkItem){
					Vector<GrxLinkItem> childLinks = exportLink(writer, (GrxLinkItem)link.children_.get(i), 
							indent+"    ", exportDir, mainPath);
					links.addAll(childLinks);
				}
			}
			writer.write(indent+"  ]\n");
			writer.write(indent+"}\n");
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return links;
	}

	public static void exportSegment(BufferedWriter writer, GrxSegmentItem segment, String indent, String exportDir, String mainPath){
		try{
			String translation = segment.getProperty("translation");
			String rotation = segment.getProperty("rotation");
			boolean useTransform = false;
			if(!valueEquals(translation, "0.0 0.0 0.0 " ) || !isZero(rotation, 3 )){
				useTransform = true;
				writer.write(indent+"Transform {\n");
				writer.write(indent+"  translation "+translation+"\n");
				writer.write(indent+"  rotation "+rotation+"\n");
				writer.write(indent+"  children[\n");
				indent += "    ";
			}
			writer.write(indent+"DEF "+segment.getName()+" Segment{\n");
			writer.write(indent+"  centerOfMass "+segment.getProperty("centerOfMass")+"\n");
			writer.write(indent+"  mass "+segment.getProperty("mass")+"\n");
			writer.write(indent+"  momentsOfInertia [ "+segment.getProperty("momentsOfInertia")+"]\n");
			writer.write(indent+"  children[\n");
			for (int i=0; i<segment.children_.size(); i++){
				if (segment.children_.get(i) instanceof GrxShapeItem){
					GrxShapeItem shape = (GrxShapeItem)segment.children_.get(i);
					exportShape(writer, shape, indent+"    ", exportDir, mainPath);
				}
			}
			writer.write(indent+"  ]\n");
			writer.write(indent+"}\n");	
			if(useTransform){
				writer.write(indent+"  ]\n");
				writer.write(indent+"}\n");	
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}	
	}
	
	/**
	 * @biref export sensor item
	 * @param writer output stream
	 * @param sensor sensor to be exported
	 * @param indent indent
	 * @param exportDir directory where VRML97 is exported
	 * @param mainPath path of the main file
	 */
	public static void exportSensor(BufferedWriter writer, GrxSensorItem sensor, String indent, String exportDir, String mainPath){
		try{
			String nodeType="unknown";
			if (sensor.type_.equals("Force")){
				nodeType = "ForceSensor";
			}else if(sensor.type_.equals("RateGyro")){
				nodeType = "Gyro"; 
			}else if(sensor.type_.equals("Acceleration")){
				nodeType = "AccelerationSensor";
			}else if(sensor.type_.equals("Vision")){
				nodeType = "VisionSensor";
			}
			writer.write(indent+"DEF "+sensor.getName()+" "+nodeType+" {\n");
			writer.write(indent+"  sensorId "+sensor.id_+"\n");
			if (!valueEquals(sensor.getProperty("translation"),"0.0 0.0 0.0 ")){
				writer.write(indent+"  translation "+sensor.getProperty("translation")+"\n");
			}
			if (!isZero(sensor.getProperty("rotation"),3)){
				writer.write(indent+"  rotation "+sensor.getProperty("rotation")+"\n");
			}
			// sensor specific parameters
			if (nodeType.equals("VisionSensor")){
				if (!valueEquals(sensor.getProperty("fieldOfView"),"0.785398")){
					writer.write(indent+"  fieldOfView "+sensor.getProperty("fieldOfView")+"\n");
				}
				if (!valueEquals(sensor.getProperty("frontClipDistance"),"0.01")){
					writer.write(indent+"  frontClipDistance "+sensor.getProperty("frontClipDistance")+"\n");
				}
				if (!valueEquals(sensor.getProperty("backClipDistance"),"10.0")){
					writer.write(indent+"  backClipDistance "+sensor.getProperty("backClipDistance")+"\n");
				}
				if (!valueEquals(sensor.getProperty("width"),"320")){
					writer.write(indent+"  width "+sensor.getProperty("width")+"\n");
				}
				if (!valueEquals(sensor.getProperty("height"),"240")){
					writer.write(indent+"  height "+sensor.getProperty("height")+"\n");
				}
				if (!valueEquals(sensor.getProperty("frameRate"),"30.0")){
					writer.write(indent+"  frameRate "+sensor.getProperty("frameRate")+"\n");
				}
				if (!sensor.getProperty("cameraType").equals("NONE")){
					writer.write(indent+"  type \""+sensor.getProperty("cameraType")+"\"\n");
				}
				if (!sensor.getProperty("name").equals("")){
					writer.write(indent+"  name \""+sensor.getProperty("name")+"\"\n");
				}
			}else if(nodeType.equals("ForceSensor")){
				String maxf = sensor.getProperty("maxForce");
				if (!valueEquals(maxf, "-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxForce "+maxf+"\n");
				}
				String maxt = sensor.getProperty("maxTorque");
				if (!valueEquals(maxt, "-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxTorque "+maxt+"\n");
				}
			}else if(nodeType.equals("Gyro")){
				String max = sensor.getProperty("maxAngularVelocity");
				if (!valueEquals(max, "-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxAngularVelocity "+max+"\n");
				}
			}else if(nodeType.equals("AccelerationSensor")){
				String max = sensor.getProperty("maxAcceleration");
				if (!valueEquals(max, "-1.0 -1.0 -1.0 ")){
					writer.write(indent+"  maxAcceleration "+max+"\n");
				}
			}
			if (sensor.children_.size() > 0){
				writer.write(indent+"  children[\n");
				for (int i=0; i<sensor.children_.size(); i++){
					if (sensor.children_.get(i) instanceof GrxShapeItem){
						exportShape(writer, (GrxShapeItem)sensor.children_.get(i), indent+"    ", exportDir, mainPath);
					}
				}
				writer.write(indent+"  ]\n");
			}
			writer.write(indent+"}\n");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * @brief export shape item 
	 * @param writer output stream
	 * @param shape shape to be exported
	 * @param indent indent 
	 * @param exportDir directory where VRML97 is exported
	 * @param mainPath path of the main file
	 */
	public static void exportShape(BufferedWriter writer, GrxShapeItem shape, String indent, String exportDir, String mainPath){
		System.out.println("mainPath = "+ mainPath);
		System.out.println("url of shape = "+shape.getURL(false));
		try{
			// If this model is created from scratch and this shape is primitive, shape.getUrl() == null
			// If this shape is loaded by Inline node, mainPath != shape.getUrl()
			// If this shape is directry written in the main file, mainPath == shape.getUrl() 
			if (shape.getURL(false)==null || shape.getURL(false).equals(mainPath)){
				writer.write(indent+"Transform {\n");
				if (!valueEquals(shape.getProperty("translation"),"0.0 0.0 0.0 ")){
					writer.write(indent+"  translation "+shape.getProperty("translation")+"\n");
				}
				if (!isZero(shape.getProperty("rotation"),3)){
					writer.write(indent+"  rotation "+shape.getProperty("rotation")+"\n");
				}
				writer.write(indent+"  children[\n");
				writer.write(indent+"    Shape{\n");
				exportGeometry(writer, shape, indent+"      ");
				if (shape.appearances_[0] != null){
					writer.write(indent+"      appearance Appearance{\n");
					if (shape.materials_[0] != null){
						MaterialInfo mat = shape.materials_[0];
						writer.write(indent+"        material Material{\n");
						writer.write(indent+"          diffuseColor "
								+mat.diffuseColor[0]+" "+mat.diffuseColor[1]+" "+mat.diffuseColor[2]+"\n");
						writer.write(indent+"        }\n");
					}
					if(shape.textures_[0] != null){
						TextureInfo tex = shape.textures_[0];
						writer.write(indent+"        texture	ImageTexture {\n");
						String url = tex.url;
						url = url.replace('\\','/');
						writer.write(indent+"          url \""+_absPath2relPath(url, exportDir)+"\"\n");
						writer.write(indent+"        }\n");
					}
					writer.write(indent+"      }\n");
				}
				writer.write(indent+"    }\n");
				writer.write(indent+"  ]\n");
				writer.write(indent+"}\n");
			}else{
				writer.write(indent+"Transform {\n");
				if (!valueEquals(shape.getProperty("translation"),"0.0 0.0 0.0 ")){
					writer.write(indent+"  translation "+shape.getProperty("translation")+"\n");
				}
				if (!isZero(shape.getProperty("rotation"),3)){
					writer.write(indent+"  rotation "+shape.getProperty("rotation")+"\n");
				}
				writer.write(indent+"  children[\n");
				writer.write(indent+"Inline { url \""+_absPath2relPath(shape.getURL(false), exportDir)+"\" }\n");
				writer.write(indent+"  ]\n");
				writer.write(indent+"}\n");
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * @brief convert absolute path into relative path
	 * @param absPath absolute path
	 * @param baseDir base directory
	 * @return relative path from the base directory
	 */
	private static String _absPath2relPath(String absPath, String baseDir){
		Pattern localPattern = Pattern.compile("\\\\");
		//Matcher localMatcher = localPattern.matcher(File.separator); は使わないで、すべて"/"として扱えるように前処理する
		Matcher localMatcher = localPattern.matcher("/");
		String localStr = localMatcher.replaceAll("\\\\\\\\");
		
		String [] dirs1 = absPath.split( localStr );
		String [] dirs2 = baseDir.split( localStr );
		String relPath = "";
		int cnt=0;
		//Linux MacOSの場合とWindowsの場合で処理を分岐する
        if (System.getProperty("os.name").equals("Linux") || System.getProperty("os.name").equals("Mac OS X")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    		while (cnt < dirs1.length && cnt < dirs2.length && dirs1[cnt].equals(dirs2[cnt])) cnt++;
    		for (int i=0; i<dirs2.length-cnt; i++) relPath += "../";
    		for (int i=cnt; i<dirs1.length; i++){
    			relPath += dirs1[i];
    			if (i != dirs1.length-1) relPath += "/";
    		}
        }else{
        	if(dirs1[0].equals(dirs2[0])) {
	    		while (cnt < dirs1.length && cnt < dirs2.length && dirs1[cnt].equals(dirs2[cnt])) cnt++;
	    		for (int i=0; i<dirs2.length-cnt; i++) relPath += "../";
        	}
    		for (int i=cnt; i<dirs1.length; i++){
    			relPath += dirs1[i];
    			if (i != dirs1.length-1) relPath += "/";
    		}
        }
		return relPath;
	}
	
	public static void exportGeometry(BufferedWriter writer, GrxShapeItem shape, String indent){
		try{
			ShapeInfo info = shape.shapes_[0];
			ShapePrimitiveType ptype = info.primitiveType;
			float [] pparams = info.primitiveParameters;
			
			if (ptype == ShapePrimitiveType.SP_MESH){
				writer.write(indent+"geometry IndexedFaceSet {\n");
				writer.write(indent+"ccw TRUE\n");
				writer.write(indent+"coord Coordinate {\n");
				writer.write(indent+"point [\n");
				for(int i=0; i<info.vertices.length;)
					writer.write(indent+info.vertices[i++]+" "+info.vertices[i++]+" "+info.vertices[i++]+",\n");
				writer.write(indent+"]\n");
				writer.write(indent+"}\n");
				writer.write(indent+"coordIndex [\n");
				for(int i=0; i<info.triangles.length;)
					writer.write(indent+info.triangles[i++]+" "+info.triangles[i++]+" "+info.triangles[i++]+" -1,\n");			
				writer.write(indent+"]\n");
				AppearanceInfo appinfo = shape.appearances_[0];
				if(appinfo != null){
					if(appinfo.normals.length != 0){
						if(!appinfo.normalPerVertex)
							writer.write(indent+"normalPerVertex FALSE\n");
						writer.write(indent+"normal Normal  {\n");
						writer.write(indent+"vector [\n");
						for(int i=0; i<appinfo.normals.length;)
							writer.write(indent+appinfo.normals[i++]+" "+appinfo.normals[i++]+" "+appinfo.normals[i++]+",\n");
						writer.write(indent+"]\n");
						writer.write(indent+"}\n");
					}
					if(appinfo.normalIndices.length != 0){
						writer.write(indent+"normalIndex [\n");
						if(appinfo.normalPerVertex){
							for(int i=0; i<appinfo.normalIndices.length;)
								writer.write(indent+appinfo.normalIndices[i++]+" "+appinfo.normalIndices[i++]+" "+appinfo.normalIndices[i++]+" -1,\n");
						}else{
							for(int i=0; i<appinfo.normalIndices.length; i++){
								if(i%3==0) writer.write(indent+"  ");
								writer.write(appinfo.normalIndices[i]+" ");
								if(i%3==2) writer.write("\n");
							}
						}
						writer.write(indent+"]\n");
					}
					if(!appinfo.solid)
						writer.write(indent+"solid FALSE\n");
					if (appinfo.creaseAngle != 0.0f)
						writer.write(indent+"creaseAngle "+ appinfo.creaseAngle+"\n");
					if(appinfo.colors.length != 0){
						if(!appinfo.colorPerVertex)
							writer.write(indent+"colorPerVertex FALSE\n");
						writer.write(indent+"color Color  {\n");
						writer.write(indent+"color [\n");
						for(int i=0; i<appinfo.colors.length;)
							writer.write(indent+appinfo.colors[i++]+" "+appinfo.colors[i++]+" "+appinfo.colors[i++]+",\n");
						writer.write(indent+"]\n");
						writer.write(indent+"}\n");
					}
					if(appinfo.colorIndices.length != 0){
						writer.write(indent+"colorIndex [\n");
						if(appinfo.colorPerVertex){
							for(int i=0; i<appinfo.colorIndices.length;)
								writer.write(indent+appinfo.colorIndices[i++]+" "+appinfo.colorIndices[i++]+" "+appinfo.colorIndices[i++]+" -1,\n");
						}else{
							for(int i=0; i<appinfo.colorIndices.length; i++){
								if(i%3==0) writer.write(indent+"  ");
								writer.write(appinfo.colorIndices[i]+" ");
								if(i%3==2) writer.write("\n");
							}
						}
						writer.write(indent+"]\n");
					}				
					TextureInfo texinfo = shape.textures_[0];
					if(texinfo != null){
						writer.write(indent+"texCoord TextureCoordinate {\n");
						writer.write(indent+"point [\n");
						for(int i=0; i<appinfo.textureCoordinate.length;)
							writer.write(indent+appinfo.textureCoordinate[i++]+" "+appinfo.textureCoordinate[i++]+",\n");
						writer.write(indent+"]\n");
						writer.write(indent+"}\n");
						writer.write(indent+"texCoordIndex [\n");
						for(int i=0; i<appinfo.textureCoordIndices.length;)
							writer.write(indent+appinfo.textureCoordIndices[i++]+" "+appinfo.textureCoordIndices[i++]+" "+appinfo.textureCoordIndices[i++]+" -1,\n");			
						writer.write(indent+"]\n");
					}
				}
				writer.write(indent+"}\n");
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
				writer.write(indent+"  bottomRadius "+pparams[0]+"\n");
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
			}else if (ptype == ShapePrimitiveType.SP_PLANE){
				writer.write(indent+"geometry Plane{\n");
				writer.write(indent+"  size "+pparams[0]+" "+pparams[1]+" "+pparams[2]+"\n");
				writer.write(indent+"}\n");
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private static boolean valueEquals(String s1, String s2){
		final double EPS = 0.00000001;
		String[] ss1 = s1.split(" ");
		String[] ss2 = s2.split(" ");
		int n = ss1.length;
		if(n!=ss2.length)
			return false;
		for(int i=0; i<n; i++){
			try{
				if( Math.abs(Double.parseDouble(ss1[i]) - Double.parseDouble(ss2[i])) > EPS )
					return false;
			}catch(Exception ex){
				return false;
			}
		}
		return true;
	}
	
	private static boolean isZero(String s1, int i){
		String[] ss1 = s1.split(" ");
		if(i < ss1.length ){
			try{
				if( Math.abs(Double.parseDouble(ss1[i])) < 1e-10 )
					return true;
				else
					return false;
			}catch(Exception ex){
				return false;
			}
		}else
			return false;
	}
}