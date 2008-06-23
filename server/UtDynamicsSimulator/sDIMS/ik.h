/*!
 * @file   ik.h
 * @author Katsu Yamane
 * @date   07/10/2003
 * @brief  Inverse kinematics (UTPoser) class.
 */

#ifndef __IK_H__
#define __IK_H__

#include <chain.h>

class IKConstraint;
class IKHandle;

//#define MEASURE_TIME

//#define MAX_CONSTRAINT 128
#define MIN_JOINT_WEIGHT 0.01

#define MAX_CONDITION_NUMBER 100.0

static char* marker_top_name = "markers";
static char joint_name_separator = '_';

/*!
 * @class IK ik.h
 * @brief Main class for inverse kinematic computation.
 */
class IK
	: virtual public Chain
{
	friend class IKConstraint;
	friend class IKHandle;
	friend class IKDesire;
public:
	/*!
	 * @enum type of constraints
	 */
	enum ConstType {
		HANDLE_CONSTRAINT,  //!< position/orientation of link
		DESIRE_CONSTRAINT,  //!< desired joint values
		COM_CONSTRAINT,     //!< center of mass position
		SCALAR_JOINT_LIMIT_CONSTRAINT,  //!< joint limit for rotate and slide joints
		IK_NUM_CONSTRAINT_TYPES,
	};
	/*!
	 * @enum priority of constraints
	 */
	enum Priority {
		HIGH_PRIORITY = 0,  //!< strictly satisfied
		HIGH_IF_POSSIBLE,   //!< strictly satisfied if possible
		LOW_PRIORITY,       //!< lower priority
		N_PRIORITY_TYPES,
	};
	/*!
	 * @enum with/without constraint for each direction
	 */
	enum ConstIndex {
		HAVE_CONSTRAINT,    //!< with constraint
		NO_CONSTRAINT,      //!< without constraint
	};
	
	//! Default constructor.
	IK();
	//! Destructor.
	~IK();

	//! Number of constraints.
	int NumConstraints() {
		return n_constraints;
	}
	//! Number of constraints with specific type.
	int NumConstraints(ConstType t);

	//! Load marker information from XML file.
	int LoadMarkerXML(const char* fname, const char* _charname);

	/*!
	 * Add a new constraint.
	 * @param[in] _constraint  pointer to the new constraint object
	 * @return    ID of the constraint
	 */
	int AddConstraint(IKConstraint* _constraint);
	/*!
	 * Remove a constraint.
	 * @param[in] _id  ID of the constraint to be removed
	 * @return    0 if success, -1 if failure (e.g. constraint not found)
	 */
	int RemoveConstraint(int _id);
	/*!
	 * Remove all constraints.
	 */
	int RemoveAllConstraints();

	/*!
	 * Perform inverse kinematics computation.
	 * @param[in] timestep  integration time step
	 * @return    the maximum condition number
	 */
	double Update(double timestep);

	//! Perform inverse kinematics with adaptive time step
	double Update(double max_timestep, double min_timestep, double max_integ_error = DEFAULT_MAX_INTEG_ERROR);

	/*!
	 * Search a constraint by constraint type and joint name.
	 * @param[in] _type  constraint type
	 * @param[in] jname  joint name
	 * @param[in] charname  character name (optional)
	 * @return    pointer to the constraint
	 */
	IKConstraint* FindConstraint(ConstType _type, const char* jname, const char* charname = 0);

	/*!
	 * Search a constraint by the ID
	 * @return  pointer to the constraint
	 */
	IKConstraint* FindConstraint(int _id);

	/*!
	 * Obtain the index of a constraint by its type and joint name.
	 * @return  index of the constraint
	 */
	int ConstraintIndex(ConstType _type, const char* jname);
	/*!
	 * Obtain the index of a constraint by its type and ID.
	 * @return  index of the constraint
	 */
	int ConstraintIndex(int _id);

	/*!
	 * Obtaint the constraint ID by its type and joint name.
	 * @return  constraint ID
	 */
	int ConstraintID(ConstType _type, const char* jname);
	/*!
	 * Obtaint the constraint ID by its index.
	 * @return  constraint ID
	 */
	int ConstraintID(int _index);

	//! Reset all constraints.
	int ResetAllConstraints();

	//! Reset the constraints with the specific type.
	int ResetConstraints(ConstType t);

	//! Set joint weight, for single-DOF joints.
	int SetJointWeight(const char* jname, double _weight);
	//! Set joint weight, for multiple-DOF joints.
	int SetJointWeight(const char* jname, const fVec& _weight);

	//! Enable desire constraint of the specific joint.
	int EnableDesire(const char* jname);
	//! Disable desire constraint of the specific joint.
	int DisableDesire(const char* jname);
	//! Set gain of desire constraint of the specific joint.
	int SetDesireGain(const char* jname, double _gain);

	//! Set maximum condition number.
	void SetMaxConditionNumber(double cn) {
		max_condnum = cn;
	}
	//! Get maximum condition number.
	double GetMaxConditionNumber() {
		return max_condnum;
	}

	//! Set constraint scale parameter.
	void SetConstraintScale(double _scale, const char* charname = 0);
	//! Set character scale parameter.
	void SetCharacterScale(double _scale, const char* charname = 0);

	//! Add new marker constraint.
	/*!
	 * Add new marker constraint.
	 * @param[in] label  marker label
	 * @param[in] linkname  name of the link to which the marker is attached
	 * @param[in] charname  character name of the link
	 * @param[in] rel_pos  position of the marker in joint frame
	 */
	IKHandle* AddMarker(const std::string& label, const std::string& linkname, const std::string& charname, const fVec3& rel_pos);
#ifndef SEGA
	IKHandle* AddMarker(const char* marker_name, Joint* parent_joint, const fVec3& abs_pos = 0.0);
	int SaveMarkers(const char* fname);
	int EditMarker(IKHandle* marker, const char* body_name, Joint* parent_joint, const fVec3& abs_pos = 0.0);
#endif
protected:
#ifdef SEGA
	int init();
	int myinit();
#else
	int init(SceneGraph* sg);
	int myinit(SceneGraph* sg);
	int load_markers(SceneGraph* sg, Joint* rj);
	int load_markers(TransformNode* marker_top);
	IKHandle* add_marker(const char* marker_name, Joint* parent_joint, const fVec3& abs_pos);
	IKHandle* add_marker(TransformNode* tn);
	int edit_marker(IKHandle* marker, const char* body_name, Joint* parent_joint, const fVec3& abs_pos);
	int save_marker(TransformNode* top_tnode, IKHandle* h);
#endif

	//! compute the Jacobian matrix
	int calc_jacobian();

	int calc_feedback();
	
	//! compute the joint velocity
	double solve_ik();

	int copy_jacobian();

	void set_character_scale(Joint* jnt, double _scale, const char* charname);

	//! number of current constraints
	int n_constraints;

	//! number of total constraints used so far, including removed ones
	int n_total_const;

	//! list of current constraints
	IKConstraint** constraints;

	int n_assigned_constraints;
	int assign_constraints(int _n);

	fMat J[N_PRIORITY_TYPES];  //!< Jacobian matrix of each type
	fVec fb[N_PRIORITY_TYPES]; //!< constraint error of each type
	fVec weight[N_PRIORITY_TYPES]; //!< weight of each type
	int n_const[N_PRIORITY_TYPES]; //!< number of constraints of each type
	int n_all_const;

	//! maximum condition number
	double max_condnum;

	//! joint weights
	fVec joint_weights;

};

