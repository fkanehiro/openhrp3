package com.generalrobotix.ui.item;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.BadTransformException;
import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import jp.go.aist.hrp.simulator.ModelLoader;
import jp.go.aist.hrp.simulator.ModelLoaderHelper;
import jp.go.aist.hrp.simulator.SceneInfo;
import jp.go.aist.hrp.simulator.ShapePrimitiveType;
import jp.go.aist.hrp.simulator.TransformedShapeIndex;
import jp.go.aist.hrp.simulator.ModelLoaderPackage.ModelLoaderException;

import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.GrxUIPerspectiveFactory;
import com.generalrobotix.ui.util.AxisAngle4d;
import com.generalrobotix.ui.util.GrxCorbaUtil;
import com.generalrobotix.ui.util.MessageBundle;

@SuppressWarnings("serial")
public class GrxShapeTransformItem extends GrxTransformItem {
	// TransformGroup is parent link local 
	private ArrayList<ShapeTransform> shapeTransforms_;  
	private ArrayList<TransformedShapeIndex> transformedShapeIndices_;
	public GrxShapeTransformItem(String name, GrxPluginManager manager, GrxModelItem model) {
		super(name, manager, model);
		shapeTransforms_ = new ArrayList<ShapeTransform>();
		transformedShapeIndices_ = new ArrayList<TransformedShapeIndex>(); 
	}
	
	protected void addTransformedShapeIndex(TransformedShapeIndex transformedShapeIndex){
		transformedShapeIndices_.add(transformedShapeIndex);
	}
	
	protected void buildShapeTransforms(double[][] inlinedShapeTransformMatrices){
		ArrayList<Integer> list = new ArrayList<Integer>();
		for(int i=0; i<transformedShapeIndices_.size(); i++)
			list.add(i);
		while(!list.isEmpty()){
        	int inlinedSTMIndex = transformedShapeIndices_.get(list.get(0)).inlinedShapeTransformMatrixIndex;
        	ArrayList<Integer> indices = new ArrayList<Integer>();
        	indices.add(list.get(0));
        	if(inlinedSTMIndex == -1){ 
        		setShapeIndex(transformedShapeIndices_.get(list.get(0)));		
        	}else{  
        		for(int k=1; k<list.size(); k++){
            		Integer j = list.get(k);
            		if(inlinedSTMIndex == transformedShapeIndices_.get(j).inlinedShapeTransformMatrixIndex){
            			indices.add(j);       			
            		}
            	}
        		setInlineShapeIndex(inlinedShapeTransformMatrices[inlinedSTMIndex], indices);
        	}
        	for(int k=0; k<indices.size(); k++)
        		list.remove(indices.get(k));
		}
	}

	private void setShapeIndex(TransformedShapeIndex transformedShapeIndex){
		Matrix4d mat = new Matrix4d(transformedShapeIndex.transformMatrix[0], transformedShapeIndex.transformMatrix[1], transformedShapeIndex.transformMatrix[2], transformedShapeIndex.transformMatrix[3],
				transformedShapeIndex.transformMatrix[4], transformedShapeIndex.transformMatrix[5], transformedShapeIndex.transformMatrix[6], transformedShapeIndex.transformMatrix[7],
				transformedShapeIndex.transformMatrix[8], transformedShapeIndex.transformMatrix[9], transformedShapeIndex.transformMatrix[10], transformedShapeIndex.transformMatrix[11],
				0,0,0,1	);
		ShapeTransform _shapeTransform = new ShapeTransform(false, mat, null, transformedShapeIndex.shapeIndex);
		shapeTransforms_.add(_shapeTransform);
	}
	
	private void setInlineShapeIndex(double[] inlinedMatrix, ArrayList<Integer> indices){
		Matrix4d inlinedMat = new Matrix4d(inlinedMatrix[0], inlinedMatrix[1], inlinedMatrix[2], inlinedMatrix[3],
				inlinedMatrix[4], inlinedMatrix[5], inlinedMatrix[6], inlinedMatrix[7],
				inlinedMatrix[8], inlinedMatrix[9], inlinedMatrix[10], inlinedMatrix[11],
				0,0,0,1	);
		Matrix4d[] mat = new Matrix4d[indices.size()];
		int[] indices0 = new int[indices.size()];
		for(int i=0; i<indices.size(); i++){
			TransformedShapeIndex transformedShapeIndex = transformedShapeIndices_.get(indices.get(i));
			mat[i] = new Matrix4d(transformedShapeIndex.transformMatrix[0], transformedShapeIndex.transformMatrix[1], transformedShapeIndex.transformMatrix[2], transformedShapeIndex.transformMatrix[3],
					transformedShapeIndex.transformMatrix[4], transformedShapeIndex.transformMatrix[5], transformedShapeIndex.transformMatrix[6], transformedShapeIndex.transformMatrix[7],
					transformedShapeIndex.transformMatrix[8], transformedShapeIndex.transformMatrix[9], transformedShapeIndex.transformMatrix[10], transformedShapeIndex.transformMatrix[11],
					0,0,0,1	);
			indices0[i] = transformedShapeIndex.shapeIndex;
		}
		ShapeTransform _shapeTransform = new ShapeTransform(true, mat, inlinedMat, indices0);
		shapeTransforms_.add(_shapeTransform);
		
	}
	
