/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * General Robotix Inc.
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/*
 *  GrxModelItem.java
 *
 *  @author Yuichiro Kawasumi (General Robotix, Inc.)
 *  @author Shin'ichiro Nakaoka (AIST)
 */

package com.generalrobotix.ui.item;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import jp.go.aist.hrp.simulator.AppearanceInfo;
import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.SceneInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;
import jp.go.aist.hrp.simulator.TextureInfo;
import jp.go.aist.hrp.simulator.TransformedShapeIndex;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.util.AxisAngle4d;
import com.generalrobotix.ui.util.MessageBundle;
import com.generalrobotix.ui.view.tdview.SceneGraphModifier;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.picking.PickTool;

@SuppressWarnings("serial") //$NON-NLS-1$
/**
 * @brief sensor item
 */
public class GrxShapeItem extends GrxShapeTransformItem{
    public ShapeInfo[] shapes_;
    public AppearanceInfo[] appearances_;
    public MaterialInfo[] materials_;
    public TextureInfo[] textures_;
    //public double transform_;
	private int primitiveFlag = Primitive.GEOMETRY_NOT_SHARED | Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS;
	private Appearance appearance_=null;
	private BranchGroup bg_;

	/**
     * @brief constructor
     * @param name name of this item
     * @param manager PluginManager
     * @param transform Transform retrieved through ModelLoader
     * @param shapeInfo ShapeInfo retrieved through ModelLoader
     * @param appearanceInfo AppearanceInfo retrieved through ModelLoader
     * @param materialInfo MaterialInfo retrieved through ModelLoader
     * @param textureInfo TextureInfo retrieved through ModelLoader
     */
   	public GrxShapeItem(String name, GrxPluginManager manager, GrxModelItem model){
   		super(name, manager, model);
   		bg_ = new BranchGroup();
    	bg_.setCapability(BranchGroup.ALLOW_DETACH);
    	bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        
   	}
   	
   	public void loadShape(Matrix4d shapeT, int index, Matrix4d segmentT) {
   		GrxModelItem model = model();
    	
    	shapes_ = new ShapeInfo[1];
        appearances_ = new AppearanceInfo[1];
        materials_ = new MaterialInfo[1];
        textures_ = new TextureInfo[1];

        Matrix4d invSegmentT = new Matrix4d();
        invSegmentT.invert(segmentT);
        Matrix4d shapeT0 = new Matrix4d();
        shapeT0.mul(invSegmentT, shapeT);
        Transform3D t3d = new Transform3D(shapeT0);
   	
        setShapeInfofromModel((short) index, 0);
        Shape3D shape3d = createShape3D(shapes_[0], appearances_[0], materials_[0], textures_[0]);
    	bg_.addChild(shape3d);
    	tg_.addChild(bg_);
        setPrimitiveProperty(model.shapes[index]);
    	
       	tg_.setTransform(t3d);
        setURL(model.shapes[index].url);
		
        initialize(t3d);

   	}
   	
   	public void createnewPrimitiveShape(int type){
   		shapes_ = new ShapeInfo[1];
    	shapes_[0] = new ShapeInfo();
    	appearances_ = new AppearanceInfo[1];
    	appearances_[0] = new AppearanceInfo();
        materials_ = new MaterialInfo[1];
        materials_[0] = new MaterialInfo();
        textures_ = new TextureInfo[1];
        textures_[0] = null;
        
        Appearance appearance = createAppearance(); 
        materials_[0].diffuseColor = new float[3];
        materials_[0].diffuseColor[0] = materials_[0].diffuseColor[1] = materials_[0].diffuseColor[2] =0.8f;
        materials_[0].emissiveColor = new float[3];
        materials_[0].emissiveColor[0] = materials_[0].emissiveColor[1] = materials_[0].emissiveColor[2] =0.0f;
        materials_[0].specularColor = new float[3];
        materials_[0].specularColor[0] = materials_[0].specularColor[1] = materials_[0].specularColor[2] =0.0f;
        materials_[0].ambientIntensity = 0.2f;
        materials_[0].shininess = 0.2f;
        materials_[0].transparency = 0.0f;
        setMaterial(appearance, materials_[0]);
        appearance_ = appearance;
   	
        Primitive primitive=null;
    	switch(type){
    	case ShapePrimitiveType._SP_BOX :
	    	float[] size=new float[3];
	    	size[0] = size[1] = size[2] = 1.0f;
	    	primitive = new Box(size[0]/2, size[1]/2, size[2]/2, primitiveFlag, appearance);
	    	shapes_[0].primitiveType = ShapePrimitiveType.SP_BOX;
			shapes_[0].primitiveParameters = size;
			setFltAry("size", size); //$NON-NLS-1$
			break;
    	case ShapePrimitiveType._SP_CONE :
    		float bottomRadius = 1.0f;
    		float height = 2.0f;
    		primitive = new Cone(bottomRadius, height, primitiveFlag, appearance);
    		shapes_[0].primitiveType = ShapePrimitiveType.SP_CONE;
    		float[] param = new float[4];
    		param[0] = bottomRadius;
    		param[1] = height;
    		param[2] = 1;   
    		param[3] = 1;	
			shapes_[0].primitiveParameters = param;
			setFlt("bottomRadius", bottomRadius); //$NON-NLS-1$
			setFlt("height", height); //$NON-NLS-1$
			setProperty("side","true"); //$NON-NLS-1$ //$NON-NLS-2$
			setProperty("bottom","true"); //$NON-NLS-1$ //$NON-NLS-2$
    		break;
    	case ShapePrimitiveType._SP_CYLINDER :
    		float radius = 1.0f;
    		height = 2.0f;
    		primitive = new Cylinder(radius, height, primitiveFlag, appearance);
    		shapes_[0].primitiveType = ShapePrimitiveType.SP_CYLINDER;
    		param = new float[5];
    		param[0] = radius;
    		param[1] = height;
    		param[2] = 1;   
    		param[3] = 1;	
    		param[4] = 1;
			shapes_[0].primitiveParameters = param;
			setFlt("radius", radius); //$NON-NLS-1$
			setFlt("height", height); //$NON-NLS-1$
			setProperty("side","true"); //$NON-NLS-1$ //$NON-NLS-2$
			setProperty("bottom","true"); //$NON-NLS-1$ //$NON-NLS-2$
			setProperty("top","true"); //$NON-NLS-1$ //$NON-NLS-2$
    		break;
    	case ShapePrimitiveType._SP_SPHERE :
    		radius = 1.0f;
    		primitive = new Sphere(radius, primitiveFlag, appearance);
    		shapes_[0].primitiveType = ShapePrimitiveType.SP_SPHERE;
    		param = new float[1];
    		param[0] = radius;
    		shapes_[0].primitiveParameters = param;
			setFlt("radius", radius); //$NON-NLS-1$
			break;
    	default :
    		break;	
    	}
    	bg_.addChild(primitive);
    	Transform3D t3d = new Transform3D();
    	tg_.setTransform(t3d);
    	tg_.addChild(bg_);

    	initialize(t3d);
    	setFltAry("diffuseColor", materials_[0].diffuseColor); //$NON-NLS-1$
   	}
 	
