/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*
 * ik.cpp
 * Create: Katsu Yamane, 03.07.10
 */

#include "ik.h"
#include <string>

IK::IK(): Chain()
{
	n_assigned_constraints = 0;
	n_constraints = 0;
	n_total_const = 0;
	for(int m=0; m<N_PRIORITY_TYPES; m++)
		n_const[m] = 0;
	n_all_const = 0;
	max_condnum = MAX_CONDITION_NUMBER;
	assign_constraints(100);
}

IK::~IK()
{
#ifdef MEASURE_TIME
	extern int n_update;
	extern double calc_jacobian_time;
	extern double solve_ik_time;
	extern double high_constraint_time;
	extern double low_constraint_time;
	cerr << "n_update = " << n_update << endl;
	cerr << "calc_jacobian = " << calc_jacobian_time << endl;
	cerr << "solve_ik = " << solve_ik_time << endl;
	cerr << "high_constraint = " << high_constraint_time << endl;
	cerr << "low_constraint = " << low_constraint_time << endl;
#endif

	for(int i=0; i<n_assigned_constraints; i++)
	{
		if(constraints[i]) delete constraints[i];
	}
	delete[] constraints;
}

#ifdef SEGA
int IK::init()
#else
int IK::init(SceneGraph* sg)
#endif
{
#ifdef SEGA
	Chain::init();
	myinit();
#else
	Chain::init(sg);
	myinit(sg);
#endif
	return 0;
}

#ifdef SEGA
int IK::myinit()
#else
int IK::myinit(SceneGraph* sg)
#endif
{
	joint_weights.resize(n_dof);
	joint_weights = 1.0;  // default weights
#ifndef SEGA
	// load markers from sg
	if(sg)
	{
		CalcPosition();
		Joint* j;
		for(j=root->child; j; j=j->brother)
			load_markers(sg, j);
	}
#endif
	return 0;
}

#ifndef SEGA
int IK::load_markers(SceneGraph* sg, Joint* rj)
{
	char* marker_top_full_name;
	char* char_name = rj->CharName();
	if(char_name)
	{
		marker_top_full_name = new char [strlen(marker_top_name) + strlen(char_name) + 2];
		sprintf(marker_top_full_name, "%s%c%s", marker_top_name, charname_separator, char_name);
	}
	else
	{
		marker_top_full_name = new char [strlen(marker_top_name) + 1];
		strcpy(marker_top_full_name, marker_top_name);
	}
	TransformNode* marker_top = sg->findTransformNode(marker_top_full_name);
	if(marker_top)
		load_markers(marker_top);
	delete[] marker_top_full_name;
	return 0;
}

int IK::load_markers(TransformNode* marker_top)
{
	// find first transform node
	Node* n;
	for(n=marker_top->getChildNodes(); n && !n->getName(); n=n->getChildNodes());
	if(!n) return 0;
	Node* tn;
	for(tn=n; tn; tn=tn->next())
	{
		if(tn->isTransformNode())
			add_marker((TransformNode*)tn);
	}
	return 0;
}

