/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   chain.h
 * @author Katsu Yamane
 * @date   06/17/2003
 * @brief  Classes for defining open/closed kinematic chains.
 */

#ifndef __CHAIN_H__
#define __CHAIN_H__

#include <dims_common.h>
#include <fMatrix3.h>
#include <fEulerPara.h>
#ifndef SEGA
#include <SceneGraph.h>
#endif
#include <list>
#include <cstring>

//! Defines for IntegAdaptiveEuler
#define DEFAULT_MIN_TIMESTEP 1e-6
#define DEFAULT_MAX_INTEG_ERROR 1e-4

static const char* ScaleString = "Scale_";

//! Extracts the character name from a joint name.
char* CharName(const char* _name);

//! Enums for joint types.
enum JointType {
	JUNKNOWN = 0,
	JFIXED,   //!< fixed (0DOF)
	JROTATE,  //!< rotational (1DOF)
	JSLIDE,   //!< prismatic (1DOF)
	JSPHERE,  //!< spherical (3DOF)
	JFREE,    //!< free (6DOF)
};

//! Direction of a 1-DOF joint.
enum AxisIndex {
	AXIS_NULL = -1,
	AXIS_X,
	AXIS_Y,
	AXIS_Z,
};

class Joint;

/*!
 * @struct JointData
 * @brief Temporary storage for basic joint information.
 */
struct JointData
{
	JointData() {
		name = NULL;
		parent_name = NULL;
		j_type = JUNKNOWN;
		rel_pos.zero();
		rel_att.identity();
		axis_index = AXIS_NULL;
		mass = 0.0;
		inertia.zero();
		com.zero();
	}
	
	JointData(const char* _name, const char* _parent_name, JointType _j_type,
			  const fVec3& _rpos, const fMat33& _ratt, AxisIndex _axis_index,
			  double _mass, const fMat33& _inertia, const fVec3& _com,
			  int _t_given = true) {
		name = NULL;
		parent_name = NULL;
		if(_name)
		{
			name = new char [strlen(_name) + 1];
			strcpy(name, _name);
		}
		if(_parent_name)
		{
			parent_name = new char [strlen(_parent_name) + 1];
			strcpy(parent_name, _parent_name);
		}
		j_type = _j_type;
		rel_pos.set(_rpos);
		rel_att.set(_ratt);
		axis_index = _axis_index;
		mass = _mass;
		inertia.set(_inertia);
		com.set(_com);
		t_given = _t_given;
	}
	JointData(const JointData& ref) {
		name = NULL;
		parent_name = NULL;
		if(ref.name)
		{
			name = new char [strlen(ref.name)];
			strcpy(name, ref.name);
		}
		if(ref.parent_name)
		{
			parent_name = new char [strlen(ref.parent_name) + 1];
			strcpy(parent_name, ref.parent_name);
		}
		j_type = ref.j_type;
		rel_pos.set(ref.rel_pos);
		rel_att.set(ref.rel_att);
		axis_index = ref.axis_index;
		mass = ref.mass;
		inertia.set(ref.inertia);
		com.set(ref.com);
		t_given = ref.t_given;
	}
	
	~JointData() {
		if(name) delete[] name;
		if(parent_name) delete[] parent_name;
	}

	char* name;         //!< joint name
	char* parent_name;  //!< parent joint's name
	JointType j_type;   //!< joint type
	fVec3 rel_pos;      //!< initial position in parent joint's frame
	fMat33 rel_att;     //!< initial orientation in parent joint's frame
	AxisIndex axis_index; //!< direction of the joint axis (only for 1DOF joints)
	double mass;        //!< link mass
	fMat33 inertia;     //!< link inertia
	fVec3 com;          //!< link center of mass in local frame
	int t_given;        //!< if true, the joint is torque controlled, otherwise position controlled (high-gain control)
};

/*!
 * @class Chain
 * @brief The class representing the whole mechanism. May contain multiple characters.
 */
class Chain
{
	friend class Joint;
public:
	Chain();
	virtual ~Chain();

	Joint* Root() {
		return root;
	}