   	public void loadInlineShape(Matrix4d[] shapeT, Matrix4d inlinedT, int[] index, Matrix4d segmentT){
   		int n = index.length;
    	shapes_ = new ShapeInfo[n];
    	appearances_ = new AppearanceInfo[n];
    	materials_ = new MaterialInfo[n];
    	textures_ = new TextureInfo[n];
    	for(int i=0; i<n; i++){
    		setShapeInfofromModel((short) index[i], i);
    	} 
    	
    	setShape(shapeT, inlinedT, n, segmentT);
    	setURL(model().shapes[index[0]].url);
   	}
 
   	private void setShape(Matrix4d[] shapeT, Matrix4d inlinedT, int n, Matrix4d segmentT){
   		Matrix4d invertIT = new Matrix4d();
   		invertIT.invert(inlinedT);
    	for(int i=0; i<n; i++){
    		TransformGroup tfg = new TransformGroup();
    		Matrix4d shapeT0 = new Matrix4d();
    		shapeT0.mul(invertIT, shapeT[i]);
    		Transform3D transform3d = new Transform3D(shapeT0);
    		tfg.setTransform(transform3d);
    		
     		Shape3D linkShape3D = createShape3D(shapes_[i], appearances_[i], materials_[i], textures_[i]);
        	tfg.addChild(linkShape3D);
    		tg_.addChild(tfg);
    	}
    	Matrix4d invSegmentT = new Matrix4d();
    	invSegmentT.invert(segmentT);
        Matrix4d inlinedT0 = new Matrix4d();
        inlinedT0.mul(invSegmentT, inlinedT);
   		Transform3D t3d = new Transform3D(inlinedT0);
       	tg_.setTransform(t3d);
		
        initialize(t3d);
   	}
   	
   	public void loadnewInlineShape(SceneInfo sInfo){  		
   		ShapeInfo[] shapes = sInfo.shapes();
        AppearanceInfo[] appearances = sInfo.appearances();
        MaterialInfo[] materials = sInfo.materials();
        TextureInfo[] textures = sInfo.textures();
   		
        TransformedShapeIndex[] tsi = sInfo.shapeIndices();
    	int n = tsi.length;
    	shapes_ = new ShapeInfo[n];
    	appearances_ = new AppearanceInfo[n];
    	materials_ = new MaterialInfo[n];
    	textures_ = new TextureInfo[n];
    	Matrix4d[] shapeT = new Matrix4d[n];
    	for(int k=0; k<n; k++){
    		ShapeInfo shapeInfo = shapes[tsi[k].shapeIndex];
   	        AppearanceInfo appearanceInfo = null;
   	        MaterialInfo materialInfo = null;
   	        TextureInfo textureInfo = null;
   	        if (shapeInfo.appearanceIndex >= 0){
   	            appearanceInfo = appearances[shapeInfo.appearanceIndex];
   	             if (appearanceInfo.materialIndex >= 0){
   	                materialInfo = materials[appearanceInfo.materialIndex];
   	            }
   	            if (appearanceInfo.textureIndex >= 0){
   	            	textureInfo = textures[appearanceInfo.textureIndex];
   	            }
   	        }
   	        shapes_[k] = shapeInfo;
   	        appearances_[k] = appearanceInfo;
   	        materials_[k] = materialInfo;
   	        textures_[k] = textureInfo;
   	        double[] m = tsi[k].transformMatrix;
   	        shapeT[k] = new Matrix4d(m[0],m[1],m[2],m[3],m[4],m[5],m[6],m[7],m[8],m[9],m[10],m[11],0,0,0,1);
    	} 
        setShape(shapeT, new Matrix4d(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1), n, new Matrix4d(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1));
   	}
   	
