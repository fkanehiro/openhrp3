package jp.go.aist.hrp.simulator;


/**
* jp/go/aist/hrp/simulator/_PathPlannerStub.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

public class _PathPlannerStub extends org.omg.CORBA.portable.ObjectImpl implements jp.go.aist.hrp.simulator.PathPlanner
{


  /**
		 * @brief DynamicsSimulatorgHxa:?		 *
		 * @param dynSim ]?F]??gogygw]??]l]|g:x:n???
		 */
  public void setDynamicsSimulator (jp.go.aist.hrp.simulator.DynamicsSimulator dynSim)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("setDynamicsSimulator", true);
                jp.go.aist.hrp.simulator.DynamicsSimulatorHelper.write ($out, dynSim);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                setDynamicsSimulator (dynSim        );
            } finally {
                _releaseReply ($in);
            }
  } // setDynamicsSimulator


  /**
		 * @brief gh]sgx]s:n?D??��Rf?g
		 *
		 * @param algorithm gb]kgt]jgz]?
		 * @param nameServer ]*?]?W]|]?
		 */
  public void initPlanner (String algorithm)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("initPlanner", true);
                $out.write_string (algorithm);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                initPlanner (algorithm        );
            } finally {
                _releaseReply ($in);
            }
  } // initPlanner


  /**
		 * @brief ]m]??]\gH ??g?		 *
		 * DynamicsSimulator :k{?r:L:?O]c]igog:nsm:]IX{?]??];?:�};?h:6Kgm]c]igoggH ??g?		 * @param name ]m]??];?gm]c]igog7?		 * @param baseLinkName humanoid]1?]??s]?]j]sgo7?		 */
  public void setRobotName (String name, String baseLinkName)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("setRobotName", true);
                $out.write_string (name);
                $out.write_string (baseLinkName);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                setRobotName (name, baseLinkName        );
            } finally {
                _releaseReply ($in);
            }
  } // setRobotName


  /**
		 * @brief gyg]|]?}?}ngH ??g?		 *
		 * �NZwo:?U?]g6}?}n?bgH ??g]?
		 * @param x xg??		 * @param y yg??		 * @param theta ??zf
		 */
  public void setStartPosition (double x, double y, double theta)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("setStartPosition", true);
                $out.write_double (x);
                $out.write_double (y);
                $out.write_double (theta);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                setStartPosition (x, y, theta        );
            } finally {
                _releaseReply ($in);
            }
  } // setStartPosition


  /**
		 * @brief gt]|]k??}ngH ??g?		 *
		 * �NZwo:?u?z?g6}?}n?bgH ??g]?
		 * @param x xg??		 * @param y yg??		 * @param theta ??zf
		 */
  public void setGoalPosition (double x, double y, double theta)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("setGoalPosition", true);
                $out.write_double (x);
                $out.write_double (y);
                $out.write_double (theta);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                setGoalPosition (x, y, theta        );
            } finally {
                _releaseReply ($in);
            }
  } // setGoalPosition


  /**
		 * @brief ??$gh]sgx]s:k]�}?]??gcgu`:?4g?		 *
		 * 7?hn]??]aggH???-A?m?:n�N??:�};:??:;K2?		 * @param properties 7??d:n�N??:�};?ce;g?Kn*2A?m?_??
		 */
  public void setProperties (String[][] properites)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("setProperties", true);
                jp.go.aist.hrp.simulator.PathPlannerPackage.PropertyHelper.write ($out, properites);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                setProperties (properites        );
            } finally {
                _releaseReply ($in);
            }
  } // setProperties


  /**
		 * @brief ??n�}R??g]g?		 *
		 * Lb:k?n?g????g?�N?z?}?}n2?]m]??gc:kSz:e:*?�NZwog?h?$:6K2?		 */
  public void calcPath ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("calcPath", true);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                calcPath (        );
            } finally {
                _releaseReply ($in);
            }
  } // calcPath


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
  public void getPath (jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHolder path)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("getPath", true);
                $in = _invoke ($out);
                path.value = jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHelper.read ($in);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                getPath (path        );
            } finally {
                _releaseReply ($in);
            }
  } // getPath


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
  public void registerCollisionCheckPair (String char1, String name1, String char2, String name2, double staticFriction, double slipFriction, double[] K, double[] C)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("registerCollisionCheckPair", true);
                $out.write_string (char1);
                $out.write_string (name1);
                $out.write_string (char2);
                $out.write_string (name2);
                $out.write_double (staticFriction);
                $out.write_double (slipFriction);
                jp.go.aist.hrp.simulator.DblSequence6Helper.write ($out, K);
                jp.go.aist.hrp.simulator.DblSequence6Helper.write ($out, C);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                registerCollisionCheckPair (char1, name1, char2, name2, staticFriction, slipFriction, K, C        );
            } finally {
                _releaseReply ($in);
            }
  } // registerCollisionCheckPair


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
  public void registerCharacter (String name, jp.go.aist.hrp.simulator.CharacterInfo cInfo)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("registerCharacter", true);
                $out.write_string (name);
                jp.go.aist.hrp.simulator.CharacterInfoHelper.write ($out, cInfo);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                registerCharacter (name, cInfo        );
            } finally {
                _releaseReply ($in);
            }
  } // registerCharacter


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
  public void registerCharacterByURL (String name, String url)
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("registerCharacterByURL", true);
                $out.write_string (name);
                $out.write_string (url);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                registerCharacterByURL (name, url        );
            } finally {
                _releaseReply ($in);
            }
  } // registerCharacterByURL


  /**
		 * @brief gw]??]l]|gw]g]s:n?D???		 *
		 * DynamicsSimulator::init(), DynamicsSimulator::setGVector(),
		 * DynamicsSimulator::initSimulation()g??:v
		 */
  public void initSimulation ()
  {
            org.omg.CORBA.portable.InputStream $in = null;
            try {
                org.omg.CORBA.portable.OutputStream $out = _request ("initSimulation", true);
                $in = _invoke ($out);
                return;
            } catch (org.omg.CORBA.portable.ApplicationException $ex) {
                $in = $ex.getInputStream ();
                String _id = $ex.getId ();
                throw new org.omg.CORBA.MARSHAL (_id);
            } catch (org.omg.CORBA.portable.RemarshalException $rm) {
                initSimulation (        );
            } finally {
                _releaseReply ($in);
            }
  } // initSimulation

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:OpenHRP/PathPlanner:1.0"};

  public String[] _ids ()
  {
    return (String[])__ids.clone ();
  }

  private void readObject (java.io.ObjectInputStream s) throws java.io.IOException
  {
     String str = s.readUTF ();
     String[] args = null;
     java.util.Properties props = null;
     org.omg.CORBA.Object obj = org.omg.CORBA.ORB.init (args, props).string_to_object (str);
     org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl) obj)._get_delegate ();
     _set_delegate (delegate);
  }

  private void writeObject (java.io.ObjectOutputStream s) throws java.io.IOException
  {
     String[] args = null;
     java.util.Properties props = null;
     String str = org.omg.CORBA.ORB.init (args, props).object_to_string (this);
     s.writeUTF (str);
  }
} // class _PathPlannerStub
