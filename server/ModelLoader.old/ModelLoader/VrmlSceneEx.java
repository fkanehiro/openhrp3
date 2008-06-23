package jp.go.aist.hrp.simulator;

import javax.media.j3d.TransformGroup;
import javax.media.j3d.BranchGroup;
import java.util.Hashtable;
import java.util.Enumeration;
import com.sun.j3d.loaders.vrml97.node.*;
import com.sun.j3d.loaders.vrml97.impl.Route;
//import com.sun.j3d.loaders.vrml97.impl.*;
import vrml.node.*;
import vrml.*;
import java.util.StringTokenizer;

// メモ
// このクラスは VrmlScene をそのままコピーしたもに
// 自作の getField と getNodeTypeString を追加したものである
// よって一部のコメントはもとの VrmlScene のものであり
// 英語のままである

public class VrmlSceneEx implements com.sun.j3d.loaders.Scene {
    com.sun.j3d.loaders.vrml97.impl.Scene base;
    String		description;
    int			numTris;
    BaseNode[]		objects;
    Viewpoint[]		viewpoints;
    Node[]		navInfos;
    Background[]	backgrounds;
    Fog[]		fogs;
    Light[]		lights;
    Hashtable<Object, Object>		defTable;
    BranchGroup		scene = null;
    // proto の内容を保存しておく
    

    // この下の二つが自分が追加したメソッドである。
    // このメソッドは def の名前と field の名前を指定して Field を返されるというそのままのメソッドである
    // 返せない場合はとりあえず null を返す。
    public com.sun.j3d.loaders.vrml97.impl.Field getField(String defName,String fieldName)
    {
        // まず getNamedObjects で def 名の一覧を取得し保存しておく
        
        try{
        
            com.sun.j3d.loaders.vrml97.impl.BaseNode node = base.use(defName);
            return node.getField(fieldName);
        }catch(Exception e)
        {
            return null;
        }
    }

    // def の名前を指定してその名前をストリングで返すというそのままのメソッドである
    public String getNodeTypeString(String defName)
    {
        try{
        
            com.sun.j3d.loaders.vrml97.impl.BaseNode node = base.use(defName);
	    return node.wrap().getType();
        }catch(Exception e)
        {
            return null;
        }
    }
    
    VrmlSceneEx(com.sun.j3d.loaders.vrml97.impl.Scene base) {
	this.base = base;
	description = base.description;
	numTris = base.numTris;
	
	objects = new BaseNode[base.objects.size()];
	Enumeration elems = base.objects.elements();
	for (int i = 0; i < objects.length; i++) {
	    com.sun.j3d.loaders.vrml97.impl.BaseNode node = 
		(com.sun.j3d.loaders.vrml97.impl.BaseNode)elems.nextElement();
	    objects[i] = node.wrap();
	}

	viewpoints = new Viewpoint[base.viewpoints.size()];
	elems = base.viewpoints.elements();
	for (int i = 0; i < viewpoints.length; i++) {
	    viewpoints[i] = (com.sun.j3d.loaders.vrml97.node.Viewpoint)
		((com.sun.j3d.loaders.vrml97.impl.BaseNode)
					elems.nextElement()).wrap();
	}

	navInfos = new Node[base.navInfos.size()];
	elems = base.navInfos.elements();
	for (int i = 0; i < navInfos.length; i++) {
	    navInfos[i] = (vrml.node.Node)
		((com.sun.j3d.loaders.vrml97.impl.BaseNode)
			elems.nextElement()).wrap();
	}

	backgrounds = new Background[base.backgrounds.size()];
	elems = base.backgrounds.elements();
	for (int i = 0; i < backgrounds.length; i++) {
	    backgrounds[i] = (com.sun.j3d.loaders.vrml97.node.Background)
		((com.sun.j3d.loaders.vrml97.impl.BaseNode)
					elems.nextElement()).wrap();
	}

	fogs = new Fog[base.fogs.size()];
	elems = base.fogs.elements();
	for (int i = 0; i < fogs.length; i++) {
	    fogs[i] = (com.sun.j3d.loaders.vrml97.node.Fog)
		((com.sun.j3d.loaders.vrml97.impl.BaseNode)
						elems.nextElement()).wrap();
	}

	lights = new Light[base.lights.size()];
	elems = base.lights.elements();
	for (int i = 0; i < lights.length; i++) {
	    lights[i] = (com.sun.j3d.loaders.vrml97.node.Light)
		((com.sun.j3d.loaders.vrml97.impl.BaseNode)
					elems.nextElement()).wrap();
	}

	defTable = new Hashtable<Object, Object>();
	for (elems = base.defTable.keys(); elems.hasMoreElements();){
	    Object key = elems.nextElement();
	    Object value = ((com.sun.j3d.loaders.vrml97.impl.BaseNode)
						base.defTable.get(key)).wrap();  
	    defTable.put(key, value);
	}
    }