	//! Find a joint from name.
	/*!
	 * Find a joint from name.  If @c charname is not null, find a joint
	 * whose @c basename is @c jname and character name is @c charname.
	 * Otherwise, find a joint whose @c name is jname.
	 * @param[in] jname    joint name
	 * @param[in] charname character name
	 * @return    the pointer to the joint, NULL if not found
	 */
	Joint* FindJoint(const char* jname, const char* charname = 0);

	//! Find a joint with ID @c _id.
	Joint* FindJoint(int _id);

	//! Find the root joint of the character with name @c charname.
	Joint* FindCharacterRoot(const char* charname);

	//! Indicates begining of creating a kinematic chain.
	/*!
	 * Indicates begining of creating a kinematic chain.  Must be called
	 * before editing the chain.
	 * @param[in] append  If false (default), clean up the current chain and build from scratch. Otherwise append new joints to the current chain.
	 */
	int BeginCreateChain(int append = false);

	//! Add the (unique) root joint.
	/*!
	 * Creates a new joint and add as the (unique) root joint. 
	 * @param[in] name  name of the root joint
	 * @param[in] grav  gravity vector
	 * @return    the pointer to the root joint; fails after the first call unless @c BeginCreateChain(false) is called.
	 */
	Joint* AddRoot(const char* name = 0, const fVec3& grav = fVec3(0.0, 0.0, 9.8));

	//! Add the (unique) root joint.
	/*!
	 * Add the (unique) root joint from a @c Joint object.
	 * @param[in] r  the pointer to the joint object
	 * @return    0 if success and -1 otherwise
	 */
	int AddRoot(Joint* r);

	//! Load the chain from a file in original (*.prm) format.
	/*!
	 * Load the chain from a file in original (*.prm) format.
	 * @param[in] fname    file name
	 * @param[in] charname character name; each joint is named as "basename:charname"
	 * @return    0 if success and -1 otherwise
	 */
	int Load(const char* fname, const char* charname = 0);

	//! Load the chain from an XML file.
	int LoadXML(const char* fname, const char* charname = 0);

	//! Add a new joint @c target as a child of joint @c p.
	/*!
	 * Add a new joint @c target as a child of joint @c p.
	 * @param[in] target  the pointer to the new joint
	 * @param[in] p       the pointer to the parent joint
	 * @return    0 if success and -1 otherwise
	 */
	int AddJoint(Joint* target, Joint* p);
	//! Add a new joint @c target as a child of joint with name @c parent_name.
	/*!
	 * Add a new joint @c target as a child of joint @c p.
	 * @param[in] target  the pointer to the new joint
	 * @param[in] parent_name  name of the parent joint
	 * @param[in] charname  character name of the parent joint
	 * @return    0 if success and -1 otherwise
	 */
	int AddJoint(Joint* target, const char* parent_name, const char* charname = 0);

	//! Create and add a joint using @c JointData structure.
	/*!
	 * Create and add a joint using @c JointData structure.
	 * @param[in] joint_data  pointer to the JointData structure
	 * @param[in] charname    character name
	 * @return    the pointer to the new joint, NULL if failed
	 */
	Joint* AddJoint(JointData* joint_data, const char* charname = 0);

	//! Automatically generate a serial chain.
	/*!
	 * Generate a serial chain with @c num_joint joints.
	 * @param[in] num_joint  number of joints
	 * @param[in] joint_data @c JointData structure to be used as the template for the joints; each joint will named as joint_data.name + number
	 * @param[in] charname   character name (optional)
	 * @param[in] parent_joint pointer to the root of the serial chain (optional); the global root if omitted or NULL
	 */
	int CreateSerial(int num_joint, const JointData& joint_data, const char* charname = 0, Joint* parent_joint = 0);
	//! Automatically generate multiple identical chains.
	/*!
	 * Generate @c num_char chains with identical structure in a prm file.
	 * @param[in] num_char  number of chains to generate
	 * @param[in] prmname   prm file name
	 * @param[in] charname  character name (the actual name includes the number starting from @c init_num)
	 * @param[in] init_pos  the position offset of the first chain
	 * @param[in] init_att  the orientation offset of the first chain
	 * @param[in] pos_offset the position offset of each chain from the previous
	 * @param[in] att_offset the orientation offset of each chain from the previous
	 * @param[in] init_num  the number of the first chain
	 */
	int CreateParallel(int num_char, const char* prmname, const char* charname,
					   const fVec3& init_pos = 0.0, const fMat33& init_att = 1.0, 
					   const fVec3& pos_offset = 0.0, const fMat33& att_offset = 1.0,
					   int init_num = 0);

