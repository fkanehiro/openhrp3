/*
 * edit.cpp
 * Create: Katsu Yamane, Univ. of Tokyo, 03.06.18
 */

#include "chain.h"

/*
 * create chain
 */
int Chain::BeginCreateChain(int append)
{
	if(in_create_chain)
	{
		cerr << "Chain::BeginCreateChain - called after BeginCreateChain()" << endl;
		return -1;
	}
	if(!append) Clear();
	in_create_chain = true;
	clear_data();
	return 0;
}

int Chain::RemoveJoint(Joint* j)
{
	if(!in_create_chain)
	{
		cerr << "Chain::RemoveJoint - attempted to edit the chain before calling BeginCreateChain" << endl;
		return -1;
	}
	if(!j->parent) return -1;
	return j->parent->remove_child(j);
}

int Joint::remove_child(Joint* j)
{
	Joint* prev = 0;
	j->parent = 0;
	if(child == j)
	{
		child = j->brother;
		j->brother = 0;
		return 0;
	}
	for(prev=child; prev && prev->brother!=j; prev=prev->brother);
	if(!prev) return -1;
	prev->brother = j->brother;
	j->brother = 0;
	return 0;
}

Joint* Chain::AddRoot(const char* name, const fVec3& grav)
{
	if(!in_create_chain)
	{
		cerr << "Chain::AddRoot - attempted to add a root before calling BeginCreateChain" << endl;
		return NULL;
	}
	if(root)
	{
		cerr << "Chain::AddRoot - root is already set" << endl;
		return NULL;
	}
	Joint* r;
	if(name)
	{
		r = new Joint(name, JFIXED);
	}
	else
	{
		r = new Joint("space", JFIXED);
	}
	r->loc_lin_acc.set(grav);
	r->chain = this;
	root = r;
	return r;
}

int Chain::AddRoot(Joint* r)
{
	if(!in_create_chain)
	{
		cerr << "Chain::AddRoot - attempted to add a root before calling BeginCreateChain" << endl;
		return -1;
	}
	if(root)
	{
		cerr << "Chain::AddRoot - root is already set" << endl;
		return -1;
	}
	r->chain = this;
	r->j_type = JFIXED;
	root = r;
	return 0;
}

int Chain::AddJoint(Joint* target, const char* parent_name, const char* charname)
{
	Joint* p = FindJoint(parent_name, charname);
	if(!strcmp(parent_name, root->name)) p = root;
	if(!p)
	{
		cerr << "Chain::AddJoint - parent " << parent_name << " not found for " << target->name << endl;
		return -1;
	}
	return AddJoint(target, p);
}

int Chain::AddJoint(Joint* target, Joint* p)
{
	if(!in_create_chain)
	{
		cerr << "Chain::AddJoint - attempted to add a joint before calling BeginCreateChain" << endl;
		return -1;
	}
	if(!root)
	{
		cerr << "Chain::AddJoint - root is not set" << endl;
		return -1;
	}
	if(!p)
	{
		cerr << "Chain::AddJoint - parent is NULL" << endl;
		return -1;
	}
	if(!p->chain)
	{
		cerr << "Joint::AddJoint - parent joint is not added yet" << endl;
		return -1;
	}
	if(FindJoint(target->name))
	{
		cerr << "Joint::AddJoint - joint " << target->name << " already exists" << endl;
		return -1;
	}
	if(!target) return 0;
	target->chain = this;
	if(p)
	{
		p->add_child(target);
	}
	else
	{
		root = target;
	}
	return 0;
}

void Joint::add_child(Joint* c)
{
	c->parent = this;
	c->brother = child;
	child = c;
}

Joint* Chain::AddJoint(JointData* joint_data, const char* charname)
{
	Joint* j = new Joint(joint_data, charname);
	if(AddJoint(j, joint_data->parent_name, charname)) return 0;
	return j;
}

#if 0
int Chain::CreateSerial(int num_joint, const JointData& joint_data,
						const char* charname, Joint* parent_joint)
{
	if(!joint_data.name) return -1;
	if(!root) AddRoot();
	Joint* last_joint = root;
	if(parent_joint) last_joint = parent_joint;
	int i;
	for(i=0; i<num_joint; i++)
	{
		JointData* jdata = new JointData(joint_data);
		jdata->name = new char [strlen(joint_data.name) + 5];
		sprintf(jdata->name, "%s%04d", joint_data.name, i+1);
		jdata->parent_name = new char [strlen(last_joint->basename) + 1];
		strcpy(jdata->parent_name, last_joint->basename);
		Joint* new_joint = AddJoint(jdata, charname);
		cerr << new_joint->name << " added to " << last_joint->basename << endl;
		last_joint = new_joint;
		delete jdata;
	}
	return 0;
}

