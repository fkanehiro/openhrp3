package jp.go.aist.hrp.simulator;

import java.util.*;

import javax.vecmath.*;
import javax.media.j3d.Node;
import javax.media.j3d.Group;
import javax.media.j3d.SharedGroup;
import javax.media.j3d.*;
import com.sun.j3d.utils.geometry.*;


//オブジェクトの情報を保持するクラス
/**
 * ObjectStruct class
 * @author K Saito (Kernel Co.,Ltd.)
 * @version 1.0 (2002/01/25)
 */
public class ObjectStruct{
    public static final int MODE_ROBOT=0;
    public static final int MODE_ENVIROMENT=1;

    public static final String ENVIROMENT_BODY_NAME="rootjoint";
    public static final String ENVIROMENT_SEGMENT_NAME="rootsegment";

    private static final String NODE_TYPE_JOINT   = "Joint";
    private static final String NODE_TYPE_SEGMENT = "Segment";
    private static final String NODE_TYPE_HUMANOID = "Humanoid";

    private static final int MODEL_NODE_GROUP = 0;
    private static final int MODEL_NODE_BRANCHGROUP = 1;
    private static final int MODEL_NODE_TRANSFORMGROUP = 2;
    private static final int MODEL_NODE_PRIMITIVE = 3;
    
    //センサ定義
    private static final String[] SENSOR_NAME = {
        "ForceSensor",
        "Gyro",
        "AccelerationSensor",
        "PressureSensor",
        "PhotoInterrupter",
	"VisionSensor",
	"TorqueSensor",
    };

    private static final SensorInfoFactory[] SENSOR_FACTORY = {
        new ForceSensorInfoFactory(),
        new GyroSensorInfoFactory(),
        new AccelerationSensorInfoFactory(),
        new PressureSensorInfoFactory(),
        new PhotoInterrupterSensorInfoFactory(),
	new VisionSensorInfoFactory(),
        new TorqueSensorInfoFactory(),
    };
    
    private Hashtable<String, BodyStruct> htBody_ = null;//BodyName-BodyStruct
    private BodyStruct rootBody_;  //root
    private int mode_;
    private int index_;
    private VrmlSceneEx scene_;// reference to the scene
    private VrmlLoaderEx loader_;

    private HumanoidInfo humanoidInfo_;//PROTO Humanoid

