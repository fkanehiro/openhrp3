package com.generalrobotix.ui.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Vector;
import java.util.regex.*;

import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;

import com.generalrobotix.ui.item.GrxLinkItem;
import com.generalrobotix.ui.item.GrxModelItem;
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
	        	writer.write("    USE "+links.get(i).getName()+"_Link,\n");
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
			writer.write(indent+"  jointType \""+link.jointType()+"\"\n");
			if (link.jointId() != -1) writer.write(indent+"  jointId "+link.jointId()+"\n");
			if (link.jointType().equals("rotate") || link.jointType().equals("slide")){
				writer.write(indent+"  jointAxis \""+link.getProperty("jointAxis")+"\"\n");
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
				exportSensor(writer, (GrxSensorItem)link.children_.get(i), indent+"    ", exportDir, mainPath);
				}
			}
			writer.write(indent+"    DEF "+link.getName()+"_Link Segment{\n");
			writer.write(indent+"      centerOfMass "+link.getProperty("centerOfMass")+"\n");
			writer.write(indent+"      mass "+link.mass()+"\n");
			writer.write(indent+"      momentsOfInertia [ "+link.getProperty("inertia")+"]\n");
			writer.write(indent+"      children[\n");
			String exported_url = null;
			for (int i=0; i<link.children_.size(); i++){
				if (link.children_.get(i) instanceof GrxShapeItem){
					GrxShapeItem shape = (GrxShapeItem)link.children_.get(i);
					String url = shape.getURL(false);
					if (url == null || (url != null && !url.equals(exported_url))){
						exportShape(writer, shape, indent+"        ", exportDir, mainPath);
						exported_url = url;
					}
				}
			}
			writer.write(indent+"      ]\n");
			writer.write(indent+"    }\n");
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
					writer.write(indent+"  type \""+sensor.getProperty("cameraType")+"\"\n");
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
			// If this model is created from scratch, modelPath == null and all shapes are loaded by Inline node
			// If this shape is loaded by Inline node, modelPath != shape.getUrl()
			// If this shape is directry written in the main file, modelPath == shape.getUrl()
			if (shape.getURL(false).equals(mainPath)){
				writer.write(indent+"Transform {\n");
				if (!shape.getProperty("translation").equals("0.0 0.0 0.0 ")){
					writer.write(indent+"  translation "+shape.getProperty("translation")+"\n");
				}
				if (!shape.getProperty("rotation").equals("0.0 1.0 0.0 0.0 ")){
					writer.write(indent+"  rotation "+shape.getProperty("rotation")+"\n");
				}
				writer.write(indent+"  children[\n");
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
				writer.write(indent+"Inline { url \""+_absPath2relPath(shape.getURL(false), exportDir)+"\" }\n");
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
		Matcher localMatcher = localPattern.matcher(File.separator);
		String localStr = localMatcher.replaceAll("\\\\\\\\");
		
		String [] dirs1 = absPath.split( localStr );
		String [] dirs2 = baseDir.split( localStr );
		int cnt=0;
		while (cnt < dirs1.length && cnt < dirs2.length && dirs1[cnt].equals(dirs2[cnt])) cnt++;
		String relPath = "";
		for (int i=0; i<dirs2.length-cnt; i++) relPath += ".."+File.separator;
		for (int i=cnt; i<dirs1.length; i++){
			relPath += dirs1[i];
			if (i != dirs1.length-1) relPath += File.separator;
		}
		return relPath;
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
			}else if (ptype == ShapePrimitiveType.SP_PLANE){
				writer.write(indent+"geometry Plane{\n");
				writer.write(indent+"  size "+pparams[0]+" "+pparams[1]+" "+pparams[2]+"\n");
				writer.write(indent+"}\n");
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}