/*!
 * @class IKConstraint ik.h
 * @brief Base class for constraints.
 */
class IKConstraint
{
	friend class IK;
public:
	//! Default constructor.
	/*!
	 * Default constructor.
	 * @param[in] _ik  pointer to the IK object
	 * @param[in] _jname name of the joint to which the constraint is attached
	 * @param[in] _jnt pointer to the joint
	 * @param[in] _pri priority of the constraint
	 * @param[in] _gain gain of the constraint
	 */
	IKConstraint(IK* _ik, const char* _jname, Joint* _jnt, IK::Priority _pri, double _gain) {
		ik = _ik;
		joint_name = 0;
		enabled = true;
		active = true;
		if(_jname)
		{
			joint_name = new char [strlen(_jname) + 1];
			strcpy(joint_name, _jname);
		}
		joint = _jnt;
		n_const = 0;
		priority = _pri;
		gain = _gain;
		i_const = -1;
		id = -1;
		is_dropped = false;
	}
	
	//! Destructor.
	virtual ~IKConstraint() {
		if(joint_name) delete[] joint_name;
	}

	//! Returns the constraint type.
	virtual IK::ConstType GetType() = 0;

	virtual int Reset() {
		return 0;
	}

	//! enable the constraint
	void Enable() {
		enabled = true;
	}
	//! disable the constraint
	void Disable() {
		enabled = false;
	}