    /**
     * Returns the root nodes of the VRML scene, gathered into a BranchGroup
     */
    public BranchGroup getSceneGroup() {
	if (scene == null) {
	    scene = new BranchGroup();
	    for (int i = 0; i < objects.length; i++) {
	        javax.media.j3d.Node j3dNode;
	        if ((j3dNode = objects[i].getImplNode()) != null) {;
		    scene.addChild(j3dNode);
	        }
	    }
	}
	return scene;
    }

    /**
     * Returns the TransformGroups associated with the VRML Viewpoints.
     * The TransformGroups returned will be parented withing the SceneGroup.
     * The ViewPlatform will be the child of the TransformGroup 
     */
    public TransformGroup[] getViewGroups() {
	TransformGroup[] views = new TransformGroup[viewpoints.length];
        for (int i = 0; i < viewpoints.length; i++) {
            views[i] = (TransformGroup)viewpoints[i].getImplNode();
        }
        return views;
    }

    /**
     * Returns the horizontal field of view values for the Viewpoints in 
     * the scene.
     */
    public float[] getHorizontalFOVs() {
	float[] fovs = new float[viewpoints.length];
        for (int i = 0; i < viewpoints.length; i++) {
            fovs[i] = viewpoints[i].getFOV();
        }
        return fovs;
    }

    /**
     * Returns Java3D Light nodes in the scene.
     * The Light nodes returned will be parented within the SceneGroup
     */
    public javax.media.j3d.Light[] getLightNodes() {
        javax.media.j3d.Light[] j3dLights = 
				new javax.media.j3d.Light[lights.length * 2];
	for (int i = 0; i < lights.length; i++) {
	    j3dLights[i*2]     = lights[i].getAmbientLight();
	    j3dLights[i*2 + 1] = lights[i].getLight();
	}
	return j3dLights;
    }

    /** 
     * Gets a Hashtable containing the VRML DEF table, with the key being the
     * DEF name and the value being the Java3D SceneGraphObject associated 
     * with the VRML Node.
     */ 
    public Hashtable<Object, javax.media.j3d.SceneGraphObject> getNamedObjects() {
        Hashtable<Object, javax.media.j3d.SceneGraphObject> j3dDefTable 
            = new Hashtable<Object, javax.media.j3d.SceneGraphObject>();
        for (Enumeration elems = defTable.keys();
                elems.hasMoreElements();){
            Object key = elems.nextElement();
            BaseNode node = (BaseNode)defTable.get(key);
            javax.media.j3d.SceneGraphObject value = node.getImplObject();
            if (value != null) {
                j3dDefTable.put(key, value);
            }
        }
        return j3dDefTable;
    }