	//! disconnect joint @c j from its parent
	int RemoveJoint(Joint* j);

	//! End editing a chain.
	/*!
	 * End editing a chain; performs initialization process.
	 * @param[in]  sg   the pointer to the SceneGraph to be used for setting the initial configuration.
	 */
#ifdef SEGA
	int EndCreateChain();
#else
	int EndCreateChain(SceneGraph* sg = NULL);
#endif

	//! Obtain a list of joint names.
	/*!
	 * Obtain a list of joint names.
	 * @return number of joints
	 */
	int GetJointNameList(char**& jnames);

	//! Obtain a list of pointers to all joints.
	/*!
	 * Obtain a list of pointers to all joints.
	 * @return number of joints
	 */
	int GetJointList(Joint**& joints);

	//! Set all joint values.
	/*!
	 * Set all joint values. The orientation of spherical/free joints
	 * are represented as Euler parameters.
	 * The index of the first data of a joint is @c Joint::i_value.
	 */
	int SetJointValue(const fVec& values);

	//! Set all joint velocities/accelerations.
	/*!
	 * Set all joint velocities/accelerations.
	 * The index of the first data of a joint is @c Joint::i_dof.
	 */
	int SetJointVel(const fVec& vels);
	int SetJointAcc(const fVec& accs);

	//! Set all joint forces/torques.
	/*!
	 * Set all joint forces/torques.
	 * The index of the first data of a joint is @c Joint::i_dof.
	 */
	int SetJointForce(const fVec& forces);

	//! Clear the joint forces/torques.
	int ClearJointForce();

	//! Clear the external forces/torques.
	int ClearExtForce();
	
	//! Obtain the joint values/velocities/accelerations.
	int GetJointValue(fVec& values);
	int GetJointVel(fVec& vels);
	int GetJointAcc(fVec& accs);

	//! Obtain the joint forces/torques.
	int GetJointForce(fVec& forces);

	//! Forward kinematics.
	void CalcPosition();
	void CalcVelocity();
	void CalcAcceleration();

	//! Inverse dynamics
	/*!
	 * Inverse dynamics
	 * @param[out] tau  joint forces/torques
	 */
	void InvDyn(fVec& tau);

	//! Remove all joints and clear all parameters.
	virtual void Clear();

	//! Total degrees of freedom.
	int NumDOF() {
		return n_dof;
	}
	//! Total number of joints.
	int NumJoint() {
		return n_joint;
	}
	//! Dimension of the joint value vector (using Euler parameter representation for orientation).
	int NumValue() {
		return n_value;
	}

	//! Center of mass of the chain.
	/*!
	 * Center of mass of the chain.
	 * @param[out] com    center of mass
	 * @param[in]  chname name of the character to compute com; all joints if omitted
	 * @return     the total mass
	 */
	double TotalCOM(fVec3& com, const char* chname = 0);

	//! Computes the com Jacobian.
	/*!
	 * Computes the com Jacobian.
	 * @param[out] J    com Jacobian
	 * @param[out] com  com
	 * @param[in]  chname name of the character to compute com Jacobian; all joints if omitted
	 * @return     the total mass
	 */
	double ComJacobian(fMat& J, fVec3& com, const char* chname = 0);

	//! Performs Euler integration with timestep @c timestep [s].
	int Integrate(double timestep);

	//! Performs Euler integration with adaptive timestep.
	/*!
	 * Performs Euler integration with adaptive timestep. Requires two
	 * acceleration computations per step.
	 * @param[in,out] timestep  inputs maximum timestep and overwritten by the actual timestep
	 * @param[in]     step  step in the integration (0/1)
	 * @param[in]     min_timestep  minimum timestep
	 * @param[in]     max_integ_error  maximum error
	 */
	int IntegrateAdaptive(double& timestep, int step, double min_timestep = DEFAULT_MIN_TIMESTEP, double max_integ_error = DEFAULT_MAX_INTEG_ERROR);