IKHandle* IK::add_marker(TransformNode* tn)
{
	// analyze name {marker name}_{link name}:{character name}
	char* fullname = new char [strlen(tn->getName()) + 1];
	strcpy(fullname, tn->getName());
	char* ch_start = strrchr(fullname, charname_separator);
	char* body_name;
	if(ch_start)
	{
		body_name = new char [ch_start - fullname + 1];
		strncpy(body_name, fullname, ch_start-fullname);
		body_name[ch_start-fullname] = '\0';
	}
	else  // no character name
	{
		body_name = new char [strlen(fullname) + 1];
		strcpy(body_name, fullname);
	}
	char* j_start = strrchr(body_name, joint_name_separator);
	if(!j_start)
	{
		delete[] body_name;
		delete[] fullname;
		return 0;
	}
	char* joint_name = new char [strlen(j_start)];
	strcpy(joint_name, j_start+1);

	char* marker_name;
	if(ch_start)
	{
		marker_name = new char [j_start - body_name + strlen(ch_start) + 1];
		strncpy(marker_name, body_name, j_start-body_name);
		marker_name[j_start-body_name] = charname_separator;
		marker_name[j_start-body_name+1] = '\0';
		strcat(marker_name, ch_start+1);
	}
	else
	{
		marker_name = new char [j_start - body_name + 1];
		strncpy(marker_name, body_name, j_start-body_name);
		marker_name[j_start-body_name] = '\0';
	}
	Joint* joint;
	if(ch_start)
		joint = FindJoint(joint_name, ch_start+1);
	else
		joint = FindJoint(joint_name);
	if(!joint)
	{
		delete[] body_name;
		delete[] fullname;
		delete[] joint_name;
		delete[] marker_name;
		return 0;
	}
	// create new fixed joint as a child of joint
	Joint* new_joint;
	if(ch_start)
	{
//		char* marker_joint_name = new char [strlen(body_name) + strlen(ch_start) + 1];
//		sprintf(marker_joint_name, "%s%s", body_name, ch_start);
		char* marker_joint_name = new char [strlen(marker_name) + 1];
		sprintf(marker_joint_name, "%s", marker_name);
		new_joint = new Joint(marker_joint_name, JFIXED);
		delete[] marker_joint_name;
	}
	else
	{
		new_joint = new Joint(body_name, JFIXED);
	}
	if(AddJoint(new_joint, joint))
	{
		delete[] body_name;
		delete[] fullname;
		delete[] joint_name;
		delete[] marker_name;
		delete new_joint;
		return 0;
	}
	// set relative position
	float fpos[3], fatt[3][3], fscale[3];
	fVec3 abs_pos;
	fMat33 abs_att;
	get_abs_matrix(tn, fpos, fatt, fscale);
	abs_pos(0) = fpos[0];
	abs_pos(1) = fpos[1];
	abs_pos(2) = fpos[2];
	abs_att(0,0) = fatt[0][0];
	abs_att(0,1) = fatt[0][1];
	abs_att(0,2) = fatt[0][2];
	abs_att(1,0) = fatt[1][0];
	abs_att(1,1) = fatt[1][1];
	abs_att(1,2) = fatt[1][2];
	abs_att(2,0) = fatt[2][0];
	abs_att(2,1) = fatt[2][1];
	abs_att(2,2) = fatt[2][2];
	set_abs_position_orientation(new_joint, abs_pos, abs_att);
	// add handle constraint
	ConstIndex cindex[6] = {
		HAVE_CONSTRAINT,
		HAVE_CONSTRAINT,
		HAVE_CONSTRAINT,
		NO_CONSTRAINT,
		NO_CONSTRAINT,
		NO_CONSTRAINT,
	};
	IKHandle* h = new IKHandle(this, marker_name, new_joint, cindex, LOW_PRIORITY, 10.0);
	AddConstraint(h);
	cerr << "marker " << marker_name << " added to " << joint->name << " (abs_pos = " << abs_pos << ", rel_pos = " << new_joint->rel_pos << ")" << endl;
//	cerr << "   joint pos = " << joint->abs_pos << endl;
	delete[] body_name;
	delete[] fullname;
	delete[] joint_name;
	delete[] marker_name;
	return h;
}

// global version
IKHandle* IK::AddMarker(const std::string& label, const std::string& linkname, const std::string& charname, const fVec3& rel_pos)
{
	Joint* pjoint = FindJoint(linkname.c_str(), charname.c_str());
	if(!pjoint) return 0;
	std::string fullname = label + charname_separator + charname;
	Joint* new_joint = new Joint(fullname.c_str(), JFIXED);
	if(AddJoint(new_joint, pjoint))
	{
		delete new_joint;
		return 0;
	}
	new_joint->rel_pos.set(rel_pos);
	// add handle constraint
	ConstIndex cindex[6] = {
		HAVE_CONSTRAINT,
		HAVE_CONSTRAINT,
		HAVE_CONSTRAINT,
		NO_CONSTRAINT,
		NO_CONSTRAINT,
		NO_CONSTRAINT,
	};
	IKHandle* h = new IKHandle(this, fullname.c_str(), new_joint, cindex, LOW_PRIORITY, 10.0);
	AddConstraint(h);
	cerr << "marker " << fullname << " added " << new_joint->rel_pos << endl;
	return h;
}

IKHandle* IK::AddMarker(const char* marker_name, Joint* parent_joint, const fVec3& abs_pos)
{
	IKHandle* ret;
	in_create_chain = true;
	clear_data();
	ret = add_marker(marker_name, parent_joint, abs_pos);
	init(0);
	in_create_chain = false;
	return ret;
}