   	protected void initialize(Transform3D t3d){
   		Vector3d trans = new Vector3d();
        Matrix3d rotat = new Matrix3d();
        t3d.get(rotat, trans);

        double [] pos = new double[3];
        trans.get(pos);
        translation(pos);
        AxisAngle4d a4d = new AxisAngle4d();
        rotat.normalize();
        a4d.setMatrix(rotat);
        double [] rot = new double[4];
        a4d.get(rot);
        rotation(rot);

        resizeBoundingBox();
        getMenu().clear();

		Action item;

		// rename
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxShapeItem.menu.rename"); //$NON-NLS-1$
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, getText(),
						MessageBundle.get("GrxShapeItem.dialog.message.newName"), getName(),null); //$NON-NLS-1$
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					rename( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// delete
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxShapeItem.menu.delete"); //$NON-NLS-1$
			}
			public void run(){
                String mes = MessageBundle.get("GrxShapeItem.dialog.message.delete"); //$NON-NLS-1$
                mes = NLS.bind(mes, new String[]{getName()});
                
				if( MessageDialog.openQuestion( null, MessageBundle.get("GrxShapeItem.dialog.title.delete"), //$NON-NLS-1$
						mes) ){
					delete();
				}
				
			}
		};
		setMenuItem(item);
		
		/* disable copy and paste menus until they are implemented
        // menu item : copy
        item = new Action(){
            public String getText(){
                return "copy";
            }
            public void run(){
                GrxDebugUtil.println("GrxModelItem.GrxShapeItem copy Action");
                manager_.setSelectedGrxBaseItemList();
            }
        };
        setMenuItem(item);

        // menu item : paste
        item = new Action(){
            public String getText(){
                return "paste";
            }
            public void run(){
            }
        };
        setMenuItem(item);
        */
		
		setIcon("segment.png"); //$NON-NLS-1$
    }

    protected Appearance createAppearance(){
        Appearance appearance = new Appearance();
        appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
        appearance.setCapability(Appearance.ALLOW_MATERIAL_READ);
        appearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);

        PolygonAttributes pa = new PolygonAttributes();
        pa.setCapability(PolygonAttributes.ALLOW_MODE_READ);
        pa.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
        pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        pa.setBackFaceNormalFlip(true);
        appearance.setPolygonAttributes(pa);

        return appearance;
    }
    
    private void setShapeInfofromModel(short shapeIndex, int id){
    	ShapeInfo shapeInfo = model().shapes[shapeIndex];
        AppearanceInfo appearanceInfo = null;
        MaterialInfo materialInfo = null;
        TextureInfo textureInfo = null;
        if (shapeInfo.appearanceIndex >= 0){
            appearanceInfo = model().appearances[shapeInfo.appearanceIndex];
             if (appearanceInfo.materialIndex >= 0){
                materialInfo = model().materials[appearanceInfo.materialIndex];
            }
            if (appearanceInfo.textureIndex >= 0){
            	textureInfo = model().textures[appearanceInfo.textureIndex];
            }
        }
        shapes_[id] = shapeInfo;
        appearances_[id] = appearanceInfo;
        materials_[id] = materialInfo;
        textures_[id] = textureInfo;
    }

	/**
     * @brief create Shape3D object from shapeInfo, appearanceInfo, MaterialInfo and TextureInfo
     * @param shapeInfo shape information
     * @param appearanceInfo appearance information
     * @param materialInfo material information
     * @param textureInfo texture information
     * @return created shape
     */
    @SuppressWarnings("deprecation") //$NON-NLS-1$
	private Shape3D createShape3D
    (ShapeInfo shapeInfo, AppearanceInfo appearanceInfo, MaterialInfo materialInfo, TextureInfo textureInfo){
        
        GeometryInfo geometryInfo = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

        // set vertices
        int numVertices = shapeInfo.vertices.length / 3;
        Point3f[] vertices = new Point3f[numVertices];
        for(int i=0; i < numVertices; ++i){
            vertices[i] = new Point3f(shapeInfo.vertices[i*3], shapeInfo.vertices[i*3+1], shapeInfo.vertices[i*3+2]);
        }
        geometryInfo.setCoordinates(vertices);
        geometryInfo.setCoordinateIndices(shapeInfo.triangles);
        
        Appearance appearance = createAppearance();
        if (appearanceInfo != null){
            setColors(geometryInfo, shapeInfo, appearanceInfo);
            setNormals(geometryInfo, shapeInfo, appearanceInfo);
            if(appearanceInfo.solid){
            	PolygonAttributes pa = appearance.getPolygonAttributes();
            	pa.setCullFace(PolygonAttributes.CULL_BACK);
            	appearance.setPolygonAttributes(pa);
            }
            
            if(materialInfo != null)
                setMaterial( appearance, materialInfo);      

            if(textureInfo != null){
                setTexture( appearance, textureInfo);
            

                int numTexCoordinate = appearanceInfo.textureCoordinate.length / 2;
                Point2f[] texCoordinate = new Point2f[numTexCoordinate];
                for(int i=0, j=0; i<numTexCoordinate;  i++)
                    texCoordinate[i] = new Point2f( appearanceInfo.textureCoordinate[j++], appearanceInfo.textureCoordinate[j++] );

                geometryInfo.setTextureCoordinates(texCoordinate);
                geometryInfo.setTextureCoordinateIndices(appearanceInfo.textureCoordIndices);               
            }
        }

        Shape3D shape3D = new Shape3D(geometryInfo.getGeometryArray());
        shape3D.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        shape3D.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        shape3D.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
        shape3D.setCapability(GeometryArray.ALLOW_COUNT_READ);
        PickTool.setCapabilities(shape3D, PickTool.INTERSECT_FULL);
        appearance_ = appearance;
        shape3D.setAppearance(appearance);
 
        return shape3D;
    }
