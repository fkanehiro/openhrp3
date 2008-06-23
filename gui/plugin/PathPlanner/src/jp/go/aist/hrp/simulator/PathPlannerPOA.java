package jp.go.aist.hrp.simulator;


/**
* jp/go/aist/hrp/simulator/PathPlannerPOA.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

public abstract class PathPlannerPOA extends org.omg.PortableServer.Servant
 implements jp.go.aist.hrp.simulator.PathPlannerOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("setDynamicsSimulator", new java.lang.Integer (0));
    _methods.put ("initPlanner", new java.lang.Integer (1));
    _methods.put ("setRobotName", new java.lang.Integer (2));
    _methods.put ("setStartPosition", new java.lang.Integer (3));
    _methods.put ("setGoalPosition", new java.lang.Integer (4));
    _methods.put ("setProperties", new java.lang.Integer (5));
    _methods.put ("calcPath", new java.lang.Integer (6));
    _methods.put ("getPath", new java.lang.Integer (7));
    _methods.put ("registerCollisionCheckPair", new java.lang.Integer (8));
    _methods.put ("registerCharacter", new java.lang.Integer (9));
    _methods.put ("registerCharacterByURL", new java.lang.Integer (10));
    _methods.put ("initSimulation", new java.lang.Integer (11));
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {

  /**
		 * @brief DynamicsSimulatorgHxa:?		 *
		 * @param dynSim ]?F]??gogygw]??]l]|g:x:n???
		 */
       case 0:  // OpenHRP/PathPlanner/setDynamicsSimulator
       {
         jp.go.aist.hrp.simulator.DynamicsSimulator dynSim = jp.go.aist.hrp.simulator.DynamicsSimulatorHelper.read (in);
         this.setDynamicsSimulator (dynSim);
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief gh]sgx]s:n?D??��Rf?g
		 *
		 * @param algorithm gb]kgt]jgz]?
		 * @param nameServer ]*?]?W]|]?
		 */
       case 1:  // OpenHRP/PathPlanner/initPlanner
       {
         String algorithm = in.read_string ();
         this.initPlanner (algorithm);
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief ]m]??]\gH ??g?		 *
		 * DynamicsSimulator :k{?r:L:?O]c]igog:nsm:]IX{?]??];?:�};?h:6Kgm]c]igoggH ??g?		 * @param name ]m]??];?gm]c]igog7?		 * @param baseLinkName humanoid]1?]??s]?]j]sgo7?		 */
       case 2:  // OpenHRP/PathPlanner/setRobotName
       {
         String name = in.read_string ();
         String baseLinkName = in.read_string ();
         this.setRobotName (name, baseLinkName);
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief gyg]|]?}?}ngH ??g?		 *
		 * �NZwo:?U?]g6}?}n?bgH ??g]?
		 * @param x xg??		 * @param y yg??		 * @param theta ??zf
		 */
       case 3:  // OpenHRP/PathPlanner/setStartPosition
       {
         double x = in.read_double ();
         double y = in.read_double ();
         double theta = in.read_double ();
         this.setStartPosition (x, y, theta);
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief gt]|]k??}ngH ??g?		 *
		 * �NZwo:?u?z?g6}?}n?bgH ??g]?
		 * @param x xg??		 * @param y yg??		 * @param theta ??zf
		 */
       case 4:  // OpenHRP/PathPlanner/setGoalPosition
       {
         double x = in.read_double ();
         double y = in.read_double ();
         double theta = in.read_double ();
         this.setGoalPosition (x, y, theta);
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief ??$gh]sgx]s:k]�}?]??gcgu`:?4g?		 *
		 * 7?hn]??]aggH???-A?m?:n�N??:�};:??:;K2?		 * @param properties 7??d:n�N??:�};?ce;g?Kn*2A?m?_??
		 */
       case 5:  // OpenHRP/PathPlanner/setProperties
       {
         String properites[][] = jp.go.aist.hrp.simulator.PathPlannerPackage.PropertyHelper.read (in);
         this.setProperties (properites);
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief ??n�}R??g]g?		 *
		 * Lb:k?n?g????g?�N?z?}?}n2?]m]??gc:kSz:e:*?�NZwog?h?$:6K2?		 */
       case 6:  // OpenHRP/PathPlanner/calcPath
       {
         this.calcPath ();
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief ??$:L:?uZwog??�}g?		 *
		 * �NZwo:n?b?�}Rx,y,theta:n??sf:y:???�}?_??:h:�};?
		 *
		 * ]1?];::g:?K:h:*?
		 *
		 * path[0][0] ~ path[0][2]:��?4?x,y,theta:g:??:;Ig?K?b2?		 *
		 * path[N][0] ~ path[N][2]:?V]|]k:n?b:h:jg]?
		 * @param path �NZwo:n?b?�}Rx,y,theta:n??sf:y:???�}?_??
		 */
       case 7:  // OpenHRP/PathPlanner/getPath
       {
         jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHolder path = new jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHolder ();
         this.getPath (path);
         out = $rh.createReply();
         jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHelper.write (out, path.value);
         break;
       }


  /**
		 * @if jp
		 * @brief fIj5d��?]?Dg?}	? :~:6?
		 *
		 * :?:g{?r:L:??gb7?ck:nfIj5d��?:Za?Og?*:6?
		 *
		 * K :h C :n?w:R0:k:6K:hgy]�}?]sgp-]??],s??h:L:~:S2?		 * @param	char1	  ]j]sgo:ngm]c]igog7?		 * @param	name1	  ]j]sgo7?		 * @param	char2     g?gs?:ngm]c]igog7?		 * @param	name2     ]j]sgo7?		 * @param	staticFriction  ?Nmb��i+f??
		 * @param   slipFriction  ?_+f??
		 * @param	K :p:m??
		 * @param	C ]??]??
		 * @else
		 * Add Collision Pairs
		 * @param	char1	  Name of character for first link
		 * @param	name1	  Name of first link
		 * @param	char2     Name of character for second link
		 * @param	name2     Name of second link
		 * @param	staticFriction  Static Friction
		 * @param   slipFriction  Slip Friction
		 * @param	K Parameters for Spring
		 * @param	C Parameters for Damper
		 * K and C should be of zero length for no Spring-Damper stuff.
		 * @endif
		 */
       case 8:  // OpenHRP/PathPlanner/registerCollisionCheckPair
       {
         String char1 = in.read_string ();
         String name1 = in.read_string ();
         String char2 = in.read_string ();
         String name2 = in.read_string ();
         double staticFriction = in.read_double ();
         double slipFriction = in.read_double ();
         double K[] = jp.go.aist.hrp.simulator.DblSequence6Helper.read (in);
         double C[] = jp.go.aist.hrp.simulator.DblSequence6Helper.read (in);
         this.registerCollisionCheckPair (char1, name1, char2, name2, staticFriction, slipFriction, K, C);
         out = $rh.createReply();
         break;
       }


  /**
		 * @if jp
		 * @brief gm]c]igogg$3?r:6K2?		 * 
		 * ModelLoader :]I�}Ig?????:ggm]c]igogg$3?r:�}*:6?
		 * @param name gw]??]l]|gw]g]s:g:ngm]c]igog7?		 * @param cinfo ModelLoader :]I�}Ig?K CharacterInfo
		 * @else
		 * Register a character
		 * @param	name	Object Character Name for Simulation
		 * @param	cinfo	CharacterInfo
		 * @endif
		 */
       case 9:  // OpenHRP/PathPlanner/registerCharacter
       {
         String name = in.read_string ();
         jp.go.aist.hrp.simulator.CharacterInfo cInfo = jp.go.aist.hrp.simulator.CharacterInfoHelper.read (in);
         this.registerCharacter (name, cInfo);
         out = $rh.createReply();
         break;
       }


  /**
		 * @if jp
		 * @brief URL :]Igm]c]igogg$3?r:6K2?		 * 
		 * ModelLoader :]I�}Ig?????:ggm]c]igogg$3?r:�}*:6?
		 * @param name gw]??]l]|gw]g]s:g:ngm]c]igog7?		 * @param url ]b]??URL
		 * @else
		 * Register a character from model url
		 * @param	name	Object Character Name for Simulation
		 * @param	url		Model url
		 * @endif
		 */
       case 10:  // OpenHRP/PathPlanner/registerCharacterByURL
       {
         String name = in.read_string ();
         String url = in.read_string ();
         this.registerCharacterByURL (name, url);
         out = $rh.createReply();
         break;
       }


  /**
		 * @brief gw]??]l]|gw]g]s:n?D???		 *
		 * DynamicsSimulator::init(), DynamicsSimulator::setGVector(),
		 * DynamicsSimulator::initSimulation()g??:v
		 */
       case 11:  // OpenHRP/PathPlanner/initSimulation
       {
         this.initSimulation ();
         out = $rh.createReply();
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:OpenHRP/PathPlanner:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public PathPlanner _this() 
  {
    return PathPlannerHelper.narrow(
    super._this_object());
  }

  public PathPlanner _this(org.omg.CORBA.ORB orb) 
  {
    return PathPlannerHelper.narrow(
    super._this_object(orb));
  }


} // class PathPlannerPOA