	public void addShape(Matrix4d segmentT){
		for(int i=0; i<shapeTransforms_.size(); i++){
			GrxShapeItem shapeItem = new GrxShapeItem(getName()+"_shape_"+i, manager_, model_ ); 
			if(!shapeTransforms_.get(i).isInline_)
				shapeItem.loadShape(shapeTransforms_.get(i).transform_[0], shapeTransforms_.get(i).shapeIndices_[0], segmentT);
			else
				shapeItem.loadInlineShape(shapeTransforms_.get(i).transform_, shapeTransforms_.get(i).inlinedTransform_, shapeTransforms_.get(i).shapeIndices_, segmentT);
			addChild(shapeItem);
			manager_.itemChange(shapeItem, GrxPluginManager.ADD_ITEM);
		}
	}
	
	/**
     * @brief create and add a new shape as a child
     * @param url URL of the file where shape is described
     */
    public void addShape(String fPath){
		if( fPath != null ) {
            try {
                ModelLoader mloader = ModelLoaderHelper.narrow(
                        GrxCorbaUtil.getReference("ModelLoader"));
                    
                SceneInfo sInfo = mloader.loadSceneInfo(fPath);
                int n=children_.size();
                GrxShapeItem shapeItem = new GrxShapeItem(getName()+"_shape_"+n, manager_, model_);
                shapeItem.loadnewInlineShape(sInfo);
                shapeItem.setURL(fPath);
                addChild(shapeItem);
                
            	//manager_.reselectItems();
                manager_.itemChange(shapeItem, GrxPluginManager.ADD_ITEM);
            } catch(ModelLoaderException me){
                MessageDialog.openError(GrxUIPerspectiveFactory.getCurrentShell(),
                                        MessageBundle.get("GrxModelItem.dialog.title.error"), //$NON-NLS-1$
                                        MessageBundle.get("GrxModelItem.dialog.message.loadSceneError") +"\n" + //$NON-NLS-1$ //$NON-NLS-2$
                                        fPath + "\n\n" + me.description); //$NON-NLS-1$
                System.out.println("Failed to load scene info:" + fPath);
                me.printStackTrace();
            } catch(Exception ex){
                System.out.println("Failed to load scene info:" + fPath);
                ex.printStackTrace();
            }
		}

    }
       
    public void addPrimitiveShape(String name){
	    	int n=children_.size();
	    	int type;
	    	if(name.equals("Box"))
	    		type = ShapePrimitiveType._SP_BOX;
	    	else if(name.equals("Cone"))
	    		type = ShapePrimitiveType._SP_CONE;
	    	else if(name.equals("Cylinder"))
	    		type = ShapePrimitiveType._SP_CYLINDER;
	    	else if(name.equals("Sphere"))
	    		type = ShapePrimitiveType._SP_SPHERE;
	    	else
	    		type = -1;
	        GrxShapeItem shapeItem = new GrxShapeItem(getName()+"_"+name+"_"+n, manager_, model_);
	        shapeItem.createnewPrimitiveShape(type);
	    	addChild(shapeItem);
	    	//manager_.reselectItems();
	        manager_.itemChange(shapeItem, GrxPluginManager.ADD_ITEM);
	}
	 
	public void transform(double[] w){
		Matrix3d m = new Matrix3d(w[0],w[1],w[2],w[4],w[5],w[6],w[8],w[9],w[10]);
    	double[] p = {w[3], w[7], w[11]};
		translation(p);
		AxisAngle4d a4d = new AxisAngle4d();
        a4d.setMatrix(m);
        double [] rot = new double[4];
        a4d.get(rot);
        rotation(rot);
    }
    
    public Matrix4d getTransform(){
    	Transform3D t3d = new Transform3D();
    	tg_.getTransform(t3d);
    	Matrix4d ret = new Matrix4d();
    	t3d.get(ret);
    	return ret;
    }
    
    public double[] translation(){
    	Transform3D t3d = new Transform3D();
    	tg_.getTransform(t3d);
    	Vector3d v = new Vector3d();
    	t3d.get(v);
    	double[] ret = new double[3];
    	v.get(ret);
    	return ret;
    	
    }
    
    public double[] rotation(){
    	Transform3D t3d = new Transform3D();
    	tg_.getTransform(t3d);
    	Matrix3d mat = new Matrix3d();
    	t3d.get(mat);
    	AxisAngle4d aa = new AxisAngle4d();
    	aa.setMatrix(mat);
    	double[] ret = new double[4];
    	aa.get(ret);
    	return ret;
    }
    
    /**
     * @brief set new translation from string
     * @param value string of space separated array of double(length=3)
     * @return true if set successfully, false otherwise
     */
    public boolean translation(String value){
    	double [] pos = getDblAry(value);
    	if (translation(pos)){
            return true;
    	}else{
    		return false;
    	}
    }
    