	//! activate the constraint
	void Activate() {
		active = true;
	}
	//! diactivate the constraint
	void Diactivate() {
		active = false;
	}
	//! whether the constraint is activate
	int Active() {
		return active;
	}

	//! set the gain
	void SetGain(double _gain) {
		gain = _gain;
	}
	//! get the gain
	double GetGain() {
		return gain;
	}

	//! set the priproty
	void SetPriority(IK::Priority _pri) {
		priority = _pri;
	}
	//! get the priproty
	IK::Priority GetPriority() {
		return priority;
	}

	int iConst() {
		return i_const;
	}
	int nConst() {
		return n_const;
	}

	Joint* GetJoint() {
		return joint;
	}
	int ID() {
		return id;
	}
	int Dropped() {
		if(priority == IK::HIGH_IF_POSSIBLE) return is_dropped;
		return false;
	}

	virtual void SetCharacterScale(double _scale, const char* charname = 0) {
	}

protected:
	//! Computes the constraint Jacobian.
	/*!
	 * Computes the constraint Jacobian.
	 * implementing the function:
	 * -# override @c calc_jacobian_slide() - @c calc_jacobian_free() \n
	 *    the default implementation of @c calc_jacobian() calls each function depending on the joint type
	 * -# overried @c calc_jacobian() \n
	 *    if the implementation does not depend on the joint type
	 */
	virtual int calc_jacobian();
	virtual int calc_jacobian_rotate(Joint* cur) {
		return 0;
	}
	virtual int calc_jacobian_slide(Joint* cur) {
		return 0;
	}
	virtual int calc_jacobian_sphere(Joint* cur) {
		return 0;
	}
	virtual int calc_jacobian_free(Joint* cur) {
		return 0;
	}

	//! the function recursively called for all joints (don't override)
	int calc_jacobian(Joint* cur);
	//! copy each constraint Jacobian to the whole Jacobian matrix
	int copy_jacobian();

	//! compute the feedback velocity 
	virtual int calc_feedback() = 0;

	IK* ik;
	Joint* joint;          //!< target joint
	char* joint_name;      
	int id;                //!< ID (unique to each constraint)

	IK::Priority priority; //! priority
	double gain;           //! feedback gain
	fVec weight;           //! weight
	int n_const;           //! number of constraints
	int enabled;
	int active;
	
	fMat J;                //! Jacobian matrix  (n_const x total DOF)
	fVec fb;               //! feedback velocity (n_const)
	int i_const;           //! index in the constraints with the same priority

	int is_dropped;

private:
	int count_constraints();
};

/*!
 * @class IKHandle ik.h
 * @brief Position constraint.
 */