	//! Performs 4-th order Runge-Kutta integration.
	/*!
	 * Performs 4-th order Runge-Kutta integration.
	 * @param[in] timestep  integration time step [s]
	 * @param[in] step      step in the integration (0/1/2/3)
	 */
	int IntegrateRK4(double timestep, int step);

	//! Integrate value/velocity only.
	/*!
	 * Integrate value/velocity only. (for test)
	 * See Guendelman et al., SIGGRAPH2003
	 */
	int IntegrateValue(double timestep);
	int IntegrateVelocity(double timestep);
	int IntegrateRK4Value(double timestep, int step);
	int IntegrateRK4Velocity(double timestep, int step);

	//! Save the chain data to a file in original (*.prm) format.
	int Save(const char* fname, const char* charname = 0) const;
	int Save(ostream& ost, const char* charname = 0) const;

	//! Save the chain data to a file in XML format.
	int SaveXML(const char* fname, const char* charname = 0) const;
	int SaveXML(ostream& ost, const char* charname = 0) const;

	//! Save current joint values, velocities, and accelerations.
	int SaveStatus(fVec& value, fVec& vel, fVec& acc);

	//! Set current joint values, velocities, and accelerations.
	int SetStatus(const fVec& value, const fVec& vel, const fVec& acc);

	//! Connect two links by adding a new virtual joint.
	int Connect(Joint* virtual_joint, Joint* parent_joint);
	//! Disconnect the loop at the specified virtual joint.
	int Disconnect(Joint* j);

	//! Change torque/motion control property of a joint.
	int SetTorqueGiven(Joint* _joint, int _tg);
	//! Change torque/motion control property of all joints.
	int SetAllTorqueGiven(int _tg);
	//! Change torque/motion control property of a character
	int SetCharacterTorqueGiven(const char* charname, int _tg);

	// compute relative position/orientation from absolute values
	// make sure to call CalcPosition() first to update parent
	int set_abs_position_orientation(Joint* jnt, const fVec3& abs_pos, const fMat33& abs_att);
protected:
	//! Initialize the parameters.
#ifdef SEGA
	virtual int init();
#else
	virtual int init(SceneGraph* sg);
	void set_relative_positions(SceneGraph* sg);
	void calc_abs_positions(Joint* cur, SceneGraph* sg);
	void calc_rel_positions(Joint* cur, SceneGraph* sg);
#endif
	//! Clear arrays only; don't delete joints.
	virtual int clear_data();

	//! Pointers to the integration variables.
	/*!
	 * Pointers to the integration variables.
	 * @verbatim
	 *   all_value += timestep * all_value_dot
	 *   all_vel += timestep * all_vel_dot  @endverbatim
	 */
	double** all_value;
	double** all_value_dot;
	double** all_vel;
	double** all_vel_dot;
	//! for 4-th order Runge-Kutta
	double* j_value_dot[4];
	double* j_acc_p[4];
	double* init_value;
	double* init_vel;

	//! Chain information
	Joint* root;
	int n_value;
	int n_dof;
	int n_joint;
	int n_thrust;  //!< total DOF of the joints with t_given = false

	//! true if between @c BeginCreateChain() and @c EndCreateChain().
	int in_create_chain;

	//! true after Connect() was called; application (or subclass) must reset the flag
	int do_connect;

	void set_all_torque_given(Joint* cur, int _tg);

#ifndef SEGA
	//! XML utility functions
public:
	struct scale_object
	{
		scale_object(): joint_name("") {
			scale = 1.0;
		}
		scale_object(const scale_object& ref): joint_name(ref.joint_name) {
			scale = ref.scale;
		}
		~scale_object() {
		}
		
		void set_joint_name(const char* _joint_name, const char* _char_name) {
			joint_name = "";
			if(_joint_name)
			{
				joint_name = std::string(_joint_name);
				if(_char_name)
				{
					std::string sep(1, charname_separator);
					joint_name.append(sep);
					joint_name.append(std::string(_char_name));
				}
			}
		}
		