IKHandle* IK::add_marker(const char* marker_name, Joint* parent_joint, const fVec3& abs_pos)
{
	char* fullname = new char [strlen(marker_name) + strlen(parent_joint->name) + 2];
	sprintf(fullname, "%s%c%s", marker_name, joint_name_separator, parent_joint->name);
	TransformNode* tnode = new TransformNode;
	tnode->setName(fullname);
	tnode->setTranslation(abs_pos(0), abs_pos(1), abs_pos(2));
	IKHandle* ret = add_marker(tnode);
	delete[] fullname;
	delete tnode;
	return ret;
}

int IK::EditMarker(IKHandle* marker, const char* body_name, Joint* parent_joint, const fVec3& abs_pos)
{
	int ret;
	in_create_chain = true;
	clear_data();
	ret = edit_marker(marker, body_name, parent_joint, abs_pos);
	init(0);
	in_create_chain = false;
	return ret;
}

int IK::edit_marker(IKHandle* marker, const char* body_name, Joint* parent_joint, const fVec3& abs_pos)
{
	// change name
	char* ch_start = strrchr(parent_joint->name, charname_separator);
	if(marker->joint_name) delete[] marker->joint_name;
	marker->joint_name = 0;
	if(ch_start)
	{
		marker->joint_name = new char [strlen(body_name) + strlen(ch_start) + 1];
		sprintf(marker->joint_name, "%s%s", body_name, ch_start);
		cerr << "new name = " << marker->joint_name << endl;
	}
	else
	{
		marker->joint_name = new char [strlen(body_name) + 1];
		strcpy(marker->joint_name, body_name);
	}
	marker->joint->SetName(marker->joint_name);
	// set parent joint
	if(marker->joint->parent == parent_joint) return 0;
	// disconnect and connect
	cerr << "fixed to " << parent_joint->name << endl;
	RemoveJoint(marker->joint);
	AddJoint(marker->joint, parent_joint);
	fMat33 abs_att;
	abs_att.identity();
	set_abs_position_orientation(marker->joint, abs_pos, abs_att);
	return 0;
}

int IK::SaveMarkers(const char* _fname)
{
	CalcPosition();
	SceneGraph* sg = new SceneGraph;
	int i;
	TransformNode* top_tnode = new TransformNode;
	top_tnode->setName(marker_top_name);
	sg->addNode(top_tnode);
	for(i=0; i<n_constraints; i++)
	{
		if(constraints[i]->GetType() == HANDLE_CONSTRAINT)
			save_marker(top_tnode, (IKHandle*)constraints[i]);
	}
	char* fname = new char [strlen(_fname) + 1];
	strcpy(fname, _fname);
	sg->save(fname);
	delete sg;
	delete[] fname;
	return 0;
}

int IK::save_marker(TransformNode* top_tnode, IKHandle* h)
{
	char* char_name = h->joint->CharName();
	char* t_name;
	if(char_name)
	{
		int marker_name_length = strlen(h->joint_name) - strlen(char_name) - 1;
		int parent_name_length = strlen(h->joint->parent->name) - strlen(char_name) - 1;
		char* marker_name = new char [marker_name_length + 1];
		char* parent_name = new char [parent_name_length + 1];
		t_name = new char [marker_name_length + parent_name_length + 2];
		strncpy(marker_name, h->joint_name, marker_name_length);
		marker_name[marker_name_length] = '\0';
		strncpy(parent_name, h->joint->parent->name, parent_name_length);
		parent_name[parent_name_length] = '\0';
		sprintf(t_name, "%s%c%s", marker_name, joint_name_separator, parent_name);
		delete[] marker_name;
		delete[] parent_name;
	}
	else
	{
		t_name = new char [strlen(h->joint_name) + strlen(h->joint->parent->name) + 2];
		sprintf(t_name, "%s%c%s", h->joint_name, joint_name_separator, h->joint->parent->name);
	}
	CalcPosition();
	TransformNode* tnode = new TransformNode();
	tnode->setName(t_name);
	tnode->setTranslation(h->joint->abs_pos(0), h->joint->abs_pos(1), h->joint->abs_pos(2));
	top_tnode->addChildNode(tnode);
	delete[] t_name;
	return 0;
}