/*
    private Primitive createPrimitive
    (ShapeInfo shapeInfo, AppearanceInfo appearanceInfo, MaterialInfo materialInfo, TextureInfo textureInfo){
    	
        Appearance appearance = createAppearance();
        if(appearanceInfo != null){
            if(materialInfo != null)
                setMaterial( appearance, materialInfo);
            if(textureInfo != null){
                setTexture( appearance, textureInfo);
                TextureAttributes texAttrBase = new TextureAttributes();
                Transform3D t3d = new Transform3D(new Matrix4d(
                    appearanceInfo.textransformMatrix[0], appearanceInfo.textransformMatrix[1], appearanceInfo.textransformMatrix[2], 0,
                    appearanceInfo.textransformMatrix[3], appearanceInfo.textransformMatrix[4], appearanceInfo.textransformMatrix[5], 0, 
                    0, 0, 1, 0,
                    0, 0, 0, 1 ));      

                //System.out.println(t3d.toString());
                texAttrBase.setTextureTransform(t3d);
                appearance.setTextureAttributes(texAttrBase);
            }
        }

        int flag = Primitive.GEOMETRY_NOT_SHARED | Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS | Primitive.ENABLE_GEOMETRY_PICKING;
        if(shapeInfo.primitiveType == ShapePrimitiveType.SP_BOX || shapeInfo.primitiveType == ShapePrimitiveType.SP_PLANE){
            Box box = new Box((float)(shapeInfo.primitiveParameters[0]/2.0), (float)(shapeInfo.primitiveParameters[1]/2.0),
                (float)(shapeInfo.primitiveParameters[2]/2.0), flag, appearance );
            return box;
        }else if( shapeInfo.primitiveType == ShapePrimitiveType.SP_CYLINDER ){
            Cylinder cylinder = new Cylinder(shapeInfo.primitiveParameters[0], shapeInfo.primitiveParameters[1],
                flag, appearance );
            if((int)shapeInfo.primitiveParameters[2]==0)  //TOP
                cylinder.removeChild(cylinder.getShape(Cylinder.TOP));
            if((int)shapeInfo.primitiveParameters[3]==0)  //BOTTOM
                cylinder.removeChild(cylinder.getShape(Cylinder.BOTTOM));
            if((int)shapeInfo.primitiveParameters[4]==0)  //SIDE
                cylinder.removeChild(cylinder.getShape(Cylinder.BODY));
            return cylinder;
        }else if( shapeInfo.primitiveType == ShapePrimitiveType.SP_CONE ){
            Cone cone = new Cone(shapeInfo.primitiveParameters[0], shapeInfo.primitiveParameters[1],
                flag, appearance );
            if((int)shapeInfo.primitiveParameters[2]==0)  //BOTTOM
                cone.removeChild(cone.getShape(Cone.CAP));
            if((int)shapeInfo.primitiveParameters[3]==0)  //SIDE
                cone.removeChild(cone.getShape(Cone.BODY));
            return cone;
        }else if( shapeInfo.primitiveType == ShapePrimitiveType.SP_SPHERE ){
            Sphere sphere = new Sphere(shapeInfo.primitiveParameters[0], flag, appearance );
            return sphere;
        }

        return null; 
    }
*/
    protected void setMaterial(Appearance appearance, MaterialInfo materialInfo){
        if(materialInfo.transparency > 0.0f){
            TransparencyAttributes ta = new TransparencyAttributes(TransparencyAttributes.NICEST, materialInfo.transparency);
            ta.setCapability(TransparencyAttributes.ALLOW_MODE_READ);
            ta.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
            ta.setCapability(TransparencyAttributes.ALLOW_VALUE_READ);
            ta.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
            appearance.setTransparencyAttributes(ta);
        }
        if(materialInfo != null){
            appearance.setMaterial(createMaterial(materialInfo));
        }        
    }

    protected void setTexture( Appearance appearance, TextureInfo textureInfo ){
        TextureInfoLocal texInfo = new TextureInfoLocal(textureInfo);
        if((texInfo.width != 0) && (texInfo.height != 0)){
            ImageComponent2D icomp2d = texInfo.readImage;
            Texture2D texture2d=null;
            switch (texInfo.numComponents) {
                case 1:
                    texture2d = new Texture2D(Texture.BASE_LEVEL, Texture.LUMINANCE, texInfo.width, texInfo.height);
                    break;
                case 2:
                    texture2d = new Texture2D(Texture.BASE_LEVEL, Texture.LUMINANCE_ALPHA, texInfo.width, texInfo.height);
                    appearance.setTransparencyAttributes( new TransparencyAttributes(TransparencyAttributes.BLENDED, 1.0f));
                    break;
                case 3:
                    texture2d = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, texInfo.width, texInfo.height);
                    break;
                case 4:
                    texture2d = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA, texInfo.width, texInfo.height);
                    appearance.setTransparencyAttributes( new TransparencyAttributes(TransparencyAttributes.BLENDED, 1.0f));
                    break;
            }
            texture2d.setImage(0, icomp2d);
            appearance.setTexture(texture2d);
        }else{
        	if(!texInfo.url.equals("")){ //$NON-NLS-1$
        		try{
		            TextureLoader tloader = new TextureLoader(texInfo.url, null);  
		            Texture texture = tloader.getTexture();
		            appearance.setTexture(texture);
        		}catch(Exception ex){
        			System.out.println(ex+" "+texInfo.url); //$NON-NLS-1$
        		}
        	}
        }
        TextureAttributes texAttrBase =  new TextureAttributes();
        texAttrBase.setTextureMode(TextureAttributes.REPLACE);
        appearance.setTextureAttributes(texAttrBase);
    }


    protected Material createMaterial(MaterialInfo materialInfo){

        Material material = new Material();

        float[] dColor = materialInfo.diffuseColor;
        material.setDiffuseColor(new Color3f(dColor[0], dColor[1], dColor[2]));

        float[] sColor = materialInfo.specularColor;
        material.setSpecularColor(new Color3f(sColor[0], sColor[1], sColor[2]));

        float[] eColor = materialInfo.emissiveColor;
        material.setEmissiveColor(new Color3f(eColor[0], eColor[1], eColor[2]));

        float r = materialInfo.ambientIntensity;
        material.setAmbientColor(new Color3f(r * dColor[0], r * dColor[1], r * dColor[2]));

        float shininess = materialInfo.shininess * 127.0f + 1.0f;
        material.setShininess(shininess);

        material.setCapability(Material.ALLOW_COMPONENT_READ);
        material.setCapability(Material.ALLOW_COMPONENT_WRITE);
        
        return material;
    }

    private void setColors(GeometryInfo geometryInfo, ShapeInfo shapeInfo, AppearanceInfo appearanceInfo) {

        int numColors = appearanceInfo.colors.length / 3;

        if(numColors > 0){
            float[] orgColors = appearanceInfo.colors;
            Color3f[] colors = new Color3f[numColors];
            for(int i=0; i < numColors; ++i){
                colors[i] = new Color3f(orgColors[i*3], orgColors[i*3+1], orgColors[i*3+2]);
            }
            geometryInfo.setColors(colors);

            int[] orgColorIndices = appearanceInfo.colorIndices;
            int numOrgColorIndices = orgColorIndices.length;
            int numTriangles = shapeInfo.triangles.length / 3;
            int[] colorIndices = new int[numTriangles * 3];

            if(numOrgColorIndices > 0){
                if(appearanceInfo.colorPerVertex){
                    colorIndices = orgColorIndices;
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        int colorIndex = orgColorIndices[i];
                        for(int j=0; j < 3; ++j){
                            colorIndices[pos++] = colorIndex;
                        }
                    }
                }
            } else {
                if(appearanceInfo.colorPerVertex){
                    for(int i=0; i < colorIndices.length; ++i){
                        colorIndices[i] = shapeInfo.triangles[i];
                    }
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        for(int j=0; j < 3; ++j){
                            colorIndices[pos++] = i;
                        }
                    }

                }
            }
            geometryInfo.setColorIndices(colorIndices);
        }
    }


    private void setNormals(GeometryInfo geometryInfo, ShapeInfo shapeInfo, AppearanceInfo appearanceInfo) {

        int numNormals = appearanceInfo.normals.length / 3;

        if(numNormals == 0){
            NormalGenerator ng = new NormalGenerator(appearanceInfo.creaseAngle);
            ng.generateNormals(geometryInfo);

        } else {

            float[] orgNormals = appearanceInfo.normals;
            Vector3f[] normals = new Vector3f[numNormals];
            for(int i=0; i < numNormals; ++i){
                normals[i] = new Vector3f(orgNormals[i*3], orgNormals[i*3+1], orgNormals[i*3+2]);
            }
            geometryInfo.setNormals(normals);

            int[] orgNormalIndices = appearanceInfo.normalIndices;
            int numOrgNormalIndices = orgNormalIndices.length;
            int numTriangles = shapeInfo.triangles.length / 3;
            int[] normalIndices = new int[numTriangles * 3];

            if(numOrgNormalIndices > 0){
                if(appearanceInfo.normalPerVertex){
                    normalIndices = orgNormalIndices;
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        int normalIndex = orgNormalIndices[i];
                        for(int j=0; j < 3; ++j){
                            normalIndices[pos++] = normalIndex;
                        }
                    }
                }
            } else {
                if(appearanceInfo.normalPerVertex){
                    for(int i=0; i < normalIndices.length; ++i){
                        normalIndices[i] = shapeInfo.triangles[i];
                    }
                } else {
                    int pos = 0;
                    for(int i=0; i < numTriangles; ++i){
                        for(int j=0; j < 3; ++j){
                            normalIndices[pos++] = i;
                        }
                    }

                }
            }

            geometryInfo.setNormalIndices(normalIndices);
        }
    }


    /**
     * @brief update value of property
     * @param property name of property
     * @param value value of property
     * @return true if updated successfully, false otherwise
     */
    public boolean propertyChanged(String property, String value) {
    	if (super.propertyChanged(property, value)){
    	}else if(property.equals("translation")){ //$NON-NLS-1$
    		translation(value);
    	}else if(property.equals("rotation")){ //$NON-NLS-1$
    		rotation(value);
    	}else if(property.equals("size")){ //$NON-NLS-1$
    			size(value);
    	}else if(property.equals("bottomRadius") || property.equals("height") || property.equals("radius") || //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    			property.equals("side") || property.equals("bottom") || property.equals("top")){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		if(shapes_[0].primitiveType==ShapePrimitiveType.SP_CONE){
    			coneSize(property, value);
    		}else if(shapes_[0].primitiveType==ShapePrimitiveType.SP_CYLINDER){
    			cylinderSize(property, value);
    		}else{
    			sphereSize(value);
    		}
    	}else if(property.equals("diffuseColor")){ //$NON-NLS-1$
    			diffuseColor(value);
    	}else
    		return false;
    	return true;
    }

    private void size(float[] newSize){
   		if (newSize != null && newSize.length == 3){
	    	setFltAry("size", newSize); //$NON-NLS-1$
	    	tg_.removeChild(bg_);
	    	bg_ = new BranchGroup();
	       	bg_.setCapability(BranchGroup.ALLOW_DETACH);
	       	bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	       	Primitive primitive = new Box(newSize[0]/2, newSize[1]/2, newSize[2]/2, primitiveFlag, appearance_);
	       	bg_.addChild(primitive);
	       	tg_.addChild(bg_);
	    	resizeBoundingBox();
	    	if (model_ != null) model_.notifyModified();
	    	shapes_[0].primitiveParameters = newSize;
	    }  	
    }

    public void size(String size){
    	float[] newSize = getFltAry(size);
    	size(newSize);
    }
    
    public void coneSize(String property, String size){
    	float bottomRadius = getFlt("bottomRadius", 0.0f); //$NON-NLS-1$
    	float height = getFlt("height", 0.0f); //$NON-NLS-1$
    	boolean side = isTrue("side"); //$NON-NLS-1$
    	boolean bottom = isTrue("bottom"); //$NON-NLS-1$
    	if(property.equals("bottomRadius")){ //$NON-NLS-1$
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		bottomRadius = newsize;
    	}else if(property.equals("height")){ //$NON-NLS-1$
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		height = newsize;
    	}else if(property.equals("side")){ //$NON-NLS-1$
    		boolean flg = size.equals("true"); //$NON-NLS-1$
    		setBool(property, flg);
    		side = flg;
    	}else if(property.equals("bottom")){ //$NON-NLS-1$
    		boolean flg = size.equals("true"); //$NON-NLS-1$
    		setBool(property, flg);
    		bottom = flg;
    	}
	    tg_.removeChild(bg_);
	    bg_ = new BranchGroup();
	    bg_.setCapability(BranchGroup.ALLOW_DETACH);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	    Primitive primitive = new Cone( bottomRadius, height, primitiveFlag, appearance_);   
	    if(!bottom)
	    	primitive.removeChild(primitive.getShape(Cone.CAP));
	    if(!side)
	    	primitive.removeChild(primitive.getShape(Cone.BODY));
	    bg_.addChild(primitive);
	    tg_.addChild(bg_);
	    resizeBoundingBox();
	    if (model_ != null) model_.notifyModified();
	    float[] param = new float[4];
	    param[0] = bottomRadius;
		param[1] = height;
		param[2] = bottom ? 1: 0;   
		param[3] = side? 1 : 0;	
		shapes_[0].primitiveParameters = param;
    }
    
    public void cylinderSize(String property, String size){
    	float radius = getFlt("radius", 0.0f); //$NON-NLS-1$
    	float height = getFlt("height", 0.0f); //$NON-NLS-1$
    	boolean side = isTrue("side"); //$NON-NLS-1$
    	boolean bottom = isTrue("bottom"); //$NON-NLS-1$
    	boolean top = isTrue("top"); //$NON-NLS-1$
    	if(property.equals("radius")){ //$NON-NLS-1$
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		radius = newsize;
    	}else if(property.equals("height")){ //$NON-NLS-1$
    		float newsize = getFlt(size);
    		setFlt(property, newsize );
    		height = newsize;
    	}else if(property.equals("side")){ //$NON-NLS-1$
    		boolean flg = size.equals("true"); //$NON-NLS-1$
    		setBool(property, flg);
    		side = flg;
    	}else if(property.equals("bottom")){ //$NON-NLS-1$
    		boolean flg = size.equals("true"); //$NON-NLS-1$
    		setBool(property, flg);
    		bottom = flg;
    	}else if(property.equals("top")){ //$NON-NLS-1$
    		boolean flg = size.equals("true"); //$NON-NLS-1$
    		setBool(property, flg);
    		top = flg;
    	}
    	
	    tg_.removeChild(bg_);
	    bg_ = new BranchGroup();
	    bg_.setCapability(BranchGroup.ALLOW_DETACH);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	    bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	    Primitive primitive = new Cylinder( radius, height, primitiveFlag, appearance_);
	    bg_.addChild(primitive);
	    if(!bottom)
	    	primitive.removeChild(primitive.getShape(Cylinder.BOTTOM));
	    if(!top)
	    	primitive.removeChild(primitive.getShape(Cylinder.TOP));
	    if(!side)
	    	primitive.removeChild(primitive.getShape(Cylinder.BODY));
	    tg_.addChild(bg_);
	    resizeBoundingBox();
	    if (model_ != null) model_.notifyModified();
	    float[] param = new float[5];
	    param[0] = radius;
		param[1] = height;
		param[2] = top ? 1 : 0;   
		param[3] = bottom? 1 : 0;	
		param[4] = side? 1 : 0;
		shapes_[0].primitiveParameters = param;
    }
    
    private void sphereSize(float[] newSize){
   		if (newSize != null && newSize.length == 1){
	    	setFlt("radius", newSize[0]); //$NON-NLS-1$
	    	tg_.removeChild(bg_);
	    	bg_ = new BranchGroup();
	       	bg_.setCapability(BranchGroup.ALLOW_DETACH);
	       	bg_.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
	        bg_.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	       	Primitive primitive = new Sphere(newSize[0], primitiveFlag, appearance_);
	       	bg_.addChild(primitive);
	       	tg_.addChild(bg_);
	    	resizeBoundingBox();
	    	if (model_ != null) model_.notifyModified();
	    	shapes_[0].primitiveParameters = newSize;
	    }  	
    }

    public void sphereSize(String size){
    	float[] newSize = getFltAry(size);
    	sphereSize(newSize);
    }
    
    void diffuseColor(float[] newValue){
    	if (newValue != null && newValue.length == 3){
    	    setFltAry("diffuseColor", newValue); //$NON-NLS-1$
    		materials_[0].diffuseColor = newValue;
    		setMaterial(appearance_, materials_[0]);
    		if (model_ != null) model_.notifyModified();
    	}
    }
    
    void diffuseColor(String value){
    	float[] newValue = getFltAry(value);
    	diffuseColor(newValue);
    }
    
    public class TextureInfoLocal
    {
        public	short		numComponents;
        public	short		width;
        public	short		height;
        public	boolean	repeatS;
        public	boolean	repeatT;
        public  ImageComponent2D readImage;
        String url;

        public TextureInfoLocal(TextureInfo texinfo) {
            width = texinfo.width;
            height = texinfo.height;
            numComponents = texinfo.numComponents;
            //System.out.println("numComponents= " + numComponents);
            repeatS = texinfo.repeatS;
            repeatT = texinfo.repeatT;
            url = texinfo.url;

            if((width == 0) || (height == 0)){
                numComponents = 3;
                repeatS = false;
                repeatT = false;
                width = 0;
                height = 0;
                return;
            }
            
            //width and height must be power of 2
            short w=1;
            do{
            	w *=2;	
            }while(w<=width);
            short width_new = (short)(w);
            if(width_new-width > width-width_new/2)
            	width_new /=2;
            w=1;
            do{
            	w *=2;	
            }while(w<=height);
            short height_new = (short)(w);
            if(height_new-height > height-height_new/2)
            	height_new /=2;
            
     
            BufferedImage bimageRead=null;
            switch(numComponents){
            case 1:    
                bimageRead = new BufferedImage(width_new, height_new, BufferedImage.TYPE_BYTE_GRAY);
                byte[] bytepixels = ( ( DataBufferByte)bimageRead.getRaster().getDataBuffer() ).getData();
                for(int i=0; i<height_new; i++){
                	for(int j=0; j<width_new; j++){
                		int k = (int)(i*height/height_new)*width+(int)(j*width/width_new);
                		bytepixels[i*width_new+j] = texinfo.image[k];
                	}
                }
                break;
            case 2:
                bimageRead = new BufferedImage(width_new, height_new, BufferedImage.TYPE_USHORT_GRAY);
                short[] shortpixels = ( ( DataBufferUShort)bimageRead.getRaster().getDataBuffer() ).getData();
                for(int i=0; i<height_new; i++){
                	for(int j=0; j<width_new; j++){
                		int k = (int)(i*height/height_new)*width*2+(int)(j*width/width_new)*2;
                		short l = texinfo.image[k];
                		short a = texinfo.image[k+1];
                		shortpixels[i*width_new+j] = (short)((l&0xff) << 8 | (a&0xff)) ;
                	}
                }
                break;
            case 3:
                bimageRead = new BufferedImage(width_new, height_new, BufferedImage.TYPE_INT_RGB);
                int[] intpixels = ( (DataBufferInt)bimageRead.getRaster().getDataBuffer() ).getData();
                for(int i=0; i<height_new; i++){
                	for(int j=0; j<width_new; j++){
                		int k = (int)(i*height/height_new)*width*3+(int)(j*width/width_new)*3;
                		short r = texinfo.image[k];
                		short g = texinfo.image[k+1];
                		short b = texinfo.image[k+2];
                		intpixels[i*width_new+j] = (r&0xff) << 16 | (g&0xff) << 8 | (b&0xff);
                	}
                }
                break;
            case 4:
                bimageRead = new BufferedImage(width_new, height_new, BufferedImage.TYPE_INT_ARGB);
                intpixels = ( (DataBufferInt)bimageRead.getRaster().getDataBuffer() ).getData();
                for(int i=0; i<height_new; i++){
                	for(int j=0; j<width_new; j++){
                		int k = (int)(i*height/height_new)*width*4+(int)(j*width/width_new)*4;
                		short r = texinfo.image[k];
                		short g = texinfo.image[k+1];
                		short b = texinfo.image[k+2];
                		short a = texinfo.image[k+3];
                		intpixels[i*width_new+j] = (a&0xff) << 24 | (r&0xff) << 16 | (g&0xff) << 8 | (b&0xff);
                	}
                }
                break;
            }
            height = height_new;
            width = width_new;    
 
        switch(numComponents){
            case 1:
                readImage = new ImageComponent2D(ImageComponent.FORMAT_CHANNEL8, bimageRead); 
                break;
            case 2:
                readImage = new ImageComponent2D(ImageComponent.FORMAT_LUM8_ALPHA8, bimageRead); 
                break;
            case 3:
                readImage = new ImageComponent2D(ImageComponent.FORMAT_RGB, bimageRead); 
                break;
            case 4:
                readImage = new ImageComponent2D(ImageComponent.FORMAT_RGBA, bimageRead); 
                break;
        }
           
        }
    }

    /**
     * @brief Override clone method
     * @return GrxShapeItem
     */
	public GrxShapeItem clone(){
		GrxShapeItem ret = (GrxShapeItem)super.clone();
/*
        ret.tg_ = new TransformGroup();
        ret.tg_.setCapability(TransformGroup.ALLOW_CHILDREN_EXTEND);
        ret.tg_.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        ret.tg_.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        ret.bg_ = new BranchGroup();
        ret.bg_.setCapability(BranchGroup.ALLOW_DETACH);

        Vector3d v3d = new Vector3d(transform[3], transform[7], transform[11]);
        Matrix3d m3d = new Matrix3d(transform[0], transform[1], transform[2],
                transform[4], transform[5], transform[6],
                transform[8], transform[9], transform[10]);
        AxisAngle4d a4d = new AxisAngle4d();
        a4d.set(m3d);
        Transform3D t3d = new Transform3D();
        t3d.setTranslation(v3d);
        t3d.setRotation(a4d);

        double [] pos = new double[3];
        v3d.get(pos);
        setDblAry("translation", pos);
        double [] rot = new double[4];
        a4d.get(rot);
        setDblAry("rotation", rot);

        tg_.setTransform(t3d);
        Shape3D linkShape3D = createLinkShape3D(shapeInfo, appearanceInfo, materialInfo, textureInfo);
        tg_.addChild(linkShape3D);

        bg_.addChild(tg_);
 */
		
/*    	
	Deep copy suspension list
	public BranchGroup bg_;
	public TransformGroup tg_;
    public GrxLinkItem parent_;
    public ShapeInfo shapeInfo_;
    public AppearanceInfo appearanceInfo_;
    public MaterialInfo materialInfo_;
    public TextureInfo textureInfo_;	
*/
		
		return ret;
	}
	
	void setColor(java.awt.Color color, Node node){
		if (node instanceof Shape3D){
			Color3f c3f = new Color3f(color);
    	    Shape3D s3d = (Shape3D)node;
    	    Appearance app = s3d.getAppearance();
    	    if (app != null){
                Material ma = app.getMaterial();
                if (ma != null){
                    ma.setAmbientColor(c3f);
                }
      	    }
		}else if (node instanceof Primitive){
			Primitive prim = (Primitive)node;
			for (int j=0; j<prim.numChildren(); j++){
				setColor(color, (Shape3D)prim.getChild(j));
			}
		}else if(node instanceof TransformGroup ){
			TransformGroup tfg = (TransformGroup)node;
			for(int j=0; j<tfg.numChildren(); j++){
				setColor(color, tfg.getChild(j));
			}
		}else if(node instanceof BranchGroup ){
			BranchGroup bg = (BranchGroup)node;
			for(int j=0; j<bg.numChildren(); j++){
				setColor(color, bg.getChild(j));
			}
		}
	}
	
	void setColor(java.awt.Color color){
		setColor(color, tg_);
	}
	
	void restoreColor(Node node, int id){
		if(node instanceof Shape3D){
			Shape3D s3d = (Shape3D)node;
			Appearance app = s3d.getAppearance();
    	    setMaterial(app, materials_[id]);
		}else if(node instanceof Primitive){
			Primitive prim = (Primitive)node;
			for (int j=0; j<prim.numChildren(); j++){
				restoreColor((Shape3D)prim.getChild(j), id);
			}
		}else if(node instanceof TransformGroup ){
			TransformGroup tfg = (TransformGroup)node;
			for(int j=0; j<tfg.numChildren(); j++){
				restoreColor(tfg.getChild(j), id);
			}
		}else if(node instanceof BranchGroup ){
			BranchGroup bg = (BranchGroup)node;
			for(int j=0; j<bg.numChildren(); j++){
				restoreColor(bg.getChild(j), id);
			}
		}
	}
	
	void restoreColor(){
		restoreColor(tg_, 0);
	}
	
	private void setPrimitiveProperty(ShapeInfo shapeInfo){
		switch(shapeInfo.primitiveType.value()){
		case ShapePrimitiveType._SP_BOX :
			setFltAry("size", shapeInfo.primitiveParameters); //$NON-NLS-1$
			setFltAry("diffuseColor", materials_[0].diffuseColor); //$NON-NLS-1$
			break;
    	case ShapePrimitiveType._SP_CONE :
			setFlt("bottomRadius", shapeInfo.primitiveParameters[0]); //$NON-NLS-1$
			setFlt("height", shapeInfo.primitiveParameters[1]); //$NON-NLS-1$
			setProperty("bottom",shapeInfo.primitiveParameters[2]==0?"false":"true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			setProperty("side",shapeInfo.primitiveParameters[3]==0?"false":"true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			setFltAry("diffuseColor", materials_[0].diffuseColor); //$NON-NLS-1$
    		break;
    	case ShapePrimitiveType._SP_CYLINDER :
			setFlt("radius", shapeInfo.primitiveParameters[0]); //$NON-NLS-1$
			setFlt("height", shapeInfo.primitiveParameters[1]); //$NON-NLS-1$
			setProperty("top",shapeInfo.primitiveParameters[2]==0?"false":"true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			setProperty("bottom",shapeInfo.primitiveParameters[3]==0?"false":"true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			setProperty("side",shapeInfo.primitiveParameters[4]==0?"false":"true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			setFltAry("diffuseColor", materials_[0].diffuseColor); //$NON-NLS-1$
    		break;
    	case ShapePrimitiveType._SP_SPHERE :
			setFlt("radius", shapeInfo.primitiveParameters[0]); //$NON-NLS-1$
			setFltAry("diffuseColor", materials_[0].diffuseColor); //$NON-NLS-1$
			break;
    	default :
    		break;	 
		}
	}

    @Override
    public ValueEditType GetValueEditType(String key) {
        
        if(shapes_.length == 1 && shapes_[0].primitiveType == ShapePrimitiveType.SP_CONE &&
           (key.equals("side") || key.equals("bottom")))
        {
            return new ValueEditCombo(booleanComboItem_);
        }
        else if(shapes_.length == 1 && shapes_[0].primitiveType == ShapePrimitiveType.SP_CYLINDER &&
                (key.equals("side") || key.equals("bottom") || key.equals("top")))
        {
            return new ValueEditCombo(booleanComboItem_);
        }
        return super.GetValueEditType(key);
    }
}