		double scale;
		std::string joint_name;
	};

	void add_scale_object(const scale_object& _s);
	void clear_scale_object_list();

	void ApplyGeomScale(SceneGraph* sg);
protected:
	std::list<scale_object> scale_object_list;

	void init_scale(SceneGraph* sg);
	virtual void apply_scale();
	void apply_scale(const scale_object& _s);
	void reset_scale();
	void init_scale_sub(Node* node);

	void apply_scale_top(Joint* top, double scale);
	void apply_scale_sub(Joint* cur, double scale);
	void reset_scale_sub(Joint* jnt);

	void _apply_scale(Joint* jnt, double scale);
	void apply_geom_scale(SceneGraph* sg, Joint* cur);
#endif
};

/*!
 * @class Joint
 * @brief The class for representing a joint.
 */
class Joint
{
	friend class Chain;
public:
	Joint();
	Joint(const char* name, JointType jt = JUNKNOWN, 
		  const fVec3& rpos = 0.0, const fMat33& ratt = 1.0,
		  AxisIndex _axis_index = AXIS_NULL,
		  int t_given = true);
	Joint(JointData* jdata, const char* charname);
	~Joint();

	void SetJointData(JointData* jdata, const char* charname);
	void UpdateJointType();

	/*!
	 * @name 1DOF joints
	 * Obtain/set the joint value/velocity/acceleration/force of a 1-DOF joint.
	 */
	/*@{*/
	int SetJointValue(double _q);
	int SetJointVel(double _qd);
	int SetJointAcc(double _qdd);
	int GetJointValue(double& _q);
	int GetJointVel(double& _qd);
	int GetJointAcc(double& _qdd);
	int SetJointForce(double _tau);
	int GetJointForce(double& _tau);
	/*@}*/

	/*!
	 * @name 3DOF joints
	 * Obtain/set the joint value/velocity/acceleration/force of a 3-DOF joint.
	 */
	/*@{*/
	int SetJointValue(const fMat33& r);
	int SetJointValue(const fEulerPara& ep);
	int SetJointVel(const fVec3& rd);
	int SetJointAcc(const fVec3& rdd);
	int SetJointAcc(double ax, double ay, double az);
	int GetJointValue(fMat33& r);
	int GetJointVel(fVec3& rd);
	int GetJointAcc(fVec3& rdd);
	int SetJointForce(const fVec3& _n3);
	int GetJointForce(fVec3& _n3);
	/*@}*/

	/*!
	 * @name 6DOF joints
	 * Obtain/set the joint value/velocity/acceleration/force of a 6-DOF joint.
	 */
	/*@{*/
	int SetJointValue(const fVec3& p, const fMat33& r);
	int SetJointValue(const fVec3& p, const fEulerPara& ep);
	int SetJointVel(const fVec3& pd, const fVec3& rd);
	int SetJointAcc(const fVec3& pdd, const fVec3& rdd);
	int SetJointAcc(double lx, double ly, double lz, double ax, double ay, double az);
	int GetJointValue(fVec3& p, fMat33& r);
	int GetJointVel(fVec3& pd, fVec3& rd);
	int GetJointAcc(fVec3& pdd, fVec3& rdd);
	int SetJointForce(const fVec3& _f3, const fVec3& _n3);
	int GetJointForce(fVec3& _f3, fVec3& _n3);
	/*@}*/

	//! Set relative position
	int SetJointPosition(const fVec3& p);
	//! Set relative orientation
	int SetJointOrientation(const fMat33& r);

	//! Set joint type to rotational.
	void SetRotateJointType(const fVec3& rpos, const fMat33& ratt, AxisIndex ai);
	//! Set joint type to prismatic.
	void SetSlideJointType(const fVec3& rpos, const fMat33& ratt, AxisIndex ai);
	//! Set joint type to spherical.
	void SetSphereJointType(const fVec3& rpos, const fMat33& ratt);
	//! Set joint type to fixed.
	void SetFixedJointType(const fVec3& rpos, const fMat33& ratt);
	//! Set joint type to free.
	void SetFreeJointType(const fVec3& rpos, const fMat33& ratt);

