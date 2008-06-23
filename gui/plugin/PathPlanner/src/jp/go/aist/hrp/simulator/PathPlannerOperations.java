package jp.go.aist.hrp.simulator;


/**
* jp/go/aist/hrp/simulator/PathPlannerOperations.java .
* IDL-to-Java �R���p�C�� (�|�[�^�u��), �o�[�W���� "3.1" �Ő���
* ������: PathPlanner.idl
* 2008�N5��21�� 16��23��58�b JST
*/

public interface PathPlannerOperations 
{

  /**
		 * @brief DynamicsSimulatorgHxa:?		 *
		 * @param dynSim ]?F]??gogygw]??]l]|g:x:n???
		 */
  void setDynamicsSimulator (jp.go.aist.hrp.simulator.DynamicsSimulator dynSim);

  /**
		 * @brief gh]sgx]s:n?D??��Rf?g
		 *
		 * @param algorithm gb]kgt]jgz]?
		 * @param nameServer ]*?]?W]|]?
		 */
  void initPlanner (String algorithm);

  /**
		 * @brief ]m]??]\gH ??g?		 *
		 * DynamicsSimulator :k{?r:L:?O]c]igog:nsm:]IX{?]??];?:�};?h:6Kgm]c]igoggH ??g?		 * @param name ]m]??];?gm]c]igog7?		 * @param baseLinkName humanoid]1?]??s]?]j]sgo7?		 */
  void setRobotName (String name, String baseLinkName);

  /**
		 * @brief gyg]|]?}?}ngH ??g?		 *
		 * �NZwo:?U?]g6}?}n?bgH ??g]?
		 * @param x xg??		 * @param y yg??		 * @param theta ??zf
		 */
  void setStartPosition (double x, double y, double theta);

  /**
		 * @brief gt]|]k??}ngH ??g?		 *
		 * �NZwo:?u?z?g6}?}n?bgH ??g]?
		 * @param x xg??		 * @param y yg??		 * @param theta ??zf
		 */
  void setGoalPosition (double x, double y, double theta);

  /**
		 * @brief ??$gh]sgx]s:k]�}?]??gcgu`:?4g?		 *
		 * 7?hn]??]aggH???-A?m?:n�N??:�};:??:;K2?		 * @param properties 7??d:n�N??:�};?ce;g?Kn*2A?m?_??
		 */
  void setProperties (String[][] properites);

  /**
		 * @brief ??n�}R??g]g?		 *
		 * Lb:k?n?g????g?�N?z?}?}n2?]m]??gc:kSz:e:*?�NZwog?h?$:6K2?		 */
  void calcPath ();

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
  void getPath (jp.go.aist.hrp.simulator.PathPlannerPackage.PointArrayHolder path);

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
  void registerCollisionCheckPair (String char1, String name1, String char2, String name2, double staticFriction, double slipFriction, double[] K, double[] C);

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
  void registerCharacter (String name, jp.go.aist.hrp.simulator.CharacterInfo cInfo);

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
  void registerCharacterByURL (String name, String url);

  /**
		 * @brief gw]??]l]|gw]g]s:n?D???		 *
		 * DynamicsSimulator::init(), DynamicsSimulator::setGVector(),
		 * DynamicsSimulator::initSimulation()g??:v
		 */
  void initSimulation ();
} // interface PathPlannerOperations
