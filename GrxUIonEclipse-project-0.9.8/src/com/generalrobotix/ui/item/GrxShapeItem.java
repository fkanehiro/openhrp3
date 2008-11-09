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
import java.awt.image.DataBufferInt;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import jp.go.aist.hrp.simulator.AppearanceInfo;
import jp.go.aist.hrp.simulator.MaterialInfo;
import jp.go.aist.hrp.simulator.ShapeInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;
import jp.go.aist.hrp.simulator.TextureInfo;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;

import com.generalrobotix.ui.GrxPluginManager;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.picking.PickTool;

@SuppressWarnings("serial")
/**
 * @brief sensor item
 */
public class GrxShapeItem extends GrxTransformItem{
    public ShapeInfo shapeInfo_;
    public AppearanceInfo appearanceInfo_;
    public MaterialInfo materialInfo_;
    public TextureInfo textureInfo_;
    public double[] transform_;

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
    public GrxShapeItem(String name, GrxPluginManager manager, double [] transform,
    		ShapeInfo shapeInfo, AppearanceInfo appearanceInfo, MaterialInfo materialInfo, TextureInfo textureInfo) {
    	super(name, manager);

    	transform_ = transform;
    	shapeInfo_ = shapeInfo;
    	appearanceInfo_ = appearanceInfo;
    	materialInfo_ = materialInfo;
    	textureInfo_ = textureInfo;

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
        translation(pos);
        double [] rot = new double[4];
        a4d.get(rot);
        rotation(rot);

        tg_.setTransform(t3d);
        if(shapeInfo.primitiveType == ShapePrimitiveType.SP_MESH ){
        	Shape3D linkShape3D = createShape3D(shapeInfo, appearanceInfo, materialInfo, textureInfo);
        	tg_.addChild(linkShape3D);
        }else{
        	Primitive primitive = createPrimitive(shapeInfo, appearanceInfo, materialInfo, textureInfo);
        	tg_.addChild(primitive);
        }

		getMenu().clear();

		Action item;

		// rename
		item = new Action(){
			public String getText(){
				return "rename";
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, null,
						"Input new name.", getName(),null);
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					rename( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// delete
		item = new Action(){
			public String getText(){
				return "delete";
			}
			public void run(){
				if( MessageDialog.openQuestion( null, "delete shape",
						"Are you sure to delete " + getName() + " ?") )
					delete();
			}
		};
		setMenuItem(item);
		setURL(shapeInfo_.url);
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
    }

    private Appearance createAppearance(AppearanceInfo appearanceInfo, MaterialInfo materialInfo, TextureInfo textureInfo){
        Appearance appearance = new Appearance();
        appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
        appearance.setCapability(Appearance.ALLOW_POLYGON_ATTRIBUTES_READ);
        appearance.setCapability(Appearance.ALLOW_MATERIAL_READ);

        PolygonAttributes pa = new PolygonAttributes();
        pa.setCapability(PolygonAttributes.ALLOW_MODE_READ);
        pa.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
        pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
        pa.setCullFace(PolygonAttributes.CULL_NONE);
        pa.setBackFaceNormalFlip(true);
        appearance.setPolygonAttributes(pa);

        return appearance;
    }
    
	/**
     * @brief create Shape3D object from shapeInfo, appearanceInfo, MaterialInfo and TextureInfo
     * @param shapeInfo shape information
     * @param appearanceInfo appearance information
     * @param materialInfo material information
     * @param textureInfo texture information
     * @return created shape
     */
    @SuppressWarnings("deprecation")
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
        
        Appearance appearance = createAppearance(appearanceInfo, materialInfo, textureInfo);
        if (appearanceInfo != null){
            setColors(geometryInfo, shapeInfo, appearanceInfo);
            setNormals(geometryInfo, shapeInfo, appearanceInfo);

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
        shape3D.setAppearance(appearance);

        return shape3D;
    }

    private Primitive createPrimitive
    (ShapeInfo shapeInfo, AppearanceInfo appearanceInfo, MaterialInfo materialInfo, TextureInfo textureInfo){
    	
        Appearance appearance = createAppearance(appearanceInfo, materialInfo, textureInfo);
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

        int flag = Primitive.GENERATE_NORMALS | Primitive.GENERATE_TEXTURE_COORDS | Primitive.ENABLE_GEOMETRY_PICKING;
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

    private void setMaterial(Appearance appearance, MaterialInfo materialInfo){
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

    private void setTexture( Appearance appearance, TextureInfo textureInfo ){
        TextureInfoLocal texInfo = new TextureInfoLocal(textureInfo);
        if(texInfo.url.length()==0){
            if((texInfo.width != 0) && (texInfo.height != 0)){
                ImageComponent2D icomp2d = texInfo.readImage;
                Texture2D texture2d = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, texInfo.width, texInfo.height);
                texture2d.setImage(0, icomp2d);
                appearance.setTexture(texture2d);
            }
        }else{
            //System.out.println("url: "+texInfo.url);
            TextureLoader tloader = new TextureLoader(texInfo.url, null);  
            Texture texture = tloader.getTexture();
            appearance.setTexture(texture);
        }
        TextureAttributes texAttrBase =  new TextureAttributes();
        texAttrBase.setTextureMode(TextureAttributes.REPLACE);
        appearance.setTextureAttributes(texAttrBase);
    }


    private Material createMaterial(MaterialInfo materialInfo){

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
    	}else if(property.equals("translation")){
    		translation(value);
    	}else if(property.equals("rotation")){
    		rotation(value);
    	}else{
    		System.out.println("GrxShapeItem.propertyChanged() : unknown property : "+property);
    		return false;
    	}
    	return true;
    }
    // ##### [Changed] NewModelLoader.IDL
    //==================================================================================================
    /*!
      @brief		"TextureInfoLocal" class
      @author		ErgoVision
      @version	0.00
      @date		2008-04-06 M.YASUKAWA <BR>
      @note		2008-04-06 M.YASUKAWA modify <BR>
      @note		"TextureInfoLocal" class
    */
    //==================================================================================================
    public class TextureInfoLocal
    {
//		public	ImageData		image;
        public	short			numComponents;
        public	short			width;
        public	short			height;
        public	boolean			repeatS;
        public	boolean			repeatT;
        public  ImageComponent2D readImage;
        String url;

        public TextureInfoLocal(TextureInfo texinfo) {
            width = texinfo.width;
            height = texinfo.height;
            numComponents = texinfo.numComponents;
            repeatS = texinfo.repeatS;
            repeatT = texinfo.repeatT;
            url = texinfo.url;
            if((width == 0) || (height == 0)){
//System.out.println( "   TextureInfoLocal width = 0  & height = 0  => No Generate " );
                numComponents = 3;
                repeatS = false;
                repeatT = false;
                width = 0;
                height = 0;
                return;
            }

            // set TextureInfo image
//System.out.println( "   TextureInfo.image  " );
            // create color infomation for reading color buffer
            // type int, (Alpha:8bit,) R:8bit, G:8bit, B:8bit
            BufferedImage bimageRead = null;
            readImage = null;

            bimageRead = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            //int[] imagefield =((DataBufferInt)bimageRead.getRaster().getDataBuffer()).getData();
            //System.out.println( "   imagefield =((DataBufferInt)bimageRead.getRaster().getDataBuffer()).getData()" );
            byte mByteCode;
            byte rByteCode;
            byte gByteCode;
            byte bByteCode;
            byte aByteCode;
            int mCode;
            int rCode;
            int gCode;
            int bCode;
            int aCode;
            int rgbCode;
            int[] pixels;
            pixels = ( (DataBufferInt)bimageRead.getRaster().getDataBuffer() ).getData();
            int img_i=0;
            int img_j=0;
            int imgsize = texinfo.image.length;
            int imgrgbsize = imgsize/3;
            //System.out.println( "   texinfo.image.length   = " + imgsize + " imgrgbsize = " + imgrgbsize );

            for(int img_a=0; img_a<imgrgbsize; img_a++){

                switch (numComponents) {

                    // 1byte:Kido MonoColor
                case 1:

                    mByteCode = (byte)(texinfo.image[img_a]);
                    mCode = mByteCode & 0xFF;
                    rCode = mCode;
                    gCode = mCode;
                    bCode = mCode;

                    rgbCode = rCode * 0x10000 + gCode * 0x100 + bCode;

                    //            rgbCode =(  ( ((mByteCode) & 0xE0) << 16 ) | ( ((mByteCode) & 0xE0) << 13 ) | ( ((mByteCode) & 0x00C0) << 10 ) |
                    //( ((mByteCode) & 0x1C) << 11 ) | ( ((mByteCode) & 0x1C) <<  8 ) | ( ((mByteCode) & 0x0018) <<  5 ) |
                    //( ((mByteCode) & 0x03) <<  6 ) | ( ((mByteCode) & 0x03) <<  4 ) |
                    //( ((mByteCode) & 0x03) <<  2 ) | ( ((mByteCode) & 0x03)       ) );

//System.out.println( "   bimageRead numComponents = 1 rgbCode: " + rgbCode );


                    //bimageRead.setRGB(img_i, img_j, rgbCode);
                    pixels[ (width * img_j) + img_i ] = rgbCode;

                    break;

                    // 1byte:Kido 2byte:Transparency
                case 2:
                    mByteCode = (byte)(texinfo.image[img_a]);
                    mCode = mByteCode & 0xFF;
                    aByteCode = (byte)(texinfo.image[img_a * 2 + 1]);
                    aCode = aByteCode & 0xFF;
                    rCode = mCode;
                    gCode = mCode;
                    bCode = mCode;

                    //            rgbCode =(  ( ((mByteCode) & 0xE0) << 16 ) | ( ((mByteCode) & 0xE0) << 13 ) | ( ((mByteCode) & 0x00C0) << 10 ) |
                    //( ((mByteCode) & 0x1C) << 11 ) | ( ((mByteCode) & 0x1C) <<  8 ) | ( ((mByteCode) & 0x0018) <<  5 ) |
                    //( ((mByteCode) & 0x03) <<  6 ) | ( ((mByteCode) & 0x03) <<  4 ) |
                    //( ((mByteCode) & 0x03) <<  2 ) | ( ((mByteCode) & 0x03)       ) );
                    rgbCode = rCode * 0x10000 + gCode * 0x100 + bCode;

                    rgbCode = aCode * 0x1000000 + rgbCode;

//System.out.println( "   bimageRead numComponents = 2 rgbCode: " + rgbCode );

                    //bimageRead.setRGB(img_i, img_j, rgbCode);
                    pixels[ (width * img_j) + img_i ] = rgbCode;
                    break;

                    // RGB
                case 3:
                    rByteCode = (byte)(texinfo.image[img_a * 3]);
                    rCode = rByteCode & 0xFF;
                    gByteCode = (byte)(texinfo.image[img_a * 3 + 1]);
                    gCode = gByteCode & 0xFF;
                    bByteCode = (byte)(texinfo.image[img_a * 3 + 2]);
                    bCode = bByteCode & 0xFF;
//System.out.println( "   bimageRead R: " + rCode  + ", G: " + gCode  + ", B: " + bCode + ")");

                    rgbCode = rCode * 0x10000 + gCode * 0x100 + bCode;

                    //bimageRead.setRGB(img_i, img_j, rgbCode);
                    pixels[ (width * img_j) + img_i ] = rgbCode;
                    //System.out.println( "   bimageRead.setRGB( " + img_i  + "," + img_j  + "," + rgbCode + ")");
                    //img_i++;
                    //if(img_i >= width){
                    //    img_i = 0;
                    //    img_j++;
                    //}

                    break;

                    // RGB+Transparency
                case 4:
                    rByteCode = (byte)(texinfo.image[img_a * 4]);
                    rCode = rByteCode & 0xFF;
                    gByteCode = (byte)(texinfo.image[img_a * 4 + 1]);
                    gCode = gByteCode & 0xFF;
                    bByteCode = (byte)(texinfo.image[img_a * 4 + 2]);
                    bCode = bByteCode & 0xFF;
                    aByteCode = (byte)(texinfo.image[img_a * 4 + 3]);
                    aCode = aByteCode & 0xFF;
//System.out.println( "   bimageRead R: " + rCode  + ", G: " + gCode  + ", B: " + bCode + ", Alfa: " + aCode + ")");

                    rgbCode =  aCode * 0x1000000 + rCode * 0x10000 + gCode * 0x100 + bCode;

                    //bimageRead.setRGB(img_i, img_j, rgbCode);
                    pixels[ (width * img_j) + img_i ] = rgbCode;
                    //System.out.println( "   bimageRead.setRGB( " + img_i  + "," + img_j  + "," + rgbCode + ")");
                    //img_i++;
                    //if(img_i >= width){
                    //    img_i = 0;
                    //    img_j++;
                    //}

                    break;

                default:
                    rByteCode = (byte)(texinfo.image[img_a * 3]);
                    rCode = rByteCode & 0xFF;
                    gByteCode = (byte)(texinfo.image[img_a * 3 + 1]);
                    gCode = gByteCode & 0xFF;
                    bByteCode = (byte)(texinfo.image[img_a * 3 + 2]);
                    bCode = bByteCode & 0xFF;
//System.out.println( "   bimageRead R: " + rCode  + ", G: " + gCode  + ", B: " + bCode + ")");

                    rgbCode = rCode * 65536 + gCode * 256 + bCode;

                    //bimageRead.setRGB(img_i, img_j, rgbCode);
                    pixels[ (width * img_j) + img_i ] = rgbCode;
                    //System.out.println( "   bimageRead.setRGB( " + img_i  + "," + img_j  + "," + rgbCode + ")");
                    //img_i++;
                    //if(img_i >= width){
                    //    img_i = 0;
                    //    img_j++;
                    //}
                    break;
                }

                img_i++;
                if(img_i >= width){
                    img_i = 0;
                    img_j++;
                }
            }


            readImage = new ImageComponent2D(ImageComponent.FORMAT_RGB, bimageRead);
//System.out.println( "   new ImageComponent2D(ImageComponent.FORMAT_RGB, bimageRead)"  );


            // set TextureInfo repeatS
            repeatS = texinfo.repeatS;
//System.out.println( "   TextureInfo.repeatS   = " + repeatS );

            // set TextureInfo repeatT
            repeatT = texinfo.repeatT;
//System.out.println( "   TextureInfo.repeatT   = " + repeatT );
        }
    }
    // ##### [Changed]

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

				
}