class IKHandle
	: public IKConstraint
{
	friend class IK;
public:
	/*!
	 * Default constructor.
	 * @param[in] _rel_pos  relative position in joint frame (default: 0,0,0)
	 * @param[in] _rel_att  relative orientation in joint frame (default: identity)
	 * @param[in] _other_joint  joint to which the constraint is attached (default: world)
	 */
	IKHandle(IK* _ik, const char* _jname, Joint* _jnt,
			 IK::ConstIndex cindex[6], IK::Priority _pri, double _gain,
			 const fVec3& _rel_pos = 0.0, const fMat33& _rel_att = 1.0,
			 Joint* _other_joint = 0)
			: IKConstraint(_ik, _jname, _jnt, _pri, _gain),
			  rel_pos(_rel_pos), rel_att(_rel_att) {
		for(int i=0; i<6; i++)
		{
			const_index[i] = cindex[i];
			if(const_index[i] == IK::HAVE_CONSTRAINT)
				n_const++;
		}
		other_joint = _other_joint;
		// compute the current absolute position/orientation
		abs_pos.zero();
		abs_att.identity();
		if(joint)
		{
			abs_att.mul(joint->abs_att, rel_att);
			abs_pos.mul(joint->abs_att, rel_pos);
			abs_pos += joint->abs_pos;
			// relative position/orientation
			if(other_joint)
			{
				static fVec3 pp;
				static fMat33 rt, att;
				rt.tran(other_joint->abs_att);
				att.set(abs_att);
				pp.sub(abs_pos, other_joint->abs_pos);
				abs_pos.mul(rt, pp);
				abs_att.mul(rt, att);
			}
		}
		J.resize(n_const, ik->NumDOF());
		J.zero();
		fb.resize(n_const);
		fb.zero();
		weight.resize(n_const);
		weight = 1.0;
	}
	
	~IKHandle() {
	}

	//! reset the constraint position/orientation by the current values
	int Reset() {
		if(joint)
		{
			abs_att.mul(joint->abs_att, rel_att);
			abs_pos.mul(joint->abs_att, rel_pos);
			abs_pos += joint->abs_pos;
			// relative position/orientation
			if(other_joint)
			{
				static fVec3 pp;
				static fMat33 rt, att;
				rt.tran(other_joint->abs_att);
				att.set(abs_att);
				pp.sub(abs_pos, other_joint->abs_pos);
				abs_pos.mul(rt, pp);
				abs_att.mul(rt, att);
			}
		}
		return 0;
	}
	//! set the constraint position
	void SetPos(const fVec3& _abs_pos) {
		abs_pos.set(_abs_pos);
	}
	//! set the constraint orientation
	void SetAtt(const fMat33& _abs_att) {
		abs_att.set(_abs_att);
	}
	
	//! obtain constraint position
	void GetPos(fVec3& _abs_pos) {
		_abs_pos.set(abs_pos);
	}
	//! obtain constraint orientation
	void GetAtt(fMat33& _abs_att) {
		_abs_att.set(abs_att);
	}

	//! set relative position of the constraint
	void SetRelPos(const fVec3& _rel_pos) {
		rel_pos.set(_rel_pos);
		Reset();
	}

	IK::ConstType GetType() {
		return IK::HANDLE_CONSTRAINT;
	}

	void GetConstIndex(IK::ConstIndex _cindex[]) {
		for(int i=0; i<6; i++)
		{
			_cindex[i] = const_index[i];
		}
	}

	void SetCharacterScale(double _scale, const char* charname = 0);

protected:
	int calc_jacobian();

	int calc_feedback();

	fVec3 abs_pos;  //!< current constraint position
	fMat33 abs_att; //!< current constraint orientation

	IK::ConstIndex const_index[6]; //!< with/without constraint for each DOF
	fVec3 rel_pos;
	fMat33 rel_att;

	//! connected to other joint: constrains the relative position and/or orientation w.r.t. other_joint's frame
	Joint* other_joint;
};

/*!
 * @class IKDesire ik.h
 * @brief Desired joint values.
 */
class IKDesire
	: public IKConstraint
{
	friend class IK;
public:
	IKDesire(IK* _ik, const char* _jname, Joint* _jnt,
			 IK::Priority _pri, double _gain)
			: IKConstraint(_ik, _jname, _jnt, _pri, _gain) {
		q_des = 0.0;
		att_des.identity();
		pos_des.zero();
		if(joint && joint->n_dof > 0)
		{
			n_const = joint->n_dof;
			J.resize(joint->n_dof, ik->NumDOF());
			J.zero();
			fb.resize(joint->n_dof);
			fb.zero();
			weight.resize(n_const);
			weight = 1.0;
			switch(joint->j_type)
			{
			case JROTATE:
			case JSLIDE:
				joint->GetJointValue(q_des);
				break;
			case JSPHERE:
				joint->GetJointValue(att_des);
				break;
			case JFREE:
				joint->GetJointValue(pos_des, att_des);
				break;
			default:
				break;
			}
		}
	}
	
	~IKDesire() {
	}

	int Reset() {
		if(joint && joint->n_dof > 0)
		{
			switch(joint->j_type)
			{
			case JROTATE:
			case JSLIDE:
				joint->GetJointValue(q_des);
				break;
			case JSPHERE:
				joint->GetJointValue(att_des);
				break;
			case JFREE:
				joint->GetJointValue(pos_des, att_des);
				break;
			default:
				break;
			}
		}
		return 0;
	}

	IK::ConstType GetType() {
		return IK::DESIRE_CONSTRAINT;
	}

	void SetDesire(double _q) {
		q_des = _q;
	}
	void SetDesire(const fMat33& _att) {
		att_des.set(_att);
	}
	void SetDesire(const fVec3& _pos, const fMat33& _att) {
		pos_des.set(_pos);
		att_des.set(_att);
	}
	void SetDesire(const fVec3& _pos) {
		pos_des.set(_pos);
	}

	void SetCharacterScale(double _scale, const char* charname = 0);
protected:
	int calc_jacobian_rotate(Joint* cur);
	int calc_jacobian_slide(Joint* cur);
	int calc_jacobian_sphere(Joint* cur);
	int calc_jacobian_free(Joint* cur);

	int calc_feedback();

	double q_des;    //!< desired joint value for 1DOF joints
	fMat33 att_des;  //!< desired joint orientation for 3/6 DOF joints
	fVec3 pos_des;   //!< desired joint position for 6DOF joints
};