	//! The Jacobian matrix of position/orientation w.r.t. the joint values.
	int CalcJacobian(fMat& J);

	//! 2nd-order derivatives of the Jacobian matrix.
	int CalcJacobian2(fMat* J2);

	//! Jdot x thdot
	int CalcJdot(fVec& jdot);

	//! Total DOF of the descendants (end link side).
	int DescendantDOF();

	//! Total number of joints of the descendants (end link side).
	int DescendantNumJoints();

	//! Identifies whether the target joint is a direct descendant.
	int isDescendant(Joint* target);

	//! Identifies whether the target joint is a direct ascendant.
	int isAscendant(Joint* target);

	//! Returns the total mass of the child links.
	double TotalMass();

	//! Returns the scale applied to the joint.
	double Scale() {
		return cur_scale;
	}

	//! Returns the character name.
	char* CharName() const {
		char* ret = strrchr(name, charname_separator);
		if(ret) return ret+1;
		return 0;
	}

	//! Change the joint name.
	void SetName(const char* _name, const char* _charname = 0) {
		if(name) delete[] name;
		name = 0;
		if(basename) delete[] basename;
		basename = 0;
		if(_name)
		{
			if(_charname)
			{
				name = new char [strlen(_name) + strlen(_charname) + 2];
				sprintf(name, "%s%c%s", _name, charname_separator, _charname);
			}
			else
			{
				name = new char [strlen(_name) + 1];
				strcpy(name, _name);
			}
			char* ch = strrchr(name, charname_separator);
			if(ch) *ch = '\0';
			basename = new char [strlen(name) + 1];
			strcpy(basename, name);
			if(ch) *ch = charname_separator;
		}
	}

	/*!
	 * @name User-specified joint parameters
	 */
	Joint* parent;    //!< pointer to the parent joint
	Joint* brother;   //!< pointer to the brother joint
	Joint* child;     //!< pointer to the child joint
	
	Joint* real;      //!< pointer to the real (connected) joint; for closed chains.
	fVec3 rpos_real;  //!< relative position in the real joint frame
	fMat33 ratt_real; //!< relative orientation in the real joint frame

	char* name;        //!< joint name (including the character name)
	char* basename;    //!< joint base name (without the character name)
	char* realname;    //!< name of the real joint (for closed chains)
	JointType j_type;  //!< joint type

	fVec3 axis;        //!< joint axis in local frame (for 1DOF joints)
	fVec3 init_pos;    //!< origin of the joint value (for prismatic joints)
	fMat33 init_att;   //!< origin of the joint value (for rotational joints)

	fVec3 rel_pos;     //!< (initial) position in parent joint's frame (for 0/3/6 DOF joints)
	fMat33 rel_att;    //!< (initial) orientation in parent joint's frame (for 0/3/6 DOF joints)
	fEulerPara rel_ep; //!< Euler parameter representation of @c rel_att (for 0/3/6 DOF joints)

	double mass;       //!< mass
	fMat33 inertia;    //!< intertia
	fVec3 loc_com;     //!< center of mass in local frame
	double gear_ratio;
	double rotor_inertia;
	int t_given;       //!< torque or motion controlled

	/*!
	 * @name Variables automatically set or computed
	 */
	/*@{*/
	int n_dof;         //!< degrees of freedom (0/1/3/6)
	int n_thrust;      //!< DOF for motion controlled joints
	int n_root_dof;    //!< total DOF in the root side

	int i_value;       //!< index in all joint values
	int i_dof;         //!< index in all DOF
	int i_thrust;      //!< index in all motion controlled joints
	int i_joint;       //!< index of the joint

	double q;       //!< joint value (for 1DOF joints)
	double qd;      //!< joint velocity (for 1DOF joints)
	double qdd;     //!< joint acceleration (for 1DOF joints)

	fVec3 rel_lin_vel;
	fVec3 rel_ang_vel;
	fVec3 rel_lin_acc;
	fVec3 rel_ang_acc;