#endif

int IK::NumConstraints(ConstType t)
{
	int count = 0;
	for(int i=0; i<n_constraints; i++)
	{
		if(constraints[i]->GetType() == t) count++;
	}
	return count;
}

int IK::assign_constraints(int _n)
{
	cerr << "assign_constraints(" << _n << ")" << endl;
	int i;
	IKConstraint** save = 0;
	if(constraints && n_assigned_constraints > 0)
	{
		save = new IKConstraint* [n_assigned_constraints];
		for(i=0; i<n_assigned_constraints; i++)
			save[i] = constraints[i];
		delete[] constraints;
	}
	constraints = 0;
	if(_n > 0)
	{
		constraints = new IKConstraint* [_n];
		for(i=0; i<_n; i++) constraints[i] = 0;
		for(i=0; i<n_assigned_constraints; i++)
			constraints[i] = save[i];
	}
	n_assigned_constraints = _n;
	if(save) delete[] save;
	cerr << "<-" << endl;
	return 0;
}

int IK::AddConstraint(IKConstraint* _constraint)
{
//	if(!_constraint->joint) return -1;
	if(n_constraints >= n_assigned_constraints)
		assign_constraints(n_assigned_constraints*2);
	int id = n_total_const++;
	constraints[n_constraints++] = _constraint;
	_constraint->id = id;
	return id;
}

int IK::RemoveConstraint(int _id)
{
	int index = ConstraintIndex(_id);
	if(index < 0) return -1;
	if(constraints[index]) delete constraints[index];
	constraints[index] = 0;
	int i;
	for(i=index+1; i<n_constraints; i++)
	{
		constraints[i-1] = constraints[i];
	}
	constraints[--n_constraints] = 0;
	return 0;
}

int IK::RemoveAllConstraints()
{
	for(int i=0; i<n_constraints; i++)
	{
		if(constraints[i]) delete constraints[i];
		constraints[i] = 0;
	}
	n_constraints = 0;
	return 0;
}

IKConstraint* IK::FindConstraint(ConstType _type, const char* jname, const char* charname)
{
  static char fullname[256];
  if(charname) {
	sprintf(fullname, "%s%c%s", jname, charname_separator, charname);
  }
  else {
	strcpy(fullname, jname);
  }
	for(int i=0; i<n_constraints; i++)
	{
		if(constraints[i]->GetType() == _type &&
		   !strcmp(fullname, constraints[i]->joint_name))
			return constraints[i];
	}
	return 0;
}

IKConstraint* IK::FindConstraint(int _id)
{
	for(int i=0; i<n_constraints; i++)
	{
		if(constraints[i]->id == _id)
			return constraints[i];
	}
	return 0;
}

int IK::ConstraintID(ConstType _type, const char* jname)
{
	for(int i=0; i<n_constraints; i++)
	{
		if(constraints[i]->GetType() == _type &&
		   !strcmp(jname, constraints[i]->joint_name))
			return constraints[i]->id;
	}
	return -1;
}

int IK::ConstraintID(int _index)
{
	if(_index < 0 || _index >= n_constraints)
		return -1;
	return constraints[_index]->id;
}

int IK::ConstraintIndex(ConstType _type, const char* jname)
{
	for(int i=0; i<n_constraints; i++)
	{
		if(constraints[i]->GetType() == _type &&
		   !strcmp(jname, constraints[i]->joint_name))
			return i;
	}
	return -1;
}

int IK::ConstraintIndex(int _id)
{
	for(int i=0; i<n_constraints; i++)
	{
		if(constraints[i]->id == _id)
			return i;
	}
	return -1;
}

int IK::ResetAllConstraints()
{
	int i;
	for(i=0; i<n_constraints; i++)
		constraints[i]->Reset();
	return 0;
}

int IK::ResetConstraints(ConstType t)
{
	int i;
	for(i=0; i<n_constraints; i++)
	{
		if(constraints[i]->GetType() == t)
			constraints[i]->Reset();
	}
	return 0;
}

int IK::SetJointWeight(const char* jname, double _weight)
{
	double weight = _weight;
	if(weight < MIN_JOINT_WEIGHT) weight = MIN_JOINT_WEIGHT;
	Joint* jnt = FindJoint(jname);
	if(!jnt) return -1;
	int i;
	for(i=0; i<jnt->n_dof; i++)
	{
		joint_weights(i+jnt->i_dof) = weight;
	}
	return 0;
}