/*!
 * @class IKCom ik.h
 * @brief Center of mass position.
 */
class IKCom
	: public IKConstraint
{
public:
	IKCom(IK* _ik, const char* _charname, IK::ConstIndex cindex[3],
			 IK::Priority _pri, double _gain)
			: IKConstraint(_ik, 0, 0, _pri, _gain) {
		charname = 0;
		if(_charname)
		{
			charname = new char [strlen(_charname)+1];
			strcpy(charname, _charname);
		}
		n_const = 0;
		for(int i=0; i<3; i++)
		{
			const_index[i] = cindex[i];
			if(const_index[i] == IK::HAVE_CONSTRAINT)
				n_const++;
		}
		fb.resize(n_const);
		des_com.zero();
		cur_com.zero();
		if(_ik)
		{
			_ik->CalcPosition();
			_ik->TotalCOM(cur_com, _charname);
			des_com.set(cur_com);
		}
	}
	~IKCom() {
		if(charname) delete[] charname;
	}

	IK::ConstType GetType() {
		return IK::COM_CONSTRAINT;
	}
	int Reset() {
		ik->TotalCOM(des_com, charname);
		return 0;
	}
	void SetPos(const fVec3& p) {
		des_com.set(p);
	}

protected:
	int calc_jacobian();
	int calc_feedback();

	IK::ConstIndex const_index[3];  //!< which of three directions (x, y, z) are constrained
	char* charname;  //!< target character name
	fVec3 des_com;   //!< desired COM position
	fVec3 cur_com;   //!< current COM position
};

/*!
 * @class IKScalarJointLimit
 * @brief Joint limit constraint for 1-DOF joints
 */
class IKScalarJointLimit
	: public IKConstraint
{
	friend class IK;
public:
	IKScalarJointLimit(IK* _ik, const char* _jname, Joint* _jnt,
					   IK::Priority _pri, double _gain)
			: IKConstraint(_ik, _jname, _jnt, _pri, _gain) {
		min_limit = false;
		max_limit = false;
		q_min = 0.0;
		q_max = 0.0;
		if(joint && (joint->j_type == JROTATE || joint->j_type == JSLIDE))
		{
			n_const = 1;
			J.resize(1, ik->NumDOF());
			J.zero();
			fb.resize(1);
			fb.zero();
			weight.resize(1);
			weight = 1.0;
		}
		else
		{
			enabled = false;
		}
	}
	
	~IKScalarJointLimit() {
	}

	IK::ConstType GetType() {
		return IK::SCALAR_JOINT_LIMIT_CONSTRAINT;
	}

	void SetMax(double _q_max) {
		q_max = _q_max;
		max_limit = true;
	}
	void SetMin(double _q_min) {
		q_min = _q_min;
		min_limit = true;
	}
	double GetMax() {
		return q_max;
	}
	double GetMin() {
		return q_min;
	}

	void SetCharacterScale(double _scale, const char* charname = 0);
protected:
	int calc_jacobian_rotate(Joint* cur);
	int calc_jacobian_slide(Joint* cur);

	int calc_feedback();

	int min_limit, max_limit;
	double q_min, q_max;
};

#endif