	double tau;   //!< joint force/torque (for 1DOF joints)
	fVec3 tau_f;  //!< joint force for 6DOF joints
	fVec3 tau_n;  //!< joint torque for 3 and 6 DOF joints
	fVec3 ext_force;  //!< external force
	fVec3 ext_moment; //!< external moment around the local frame

	/*! @name results of forward kinematics computation */
	/*@{*/
	fVec3 abs_pos;      //!< absolute position
	fMat33 abs_att;     //!< absolute orientation
	fVec3 loc_lin_vel;  //!< linear velocity in local frame
	fVec3 loc_ang_vel;  //!< angular velocity in local frame
	fVec3 loc_com_vel;  //!< com velocity in local frame
	fVec3 loc_lin_acc;  //!< linear acceleration in local frame
	fVec3 loc_ang_acc;  //!< angular acceleration in local frame
	fVec3 loc_com_acc;  //!< com acceleration in local frame
	/*@}*/

	/*! @name Results of inverse dynamics computation 
	 * force/moment applied to joints/links
	 */
	/*@{*/
	fVec3 joint_f, total_f;
	fVec3 joint_n, total_n;
	/*@}*/

	/*! @name Velocities and accelerations in parent joint's frame for integration */
	/*@{*/
	fVec3 p_lin_vel;
	fEulerPara p_ep_dot;
	fVec3 p_ang_vel;
	fVec3 p_lin_acc;
	fVec3 p_ang_acc;
	/*@}*/

protected:
	Chain* chain;

	double total_mass();

	void init();
	void init_arrays();
	void init_virtual();
	void _init_virtual();

	void clear_data();

	int pre_integrate();
	int post_integrate();

	void clear();

	int set_joint_value(const fVec& values);
	int set_joint_vel(const fVec& vels);
	int set_joint_acc(const fVec& accs);
	int set_joint_force(const fVec& forces);

	int get_joint_value(fVec& values);
	int get_joint_vel(fVec& vels);
	int get_joint_acc(fVec& accs);
	int get_joint_force(fVec& forces);

	void calc_position();
	void calc_velocity();
	void calc_acceleration();

	void inv_dyn();
	void inv_dyn_1();
	void inv_dyn_2();
	void calc_joint_force(fVec& tau);

	void get_joint_name_list(char** jnames);
	void get_joint_list(Joint** joints);

	void add_child(Joint* j);
	int remove_child(Joint* j);
	Joint* find_joint(const char* n, const char* charname);
	Joint* find_joint(int _id);

	int calc_jacobian(fMat& J, Joint* target);
	int calc_jacobian_rotate(fMat& J, Joint* target);
	int calc_jacobian_slide(fMat& J, Joint* target);
	int calc_jacobian_sphere(fMat& J, Joint* target);
	int calc_jacobian_free(fMat& J, Joint* target);

	int calc_jacobian_2(fMat* J, Joint* target);
	int calc_jacobian_2_rotate_sub(fMat* J, Joint* target, Joint* j1);
	int calc_jacobian_2_slide_sub(fMat* J, Joint* target, Joint* j1);
	int calc_jacobian_2_sphere_sub(fMat* J, Joint* target, Joint* j1);
	int calc_jacobian_2_free_sub(fMat* J, Joint* target, Joint* j1);

	int calc_jacobian_2_rotate_rotate(fMat* J, Joint* target, Joint* j1, const fVec3& axis1, int index1, const fVec3& axis2, int index2);
	int calc_jacobian_2_slide_rotate(fMat* J, Joint* target, Joint* jk, const fVec3& k_axis, int k_index, const fVec3& loc_axis, int loc_index);
	int calc_jacobian_2_rotate_slide(fMat* J, Joint* target, Joint* jk, const fVec3& k_axis, int k_index, const fVec3& loc_axis, int loc_index);


	int descendant_dof();
	int descendant_num_joints();
	int is_descendant(Joint* cur, Joint* target);
	
	int save(ostream& ost, int indent, const char* charname) const;
	int save_xml(ostream& ost, int indent, const char* charname) const;

	int clear_joint_force();
	int clear_ext_force();

	double total_com(fVec3& com, const char* chname);
	double com_jacobian(fMat& J, fVec3& com, const char* chname);

private:
	double cur_scale;
};

#endif