    /**
     * @brief set new translation to TransformGroup
     * @param pos new translation(length=3)
     * @return true if new translation is set successfully, false otherwise
     */
    public boolean translation(double[] pos){
    	if (pos == null || pos.length != 3) return false;
    	Vector3d v = new Vector3d(pos);
        Transform3D t3d = new Transform3D();
        tg_.getTransform(t3d);
        t3d.setTranslation(v);
        setDblAry("translation", pos, 4);
        if (model_ != null && parent_ != null) model_.notifyModified();
        try{
        	tg_.setTransform(t3d);
        }catch(BadTransformException e){
        	System.out.println("Invalid translation:"+v+" is applied to "+getName());
        	return false;
        }
        return true;
    }

    /**
     * @brief set new rotation from string
     * @param value string of space separated array of double(length=4)
     * @return true if set successfully, false otherwise
     */
    public boolean rotation(String value){
    	double [] rot = getDblAry(value);
    	if (rotation(rot)){
            return true;
    	}else{
    		return false;
    	}
    }
    
    /**
     * @breif set new rotation to TransformGroup
     * @param rot new rotation(axis and angle, length=4)
	 * @return true if new rotation is set successfully, false otherwise
     */
    public boolean rotation(double[] rot){
    	if (rot == null || rot.length != 4) return false;
        Transform3D t3d = new Transform3D();
        tg_.getTransform(t3d);
        t3d.setRotation(new AxisAngle4d(rot));
        tg_.setTransform(t3d);
        setDblAry("rotation", rot, 4);
        if (model_ != null && parent_ != null) model_.notifyModified();
        return true;
    }

    protected void initMenu(){
    	getMenu().clear();
		Action item;

		// rename
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxSensorItem.menu.rename"); //$NON-NLS-1$
			}
			public void run(){
				InputDialog dialog = new InputDialog( null, getText(),
						MessageBundle.get("GrxSensorItem.dialog.message.newName"), getName(),null); //$NON-NLS-1$
				if ( dialog.open() == InputDialog.OK && dialog.getValue() != null)
					rename( dialog.getValue() );
			}
		};
		setMenuItem(item);

		// delete
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxSensorItem.menu.delete"); //$NON-NLS-1$
			}
			public void run(){
				if( MessageDialog.openQuestion( null, MessageBundle.get("GrxSensorItem.dialog.title.delete"), //$NON-NLS-1$
						MessageBundle.get("GrxSensorItem.dialog.message.delete") + getName() + " ?") ) //$NON-NLS-1$ //$NON-NLS-2$
					delete();
			}
		};
		setMenuItem(item);
		
		// menu item : add shape
		item = new Action(){
			public String getText(){
				return MessageBundle.get("GrxLinkItem.menu.VRML97"); //$NON-NLS-1$
			}
			public void run(){
				FileDialog fdlg = new FileDialog( GrxUIPerspectiveFactory.getCurrentShell(), SWT.OPEN);
				fdlg.setFilterExtensions(new String[]{"*.wrl"});
				fdlg.setFilterPath(getDefaultDir().getAbsolutePath());
				String fPath = fdlg.open();
				System.out.println("fPath = "+fPath); //$NON-NLS-1$
				if( fPath != null ) {
					addShape( fPath );
					setDefaultDirectory(new File(fPath).getParent());
				}
			}
		};
        setMenuItem(item);
        
        // menu item : add primitive shape
        MenuManager subMenu= new MenuManager(MessageBundle.get("GrxLinkItem.menu.primitiveShape")); //$NON-NLS-1$
        setSubMenu(subMenu);      
        item = new Action(){
			public String getText(){
				return "Box"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Box"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		item = new Action(){
			public String getText(){
				return "Cone"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Cone"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		item = new Action(){
			public String getText(){
				return "Cylinder"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Cylinder"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		item = new Action(){
			public String getText(){
				return "Sphere"; //$NON-NLS-1$
			}
			public void run(){
				addPrimitiveShape("Sphere"); //$NON-NLS-1$
			}
		};
		subMenu.add(item);
		setSubMenu(subMenu);
    }
    
}

class ShapeTransform {
	public boolean isInline_ = false;
	public Matrix4d[] transform_ = null;
	public Matrix4d inlinedTransform_ = null;
	public int[] shapeIndices_ = null;
	
	public ShapeTransform(boolean isInline, Matrix4d mat, Matrix4d inlinedMat, int index){
		isInline_ = isInline;
		transform_ = new Matrix4d[1];
		transform_[0] = mat; 
		inlinedTransform_ = inlinedMat;
		shapeIndices_ = new int[1];
		shapeIndices_[0] = index;
	}
	
	public ShapeTransform(boolean isInline, Matrix4d[] mat, Matrix4d inlinedMat, int[] indices){
		isInline_ = isInline;
		transform_ = mat;
		inlinedTransform_ = inlinedMat;
		shapeIndices_ = indices;
	}
}