int IK::SetJointWeight(const char* jname, const fVec& _weight)
{
	Joint* jnt = FindJoint(jname);
	if(!jnt) return -1;
	int i;
	for(i=0; i<jnt->n_dof && i<_weight.size(); i++)
	{
		if(_weight(i) >= MIN_JOINT_WEIGHT)
			joint_weights(i+jnt->i_dof) = _weight(i);
	}
	return 0;
}

int IK::SetDesireGain(const char* jname, double _gain)
{
	if(_gain < 0.0) return -1;
	IKDesire* des = (IKDesire*)FindConstraint(DESIRE_CONSTRAINT, jname);
	if(!des) return -1;
	des->gain = _gain;
	return 0;
}

int IK::EnableDesire(const char* jname)
{
	IKDesire* des = (IKDesire*)FindConstraint(DESIRE_CONSTRAINT, jname);
	if(!des) return -1;
	des->Enable();
	cerr << "desire constraint at " << jname << " enabled" << endl;
	return 0;
}

int IK::DisableDesire(const char* jname)
{
	IKDesire* des = (IKDesire*)FindConstraint(DESIRE_CONSTRAINT, jname);
	if(!des) return -1;
	des->Disable();
	cerr << "desire constraint at " << jname << " disabled" << endl;
	return 0;
}

void IK::SetCharacterScale(double _scale, const char* charname)
{
	SetConstraintScale(_scale, charname);
	// set joint weights
	joint_weights.resize(n_dof);
	joint_weights = 1.0;
	set_character_scale(root, _scale, charname);
}

void IK::SetConstraintScale(double _scale, const char* charname)
{
	int i;
	for(i=0; i<n_constraints; i++)
	{
		constraints[i]->SetCharacterScale(_scale, charname);
	}
}

void IK::set_character_scale(Joint* jnt, double _scale, const char* charname)
{
	if(!jnt) return;
	if((!charname || strstr(jnt->name, charname)) && jnt->n_dof > 0)
	{
		// rotation -> s
		switch(jnt->j_type)
		{
		case JROTATE:
			joint_weights(jnt->i_dof) = _scale;
			break;
		case JSPHERE:
			joint_weights(jnt->i_dof) = _scale;
			joint_weights(jnt->i_dof+1) = _scale;
			joint_weights(jnt->i_dof+2) = _scale;
			break;
		case JFREE:
			joint_weights(jnt->i_dof+3) = _scale;
			joint_weights(jnt->i_dof+4) = _scale;
			joint_weights(jnt->i_dof+5) = _scale;
			break;
		default:
			break;
		}
	}
	set_character_scale(jnt->brother, _scale, charname);
	set_character_scale(jnt->child, _scale, charname);
}

void IKHandle::SetCharacterScale(double _scale, const char* charname)
{
	if(!joint) return;
	if(n_const == 0) return;
	if(charname && !strstr(joint->name, charname)) return;  // not a strict check
	int i;
	weight.resize(n_const);
	weight = 1.0;
	int count = 0;
//	double s1 = 1.0/_scale;
	double s1 = _scale;
	for(i=0; i<6; i++)
	{
		if(const_index[i] == IK::HAVE_CONSTRAINT)
		{
			// rotation -> 1/s
			if(i >= 3) weight(count) = s1;
			count++;
		}
	}
}

void IKDesire::SetCharacterScale(double _scale, const char* charname)
{
	if(!joint) return;
	if(n_const == 0) return;
	if(charname && !strstr(joint->name, charname)) return;  // not a strict check
	weight.resize(n_const);
	if(_scale < 1.0)
		weight = _scale;
	else
		weight = 1.0;
#if 0
//	double s1 = 1.0/_scale;
	double s1 = _scale;
	// rotation -> 1/s
	switch(joint->j_type)
	{
	case JROTATE:
		weight(0) = s1;
		break;
	case JSPHERE:
		weight(0) = s1;
		weight(1) = s1;
		weight(2) = s1;
		break;
	case JFREE:
		weight(3) = s1;
		weight(4) = s1;
		weight(5) = s1;
		break;
	default:
		break;
	}
#endif
}