int Chain::CreateParallel(int num_char, const char* prmname, const char* charname_base, const fVec3& init_pos, const fMat33& init_att, const fVec3& pos_offset, const fMat33& att_offset, int init_num)
{
	int i;
	fVec3 cur_pos_offset(init_pos);
	fMat33 cur_att_offset(init_att);
	for(i=0; i<num_char; i++)
	{
		char* charname = new char [strlen(charname_base) + 5];
		sprintf(charname, "%s%04d", charname_base, i+init_num);
		Load(prmname, charname);
		Joint* char_root = FindCharacterRoot(charname);
		fVec3 tmp, tmpp;
		fMat33 tmpr;
		tmp.mul(cur_att_offset, pos_offset);
		cur_pos_offset += tmp;
		tmpr.mul(cur_att_offset, att_offset);
		cur_att_offset.set(tmpr);
		tmpp.add(char_root->rel_pos, cur_pos_offset);
		tmpr.mul(char_root->rel_att, cur_att_offset);
		char_root->SetJointValue(tmpp, tmpr);
		cerr << charname << ": " << char_root->rel_pos << endl << char_root->rel_att << endl;
		delete[] charname;
	}
	return 0;
}
#endif

#ifdef SEGA
int Chain::EndCreateChain()
#else
int Chain::EndCreateChain(SceneGraph* sg)
#endif
{
	if(!in_create_chain)
	{
		cerr << "Chain::EndCreateChain - called before BeginCreateChain()" << endl;
		return -1;
	}
#ifdef SEGA
	init();
#else
	init(sg);
#endif
	in_create_chain = false;
	return 0;
}

#ifndef SEGA
void Chain::set_relative_positions(SceneGraph* sg)
{
	root->abs_pos.zero();
	root->abs_att.identity();
	calc_abs_positions(root, sg);
	calc_rel_positions(root, sg);
}

void Chain::calc_abs_positions(Joint* cur, SceneGraph* sg)
{
	if(!cur) return;
	TransformNode* tnode = sg->findTransformNode(cur->name);
//	cerr << "---- calc_abs_positions: " << cur->name << endl;
	if(cur->parent && tnode)
	{
		float abs_pos[3], abs_att[3][3], abs_scale[3];
		get_abs_matrix(tnode, abs_pos, abs_att, abs_scale);
		cur->abs_pos(0) = abs_pos[0];
		cur->abs_pos(1) = abs_pos[1];
		cur->abs_pos(2) = abs_pos[2];
		cur->abs_att(0,0) = abs_att[0][0];
		cur->abs_att(0,1) = abs_att[0][1];
		cur->abs_att(0,2) = abs_att[0][2];
		cur->abs_att(1,0) = abs_att[1][0];
		cur->abs_att(1,1) = abs_att[1][1];
		cur->abs_att(1,2) = abs_att[1][2];
		cur->abs_att(2,0) = abs_att[2][0];
		cur->abs_att(2,1) = abs_att[2][1];
		cur->abs_att(2,2) = abs_att[2][2];
//		cerr << cur->abs_pos << endl;
//		cerr << cur->abs_att << endl;
	}
	calc_abs_positions(cur->brother, sg);
	calc_abs_positions(cur->child, sg);
}

void Chain::calc_rel_positions(Joint* cur, SceneGraph* sg)
{
	if(!cur) return;
	TransformNode* tnode = sg->findTransformNode(cur->name);
//	cerr << "---- calc_rel_positions: " << cur->name << endl;
	if(cur->parent && tnode)
	{
		static fVec3 pp, rel_pos;
		static fMat33 tr, rel_att;
		pp.sub(cur->abs_pos, cur->parent->abs_pos);
		tr.tran(cur->parent->abs_att);
		rel_pos.mul(tr, pp);
		rel_att.mul(tr, cur->abs_att);
		// set rel_pos, rel_att, rel_ep
		cur->rel_pos.set(rel_pos);
		cur->rel_att.set(rel_att);
		cur->rel_ep.set(rel_att);
		// set init_pos and init_att
		cur->init_pos.set(rel_pos);
		cur->init_att.set(rel_att);
//		cerr << "--- " << cur->name << endl;
//		cerr << rel_pos << endl;
//		cerr << rel_att << endl;
	}
	calc_rel_positions(cur->brother, sg);
	calc_rel_positions(cur->child, sg);
}
#endif