    /**
     * Returns the J3D Background nodes in the scene.
     * The Background nodes returned will be parented within the SceneGroup
     */
    public javax.media.j3d.Background[] getBackgroundNodes() {
	javax.media.j3d.Background[] j3dBackgrounds = 
		new javax.media.j3d.Background[backgrounds.length];
	for (int i = 0; i < backgrounds.length; i++) {
	    j3dBackgrounds[i] = backgrounds[i].getBackgroundImpl();
	}
	return j3dBackgrounds;
    }

    /**
     * Returns the J3D Fog nodes in the scene.
     * The Fog nodes returned will be parented within the SceneGroup
     */
    public javax.media.j3d.Fog[] getFogNodes() {
	javax.media.j3d.Fog[] j3dFogs = new javax.media.j3d.Fog[fogs.length];
	for (int i = 0; i < fogs.length; i++) {
	    j3dFogs[i] = fogs[i].getFogImpl();
	}
	return j3dFogs;
    }

    /**
     * The VRML loader does not support loading behaviors, this method
     * returns null.
     */
    public javax.media.j3d.Behavior[] getBehaviorNodes() {
	return null;
    }


    /**
     * The VRML loader does not support loading sounds, this method
     * returns null.
     */
    public javax.media.j3d.Sound[] getSoundNodes() {
	return null;
    }
 
    /**
     * Returns the description (if any) from the first WorldInfo node
     * read.  If there is no description specified, null will be returned
     */
    public String getDescription() {
	 return description;
    }


    // the VRML specific methods start here

    /**
     * Scans the subgraph, clearing the pickable and collidable flags on
     * the Shape3Ds in the subgraph to allow compilation.  The pickable 
     * flag will be set to false if the Shape3D does not have an ancestor
     * which sets the ALLOW_PICK_REPORTING bit.  The collidable flag will
     * always be set to false.
     */
    public void cleanForCompile(javax.media.j3d.Node root) {
    	com.sun.j3d.loaders.vrml97.impl.TreeCleaner.cleanSubgraph(root);
    }

    /**
     * Returns the base level VRML nodes
     */
    public BaseNode[] getObjects() {
	BaseNode nodes[] = new BaseNode[objects.length];
	for (int i = 0; i < objects.length; i++) {
	    nodes[i] = objects[i];
	}
	return nodes;
    }

    /**
     * Returns the Viewpoint nodes in the scene
     */
    public Viewpoint[] getViewpoints() {
	Viewpoint[] vps = new Viewpoint[viewpoints.length];
	for (int i = 0; i < viewpoints.length; i++) {
	    vps[i] = viewpoints[i];
	}
	return vps;
    }

    /**
     * Returns the a Hashtable which associated DEF names with Nodes
     */
    public Hashtable getDefineTable() {
        Hashtable<Object, Object> userDefTable = new Hashtable<Object, Object>();
        for (Enumeration elems = defTable.keys();
                elems.hasMoreElements();){
            Object key = elems.nextElement();
            Object value = defTable.get(key);
            userDefTable.put(key, value);
        }
        return userDefTable;
    }

    /**
     * Returns the approximate number of triangles in the Scene.  For Switch
     * and LOD nodes, only the triangles on the first child of the node are
     * counted.
     */
    public int getNumTris() {
	return numTris;
    }

    public void writeX3D(java.io.Writer w,String systemId) throws java.io.IOException {
	// will need to surround with <Scene> and </Scene>	
	w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");	
	w.write("<!DOCTYPE X3D SYSTEM \""+systemId+"\">\n");
	w.write("<X3D>\n <Scene>\n");
	// コメントアウトしているのはここを On にすると良く分からないエラーを出力するため
//	for(int i = 0; i< objects.length; i++)
//	    objects[i].writeX3D(w,this);

	// routes
	Enumeration e = base.routes.elements();
	while(e.hasMoreElements()) {
	    Route r = (Route)(e.nextElement());
	    r.writeX3D(w);
	    w.write("\n");
	}

	    
	w.write(" </Scene>\n</X3D>\n");
    }
	
}