    //コンストラクタ
    public ObjectStruct(String url){

        System.out.println("Loader: loadURL()");
        System.out.println("\tURL = " + url);

        // load
        System.out.print("loading ...");
        loader_ = new VrmlLoaderEx();
        try {
            java.net.URL u = new java.net.URL(url);
            scene_ = (VrmlSceneEx)loader_.load(u);
        } catch (java.net.MalformedURLException ex) {
            System.out.println("* MalformedURLException");
        } catch (java.io.IOException ex) {
            System.out.println("* IOException");
        }
        System.out.println(" finished.");
        
        System.out.print("Loader: _parse() ");
        
        Hashtable ht   = scene_.getNamedObjects();      // VrmlSceneExからDEF名を取得する。
        Hashtable table = new Hashtable();             // DEFに対応するＯＢＪを保持
        java.lang.Object key = null;
        for (Enumeration keys = ht.keys(); keys.hasMoreElements();) {
            key = keys.nextElement();                  // 
            try {
                Node obj = (Node) ht.get(key);
                if (obj instanceof Link) {             // Linkの時は、SharedGroupを保持する
                    Link lk = (Link)ht.get(key);
                    lk.setCapability(Link.ALLOW_SHARED_GROUP_READ);
                    lk.setCapability(Link.ALLOW_SHARED_GROUP_WRITE);
                    SharedGroup sg = lk.getSharedGroup();
                    sg.setCapability(SharedGroup.ALLOW_CHILDREN_READ);
                    sg.setCapability(SharedGroup.ALLOW_CHILDREN_WRITE);
                    for (int j = 0; j < sg.numChildren(); j++) {
                        Node sgNode = (Node) sg.getChild(j);
                        if (sgNode instanceof TransformGroup) {
                            TransformGroup tg = (TransformGroup) sgNode;
                            tg.setCapability(TransformGroup.ALLOW_CHILDREN_READ);         // Children Read/Write
                            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);        // Transform Read/Write
                            tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                            tg.setCapability(TransformGroup.ALLOW_LOCAL_TO_VWORLD_READ);  // LocalToVworld Read
                            tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);       // LocalToVworld Read
                            break;
                        }
                    }
                    table.put(key, sg);
                } else if (obj instanceof TransformGroup) {
                    table.put(key, obj);               // 
                } else {
                    table.put(key, obj);               // Link,TransformGroup以外の時
                }
            }  catch(Exception e)  {
                ;
            }
        }
        
        htBody_=new Hashtable<String, BodyStruct>();
        humanoidInfo_ = new HumanoidInfo();
        mode_=MODE_ROBOT;
        if (mode_==MODE_ROBOT){
                        //index_はhtBody要素数-1。
            index_=-1;

            _analyze(table,scene_.getSceneGroup(),null,null,new Transform3D(),0);// シーングラフを辿る。
            
            //rootBodyを探す(かっこわるいけど妥協)
            key = null;                    // 検索キー取得
            for (Enumeration keys = htBody_.keys(); keys.hasMoreElements();) {
                key = keys.nextElement();
                BodyStruct body=htBody_.get(key);
                if (body.getId()==0) {               //root?
                    rootBody_=body;
                    break;
                }
            }
            
        }else if (mode_==MODE_ENVIROMENT){
            index_=0;
            rootBody_=new BodyStruct(scene_.getSceneGroup(),null,index_);
            rootBody_.setParamForEnviroment(ENVIROMENT_BODY_NAME,ENVIROMENT_SEGMENT_NAME);        //内部情報セット
            htBody_.put(ENVIROMENT_BODY_NAME,rootBody_);                //テーブルに追加
            _analyze(table,scene_.getSceneGroup(),rootBody_,null,new Transform3D(),0);// シーングラフを辿る。
        }
    }
    
    public BodyStruct rootBody(){
        return rootBody_;
    }

    public BodyStruct getBody(String name){
        return htBody_.get(name);
    }

    public String[] getBodyList(){
         String[] s=new String[htBody_.size()];
         int c=0;
         for (Enumeration e = htBody_.keys() ; e.hasMoreElements() ;) {
            s[c]=(String)e.nextElement();
            c++;
        }
        return s;

    }

    public BodyStruct[] getBodys(){
         BodyStruct[] b=new BodyStruct[htBody_.size()];
         int c=0;
         for (Enumeration e = htBody_.keys() ; e.hasMoreElements() ;) {
            String name =(String)e.nextElement();
            b[c]=htBody_.get(name);
            c++;
        }
        return b;
    }

    public String[] getSegmentList(){
        Vector<String> vec= new Vector<String>();
        for (Enumeration e = htBody_.keys() ; e.hasMoreElements() ;) {
            String name =(String)e.nextElement();
            BodyStruct body=htBody_.get(name);
            String sName=body.getSegmentName();
            if (sName!=null){
                vec.add(sName);
            }
        }
        
         String[] s=new String[vec.size()];
         for (int i=0;i<vec.size();i++){
            s[i]=(String)vec.get(i);
         }
         return s;

    }

    public LinkInfo_impl[] getLinkInfos(){
        BodyStruct[] bs=getBodys();
        LinkInfo_impl[] mos=new LinkInfo_impl[bs.length];
        for(int i=0;i<bs.length;i++){
            mos[bs[i].getId()]=bs[i].getLinkInfo();
        }
        return mos;
    }
    
    public HumanoidInfo getHumanoidInfo(){
        return humanoidInfo_;
    }
    /**
     * 再帰的にNodeを解析してBodyStructを構築htBody_に格納
     *
     * @param  table         defName-Nodeのテーブル
     * @param  nd            現在のNode
     * @param  motherBody    親Body
     * @param  sisterBody    姉Body
     * @param  tfBody,       ボディ起点の位置姿勢
     * @param  depth
     * @return  ndがJointだった場合そのBody,そうでない場合null;
     */
    private BodyStruct _analyze(Hashtable table, Node nd,BodyStruct motherBody, BodyStruct sisterBody,
                                Transform3D tfBody,int depth)
    {

        Node node = nd;
        Node nameNode = nd;
        Group group = null;
        BodyStruct myBody=null;
        
        if (node instanceof Link) {                         // Linkノードの時はSharedGroupが
            Link lk = (Link) node;                          // DEFに対応して保持されている
            SharedGroup sg = lk.getSharedGroup();
            nameNode = (Node) sg;
            //Joint用
            if(sg.numChildren() == 1){
                node = (Node) sg.getChild(0);
            }else{
                node = (Node) sg;
            }
        }

        //System.out.println("trf = " + tfBody);

                                                             // どんなノードか調べる
        if (node instanceof Group) {
            int nodetype = MODEL_NODE_GROUP;
            try {
                BranchGroup bg = (BranchGroup)node;
                nodetype = MODEL_NODE_BRANCHGROUP;
            } catch (ClassCastException exbg) {
                try {
                    TransformGroup tg = (TransformGroup)node;
                    nodetype = MODEL_NODE_TRANSFORMGROUP;
                } catch (ClassCastException extg) {
                    try {
                        Primitive pr = (Primitive)node;
                        nodetype = MODEL_NODE_GROUP;                  // PrimitiveはGroup_Node
                        //nodetype = MODEL_NODE_PRIMITIVE;
                    } catch (ClassCastException expr) {
                    }
                }
            }
    
            //---------------------------------------------------------------------------
            // このnameNodeの情報取得(MODE_ROBOTのときEnviromentのときはJointだろうがなんだろうが無視)
            //
            if (table.containsValue(nameNode) && mode_==MODE_ROBOT) {                    // ノードは、DEFされている
                
                                                                //nodeからdef名を引く
                java.lang.Object key = null;                    // 検索キー取得
                for (Enumeration keys = table.keys(); keys.hasMoreElements();) {
                    key = keys.nextElement();
                    if (table.get(key) == nameNode) {               // ノードを取得する
                        break;
                    }
                }
                                                           // NodeTypeを取得
                String defName = key.toString();
                String nodeType = scene_.getNodeTypeString( defName );
        
                //System.out.println("def:" + nd);
                if (nodeType.equals( NODE_TYPE_JOINT )) {       // NodeType == Joint
                    for (int i=0; i<depth; i++) System.out.print(" ");
                    System.out.println("Joint("+defName+")");
                
                    index_++;                                   //グロバールなインデックスカウンタをインクリメント
                                                                       //このジョイントの情報を格しいボディ構造を生成
                                                                       //このとき,MO配列を作るときに必要になるindexを設定
                    myBody=new BodyStruct((Group)node,motherBody,index_); //さらに、母を設定「ママ！」
                    myBody.setJointParam(defName,scene_);        //内部情報セット
                    htBody_.put(defName,myBody);                //テーブルに追加

                    if(sisterBody!=null){                        //姉がいるなら「私は妹ね」
                                                           //姉のsisterに私をセット
                                                           //「妹は私よ！」
                        sisterBody.setSister(myBody);
                
                    }else{                                      //姉がいないなら「私は長女ね」
            
                        if(motherBody!=null){                            //親がいるなら
                                                                //親の娘情報を更新
                            motherBody.setDaughter(myBody);              //「長女はわたしよ！」
                        }
                    }

                } else if (nodeType.equals( NODE_TYPE_SEGMENT )) {   // NodeType == Segment
                    for (int i=0; i<depth; i++) System.out.print(" ");
                    System.out.println("Segment("+defName+")");
                    //motherBodyにデータ取得
                    if(motherBody!=null){
                        motherBody.setSegmentParam(defName,scene_);
                    }
                } else if (nodeType.equals( NODE_TYPE_HUMANOID )) {   // NodeType == Segment
                    for (int i=0; i<depth; i++) System.out.print(" ");
                    System.out.println("Humanoid("+defName+")");
                    //humanoidInfo_にデータ取得
                    humanoidInfo_.setParam(defName,scene_);
                                                        
                } else {                                        // NodeType != Joint Segment

                    //センサー    kokok
                    for(int i = 0 ; i < SENSOR_NAME.length ; i++){
                        if (nodeType.equals(SENSOR_NAME[i])){
                            for (int j=0; j<depth; j++) System.out.print(" ");
                            System.out.println(SENSOR_NAME[i]+"("+defName+")");
                            motherBody.addSensor(
                                SENSOR_FACTORY[i].createSensorInfo( 
                                    defName,
                                    scene_
                                )
                            );
                        }
                    }
                 }
            } else {                                            // DEFされていない？
                //
                //System.out.println("no def:" + nd);
            } 
    

            //---------------------------------------------------------------------------
            // 子ノードへ
            //

            if(myBody!=null){                                //Jointならば
                                                             //JointならばGroupなはず
                group = (Group)node;
            
                //子へ
                BodyStruct lastDaughterBody=null;                         // 姉インデックス、長女はnull
                BodyStruct tempBody;                                      //戻り値格納用
                for (int i = 0; i < group.numChildren(); i++) {
                    //子ノード解析
                    tempBody=_analyze(table, group.getChild(i), myBody, lastDaughterBody,
                                      (new Transform3D()),depth+1);
                    
                    if(tempBody!=null){                      //子NodeがJointだったら
                        lastDaughterBody=tempBody;              //その子のインデックスを姉として保持
                    }
                }
            }else{
                //Joint以外なら
                // このノードは無視して子ノードへ
                //注意：
                //  Segmentは子にJointもSegmentも持たないはず
                //  でも、一応子ノードへ行く
                //  (傘下のノードがSegmentならエラーになる)
                //  (傘下のノードがJointならこのSegmentをもつJointを親とする)
                
                if (nodetype == MODEL_NODE_GROUP||nodetype == MODEL_NODE_BRANCHGROUP) {         //Group or BranchGroupノードである
                    group = (Group)node;

                    //子へ
                    BodyStruct lastDaughterBody=null;                         // 姉インデックス、長女はnull
                    BodyStruct tempBody;                                      //戻り値格納用
                    for (int i = 0; i < group.numChildren(); i++) {
                        //子ノード解析
                        tempBody=_analyze(table, group.getChild(i), motherBody, lastDaughterBody,
                                          tfBody,depth+1);
                    
                        if(tempBody!=null){                      //子NodeがJointだったら
                            lastDaughterBody=tempBody;              //その子のインデックスを姉として保持
                        }
                    }
                }else if (nodetype == MODEL_NODE_TRANSFORMGROUP) {         //ノードはTransformGroup
                    TransformGroup tg = (TransformGroup)node;

                    // ローカル座標系での位置と姿勢を取得
                    Transform3D t3dLocal = new Transform3D();
                    tg.getTransform(t3dLocal);
                    // 所属するボディ起点での位置と姿勢を取得
                    Transform3D newtfBody = new Transform3D(tfBody);
                    newtfBody.mul(t3dLocal);

                    //子へ
                    BodyStruct lastDaughterBody=null;                         // 姉インデックス、長女はnull
                    BodyStruct tempBody;                                      //戻り値格納用
                    for (int i = 0; i < tg.numChildren(); i++) {
                        //子ノード解析
                        tempBody=_analyze(table, tg.getChild(i), motherBody, lastDaughterBody,
                                          newtfBody,depth+1);
                    
                        if(tempBody!=null){                      //子NodeがJointだったら
                            lastDaughterBody=tempBody;              //その子のインデックスを姉として保持
                        }
                    }

                }else{
                    //
                }
            } // if(joint){}else{}
        }else if(node instanceof Shape3D) { // Groupノードを世襲しないなら、ノードはShape3D？
            Shape3D s3d = (Shape3D)node;
            //三角形集合を登録
            motherBody.addShape3D(s3d,tfBody);
        }else{                              // ノードはその他？
            //Linkノードはありえない。

            System.out.println("* The node " + node.toString() + " is not supported.");
        }
        return myBody;
    }
    

}



