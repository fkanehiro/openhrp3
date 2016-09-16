/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/**
 * schedule.cpp
 * Create: Katsu Yamane, 06.06.08
 */

#include "psim.h"
#include <dp.h>

#define TINY_MASS 1.0e-16
#define N_2_COEF 1.6 // alpha
#define N_COEF 1.0 // beta
#define DOF_COEF -1.0 // gamma
#define CONST_COEF 14.4 // delta

static pSim* sim = 0;
static int max_procs = 0;

#include <fstream>
ofstream logfile("schedule.log");
//static ofstream logfile;

// special handling of nodes with only one process
// 1. cost of the node: the cost to assemble all internal joints (constuctor)
// 2. assigning schedule children: no schedule children will be assigned (find_available_parent)
// 3. A* cost: always zero (calc_astar_cost)
// 4. converting the node to schedule: add all joints
class dpScheduleNode
	: public dpNode
{
public:
	dpScheduleNode(dpScheduleNode* potential_parent,
				   Joint* _last_joint, const fVec& _proc_costs,
				   dpScheduleNode* _schedule_parent, int child_id,
				   int _first_proc, int _last_proc,
				   const joint_list& org_internal_joints,
				   const p_joint_list& org_outer_joints): dpNode() {
		last_joint = _last_joint;
		if(last_joint)
		{
			sim->GetPJoint(last_joint, last_pjoints);
		}
		else
		{
			last_pjoints[0] = 0;
			last_pjoints[1] = 0;
		}
		schedule_parent = _schedule_parent;
		first_proc = _first_proc;
		last_proc = _last_proc;
		if(schedule_parent)
		{
			schedule_depth = schedule_parent->schedule_depth + 1;
			if(schedule_parent->last_joint)
			{
				set_outer_joints(schedule_parent->last_pjoints[child_id], org_outer_joints, outer_joints);
			}
		}
		else
		{
			schedule_depth = -1;
			for(p_joint_list::const_iterator j=org_outer_joints.begin(); j!=org_outer_joints.end(); j++)
			{
				outer_joints.push_back(*j);
			}
		}
		single_joint_cost = 0.0;
		proc_costs.resize(max_procs);
		proc_costs.set(_proc_costs);
		mycost = 0.0;
		if(last_joint)
		{
			logfile << "new node: " << last_joint->name << " (schedule depth = " << schedule_depth << flush;
			logfile << ", procs = [" << first_proc << ", " << last_proc << "]" << flush;
			if(schedule_parent && schedule_parent->last_joint)
				logfile << ", schedule_parent = " << schedule_parent->last_joint->name << ")" << endl;
			else
				logfile << ")" << endl;
			// split org_internal_joints by last_joint and find outer_joints 
			split_internal_joints(last_joint, org_internal_joints, internal_joints[0], internal_joints[1]);
			// compute costs
			if(last_proc - first_proc == 1)
			{
				joint_list added_joints;
				single_joint_cost = calc_min_subchain_cost(outer_joints, added_joints);
			}
			else
			{
				single_joint_cost = calc_single_joint_cost(last_joint->n_dof, outer_joints.size());
			}
		}
		else
		{
			for(joint_list::const_iterator i=org_internal_joints.begin(); i!=org_internal_joints.end(); i++)
			{
				internal_joints[0].push_back(*i);
			}
		}
		logfile << "  single_joint_cost = " << single_joint_cost << endl;
		for(int i=first_proc; i<last_proc; i++)
		{
			proc_costs(i) += single_joint_cost;
		}
		mycost = proc_costs.max_value();
		if(potential_parent)
		{
			mycost -= potential_parent->proc_costs.max_value();
		}
		logfile << "  proc_costs = " << tran(proc_costs) << endl;
		logfile << "  mycost = " << mycost << endl;
	}
	~dpScheduleNode() {
	}

	fVec& ProcCosts() {
		return proc_costs;
	}
	
	Joint* LastJoint() {
		return last_joint;
	}
	int ScheduleDepth() {
		return schedule_depth;
	}
	int FirstProc() {
		return first_proc;
	}
	int LastProc() {
		return last_proc;
	}
	int ListAllInternalJoints(joint_list& all_internal);
	
protected:
	int list_all_internal_joints_sub(Joint* cur, joint_list& all_internal);
	int list_all_internal_joints_rev(Joint* cur, joint_list& all_internal);
	int split_internal_joints(Joint* _last_joint, const joint_list& org_internal_joints, joint_list& _internal_joints_0, joint_list& _internal_joints_1);
	int set_outer_joints(pJoint* last_pjoint, const p_joint_list& org_outer_joints, p_joint_list& new_outer_joints);

	dpScheduleNode* find_available_parent(int target_depth, int& child_id);
	dpScheduleNode* find_available_parent_sub(dpScheduleNode* cur, int target_depth, dpNode* cur_leaf, int& child_id);
	// functions that should be defined in the subclass
	// create and add child nodes
	int create_child_nodes(dpNode**& nodes) {
		if(is_goal()) return 0;
		logfile << "create_child_node [" << TotalAstarCost() << "] (joints = " << flush;
		dpNode* n;
		for(n=this; n; n=n->Parent())
		{
			dpScheduleNode* s = (dpScheduleNode*)n;
			if(s->last_joint)
				logfile << "[" << s->last_joint->name << "/" << s->schedule_depth << "]" << flush;
		}
		logfile << ")" << endl;
		// last joint
		if(depth == sim->NumJoint()-1)
		{
			logfile << "create goal (0)" << endl;
			nodes = new dpNode* [1];
			nodes[0] = new dpScheduleNode(this, 0, proc_costs, this, 0, first_proc, last_proc, joint_list(), p_joint_list());
			return 1;
		}
		// find the schedule_parent to add
		dpScheduleNode* target_parent = 0;
		int child_id = -1;
		target_parent = find_available_parent(schedule_depth-1, child_id);
		if(!target_parent)
		{
			// try next depth
			target_parent = find_available_parent(schedule_depth, child_id);
			// no parent to add: add a goal
			if(!target_parent)
			{
				logfile << "create goal (1)" << endl;
				nodes = new dpNode* [1];
				nodes[0] = new dpScheduleNode(this, 0, proc_costs, this, 0, first_proc, last_proc, joint_list(), p_joint_list());
				return 1;
			}
		}
		///// create child nodes
		if(target_parent->last_joint)
		{
			logfile << "target_parent = " << target_parent->last_joint->name << ", schedule_depth = " << schedule_depth << ", child_id = " << child_id << endl;
		}
		else
		{
			logfile << "target_parent = null, schedule_depth = " << schedule_depth << ", child_id = " << child_id << endl;
		}
		int _first_proc = 0, _last_proc = max_procs;
		if(child_id == 0)
		{
			if(target_parent->internal_joints[1].size() > 0)
			{
				_first_proc = target_parent->first_proc;
				_last_proc = target_parent->first_proc + (target_parent->last_proc-target_parent->first_proc)/2;
			}
			else
			{
				_first_proc = target_parent->first_proc;
				_last_proc = target_parent->last_proc;
			}
		}
		else
		{
			if(target_parent->internal_joints[0].size() > 0)
			{
				_first_proc = target_parent->first_proc + (target_parent->last_proc-target_parent->first_proc)/2;
				_last_proc = target_parent->last_proc;
			}
			else
			{
				_first_proc = target_parent->first_proc;
				_last_proc = target_parent->last_proc;
			}
		}
		joint_list& internal_joints = target_parent->internal_joints[child_id];
		// only one process available: apply default schedule
		if(_last_proc - _first_proc == 1)
		{
			nodes = new dpNode* [1];
			// set child's last_joint
			Joint* next_last = 0;
			if(target_parent->last_joint)
			{
				joint_list& in_joints = target_parent->internal_joints[child_id];
				for(joint_list::iterator i=in_joints.begin(); i!=in_joints.end(); i++)
				{
					if(*i == target_parent->last_joint->parent ||
					   (*i)->parent == target_parent->last_joint ||
					   (*i)->parent == target_parent->last_joint->parent)
					{
						next_last = *i;
						break;
					}
				}
			}
			else
			{
				next_last = sim->Root();
			}
			dpScheduleNode* n = new dpScheduleNode(this, next_last, proc_costs, target_parent, child_id, _first_proc, _last_proc, internal_joints, target_parent->outer_joints);
			nodes[0] = (dpNode*)n;
			return 1;
		}
		// more than two processors available
		int n_internal_joints = internal_joints.size();
		nodes = new dpNode* [n_internal_joints];
		joint_list::iterator j;
		int i;
		for(i=0, j=internal_joints.begin(); j!=internal_joints.end(); i++, j++)
		{
			dpScheduleNode* n = new dpScheduleNode(this, *j, proc_costs, target_parent, child_id, _first_proc, _last_proc, internal_joints, target_parent->outer_joints);
#if 1  // always generate two non-empty subchains
			if(n->internal_joints[0].size() == 0 ||
			   n->internal_joints[1].size() == 0)
			{
				delete n;
				nodes[i] = 0;
			}
			else
#endif
			{
				nodes[i] = (dpNode*)n;
			}
		}
		return n_internal_joints;
	}
	// compute (local) cost
	double calc_cost() {
		return mycost;
	}
	// (optional) compute A-star cost
	double calc_astar_cost(dpNode* potential_parent) {
		double c = 0.0;
		logfile << "calc_astar_cost(" << flush;
		if(last_joint)
		{
			logfile << last_joint->name << "/" << schedule_depth << ")" << endl;
			p_joint_list new_outer_joints;
			if(last_proc - first_proc == 1)
			{
#if 0
				double tmp = calc_min_subchain_cost(outer_joints);
				logfile << "  one process: " << tmp << endl;
				if(tmp > c) c = tmp;
#endif
			}
			else
			{
				// child 0
				if(internal_joints[0].size() > 0)
				{
					int n_myprocs = last_proc - first_proc;
					joint_list added_joints;
					if(internal_joints[1].size() > 0)
					{
						n_myprocs = (last_proc - first_proc)/2;
					}
					set_outer_joints(last_pjoints[0], outer_joints, new_outer_joints);
					double tmp = calc_min_subchain_cost(new_outer_joints, added_joints)/n_myprocs;
					logfile << "  child 0: " << tmp << " [" << n_myprocs << "]" << endl;
					if(tmp > c) c = tmp;
				}
				// child 1
				if(internal_joints[1].size() > 0)
				{
					joint_list added_joints;
					int n_myprocs = last_proc - first_proc;
					if(internal_joints[0].size() > 0)
					{
						n_myprocs -= (last_proc-first_proc)/2;
					}
					set_outer_joints(last_pjoints[1], outer_joints, new_outer_joints);
					double tmp = calc_min_subchain_cost(new_outer_joints, added_joints)/n_myprocs;
					logfile << "  child 1: " << tmp << " [" << n_myprocs << "]" << endl;
					if(tmp > c) c = tmp;
				}
			}
		}
		else
		{
			logfile << "default)" << endl;
		}
		logfile << " final = " << c << endl;
		return c;
	}
	// check if the states are same
	int same_state(dpNode* refnode) {
		return false;
	}
	// check if the state is goal
	int is_goal() {
		return ((!last_joint && depth > 0) || depth == sim->NumJoint());
	}

private:
	Joint* last_joint;
	pJoint* last_pjoints[2];

	int schedule_depth;
	dpScheduleNode* schedule_parent;
	p_joint_list outer_joints;
	joint_list internal_joints[2];

	double single_joint_cost;
	double mycost;
	fVec proc_costs;
	int first_proc, last_proc;

	double calc_single_joint_cost(int n_dof, int n_outer) {
//		return INVERSE_UNIT*(6-n_dof)*(6-n_dof)*(6-n_dof) + n_outer*n_outer;
		return N_2_COEF*n_outer*n_outer + N_COEF*n_outer + DOF_COEF*n_dof + CONST_COEF;
	}

	double calc_min_subchain_cost(const p_joint_list& _outer_joints, joint_list& added_joints);
	double calc_min_subchain_cost_sub(Joint* cur, const p_joint_list& _outer_joints, int* n_outer_parent_side, int* n_outer_child_side, joint_list& added_joints);

	double add_to_child_side(Joint* cur, joint_list& added_joints, int* n_outer_parent_side, int* n_outer_child_side);
	double add_to_parent_side(Joint* cur, joint_list& added_joints, int* n_outer_parent_side, int* n_outer_child_side);
};

int dpScheduleNode::ListAllInternalJoints(joint_list& all_internal)
{
#if 0
	all_internal.clear();
	if(!last_joint) return 0;
	if(last_proc - first_proc > 1)
	{
		all_internal.push_back(last_joint);
		return 0;
	}
	pJoint* top_pjoint = 0;
	// find the top joint
	for(p_joint_list::const_iterator j=outer_joints.begin(); j!=outer_joints.end(); j++)
	{
		if(!(*j)->ParentSide())
		{
			top_pjoint = *j;
			break;
		}
	}
	if(!top_pjoint)
	{
		list_all_internal_joints_rev(sim->Root(), all_internal);
	}
	else
	{
		list_all_internal_joints_sub(top_pjoint->GetJoint()->child, all_internal);
	}
#else
	if(last_proc - first_proc == 1)
	{
		calc_min_subchain_cost(outer_joints, all_internal);
	}
	else if(last_joint)
	{
		all_internal.push_back(last_joint);
	}
#endif
	return 0;
}

int dpScheduleNode::list_all_internal_joints_rev(Joint* cur, joint_list& all_internal)
{
	if(!cur) return 0;
	pJoint* pjoints[2];
	sim->GetPJoint(cur, pjoints);
	for(p_joint_list::const_iterator j=outer_joints.begin(); j!=outer_joints.end(); j++)
	{
		if(pjoints[1] == *j)
		{
			return 0;
		}
	}
	all_internal.push_back(cur);
	list_all_internal_joints_rev(cur->child, all_internal);
	list_all_internal_joints_rev(cur->brother, all_internal);
	return 0;
}

int dpScheduleNode::list_all_internal_joints_sub(Joint* cur, joint_list& all_internal)
{
	if(!cur) return 0;
	pJoint* pjoints[2];
	sim->GetPJoint(cur, pjoints);
	for(p_joint_list::const_iterator j=outer_joints.begin(); j!=outer_joints.end(); j++)
	{
		if(pjoints[1] == *j)
		{
			return 0;
		}
	}
	list_all_internal_joints_sub(cur->child, all_internal);
	list_all_internal_joints_sub(cur->brother, all_internal);
	all_internal.push_back(cur);
	return 0;
}

double dpScheduleNode::calc_min_subchain_cost(const p_joint_list& _outer_joints, joint_list& added_joints)
{
	pJoint* top_pjoint = 0;
	logfile << "=== calc_min_subchain_cost ===" << endl;
	logfile << "outer = [" << flush;
	// find the top joint
	for(p_joint_list::const_iterator j=_outer_joints.begin(); j!=_outer_joints.end(); j++)
	{
		logfile << " " << (*j)->GetJoint()->name << flush;
		if(!(*j)->ParentSide())
		{
			top_pjoint = *j;
//			break;
		}
	}
	logfile << "]" << endl;

	int* n_outer_parent_side = new int [sim->NumJoint()];
	int* n_outer_child_side = new int [sim->NumJoint()];
	for(int i=0; i<sim->NumJoint(); i++)
	{
		n_outer_parent_side[i] = 0;
		n_outer_child_side[i] = 0;
	}
	for(p_joint_list::const_iterator j=_outer_joints.begin(); j!=_outer_joints.end(); j++)
	{
		if((*j)->ParentSide())
		{
			Joint* cur = (*j)->GetJoint();
			for(Joint* p=cur->parent; p; p=p->parent)
			{
				n_outer_child_side[p->i_joint]++;
			}
		}
	}
	for(int i=0; i<sim->NumJoint(); i++)
	{
		n_outer_parent_side[i] = _outer_joints.size() - n_outer_child_side[i];
//		logfile << sim->FindJoint(i)->name << ": parent_side = " << n_outer_parent_side[i] << ", child_side = " << n_outer_child_side[i] << endl;
	}
	double ret = 0.0;
	added_joints.clear();
	if(!top_pjoint)
	{
		ret = calc_min_subchain_cost_sub(sim->Root(), _outer_joints, n_outer_parent_side, n_outer_child_side, added_joints);
	}
	else
	{
		joint_list sorted_child;
		for(Joint* c = top_pjoint->GetJoint()->child; c; c=c->brother)
		{
			int done = false;
			for(joint_list::iterator j=sorted_child.begin(); j!=sorted_child.end(); j++)
			{
				if(n_outer_child_side[c->i_joint] <= n_outer_child_side[(*j)->i_joint])
				{
					done = true;
					sorted_child.insert(j, c);
					break;
				}
			}
			if(!done) sorted_child.push_back(c);
		}
		for(joint_list::iterator j=sorted_child.begin(); j!=sorted_child.end(); j++)
		{
			ret += calc_min_subchain_cost_sub(*j, _outer_joints, n_outer_parent_side, n_outer_child_side, added_joints);
		}
	}
	delete[] n_outer_parent_side;
	delete[] n_outer_child_side;
	return ret;
}

int is_in(const joint_list& jlist, Joint* jnt)
{
	for(joint_list::const_iterator i=jlist.begin(); i!=jlist.end(); i++)
	{
		if(*i == jnt) return true;
	}
	return false;
}

double dpScheduleNode::calc_min_subchain_cost_sub(Joint* cur, const p_joint_list& _outer_joints, int* n_outer_parent_side, int* n_outer_child_side, joint_list& added_joints)
{
	if(!cur) return 0.0;
//	logfile << "calc_min_subchain_cost_sub(" << cur->name << ") ->" << endl;
	pJoint* pjoints[2];
	sim->GetPJoint(cur, pjoints);
	for(p_joint_list::const_iterator j=_outer_joints.begin(); j!=_outer_joints.end(); j++)
	{
		if(pjoints[1] == *j)
		{
			return 0.0;
		}
	}
	double ret = 0.0;
	int descend = false;
	int my_outer = n_outer_child_side[cur->i_joint];
	if(n_outer_parent_side[cur->i_joint] < n_outer_child_side[cur->i_joint])
	{
		descend = true;
		my_outer = n_outer_parent_side[cur->i_joint];
	}
	// if not in the descending order, this joint should be processed later
	if(!descend)
	{
		joint_list sorted_child;
		for(Joint* c = cur->child; c; c=c->brother)
		{
			int done = false;
			for(joint_list::iterator j=sorted_child.begin(); j!=sorted_child.end(); j++)
			{
				if(n_outer_child_side[c->i_joint] <= n_outer_child_side[(*j)->i_joint])
				{
					done = true;
					sorted_child.insert(j, c);
					break;
				}
			}
			if(!done) sorted_child.push_back(c);
		}
		for(joint_list::iterator j=sorted_child.begin(); j!=sorted_child.end(); j++)
		{
			ret += calc_min_subchain_cost_sub(*j, _outer_joints, n_outer_parent_side, n_outer_child_side, added_joints);
		}
		ret += add_to_parent_side(cur, added_joints, n_outer_parent_side, n_outer_child_side);
	}
	// if in descending order, sort the children in the descending order of
	// outer joints
	else
	{
		joint_list smaller_child, larger_child;
		for(Joint* c=cur->child; c; c=c->brother)
		{
			int n_child_outer = n_outer_child_side[c->i_joint];
			if(n_child_outer <= my_outer)
			{
				// add to smaller_child
				int done = false;
				for(joint_list::iterator i=smaller_child.begin(); i!=smaller_child.end(); i++)
				{
					int n = n_outer_child_side[(*i)->i_joint];
					if(n_child_outer <= n)
					{
						smaller_child.insert(i, c);
						done = true;
						break;
					}
				}
				if(!done)
				{
					smaller_child.push_back(c);
				}
			}
			else
			{
				// add to larger_child
				int done = false;
				for(joint_list::iterator i=larger_child.begin(); i!=larger_child.end(); i++)
				{
					int n = n_outer_child_side[(*i)->i_joint];
					if(n_child_outer <= n)
					{
						larger_child.insert(i, c);
						done = true;
						break;
					}
				}
				if(!done)
				{
					larger_child.push_back(c);
				}
			}
		}
//		logfile << "  smaller: " << endl;
		for(joint_list::iterator j=smaller_child.begin(); j!=smaller_child.end(); j++)
		{
//			logfile << "   " << (*j)->name << endl;
			ret += calc_min_subchain_cost_sub(*j, _outer_joints, n_outer_parent_side, n_outer_child_side, added_joints);
		}
		ret += add_to_child_side(cur, added_joints, n_outer_parent_side, n_outer_child_side);
//		logfile << "  larger: " << endl;
		for(joint_list::iterator j=larger_child.begin(); j!=larger_child.end(); j++)
		{
//			logfile << "   " << (*j)->name << endl;
			ret += calc_min_subchain_cost_sub(*j, _outer_joints, n_outer_parent_side, n_outer_child_side, added_joints);
		}
	}
//	logfile << "<- calc_min_subchain_cost_sub(" << cur->name << ")" << endl;
	return ret;
}

double dpScheduleNode::add_to_child_side(Joint* cur, joint_list& added_joints, int* n_outer_parent_side, int* n_outer_child_side)
{
	double ret = 0.0;
	int n_outer = n_outer_parent_side[cur->i_joint];
	int n_link_outer = 0;
	for(Joint* c=cur->child; c; c=c->brother)
	{
		if(!is_in(added_joints, c))
		{
			n_link_outer++;
		}
	}
	added_joints.push_back(cur);
	ret = calc_single_joint_cost(cur->n_dof, n_outer+n_link_outer);
	logfile << " adding " << cur->name << ", n_outer = " << n_outer << ", n_link_outer = " << n_link_outer << ", cost = " << ret << endl;
	return ret;
}

double dpScheduleNode::add_to_parent_side(Joint* cur, joint_list& added_joints, int* n_outer_parent_side, int* n_outer_child_side)
{
	double ret = 0.0;
	int n_outer = n_outer_child_side[cur->i_joint];
	int n_link_outer = 0;
	if(cur->parent && !is_in(added_joints, cur->parent))
	{
		n_link_outer++;
	}
	if(cur->parent)
	{
		for(Joint* c=cur->parent->child; c; c=c->brother)
		{
			if(!is_in(added_joints, c) && c!=cur)
			{
				n_link_outer++;
			}
		}
	}
	added_joints.push_back(cur);
	ret = calc_single_joint_cost(cur->n_dof, n_outer+n_link_outer);
	logfile << " adding " << cur->name << ", n_outer = " << n_outer << ", n_link_outer = " << n_link_outer << ", cost = " << ret << endl;
	return ret;
}

dpScheduleNode* dpScheduleNode::find_available_parent(int target_depth, int& child_id)
{
	dpScheduleNode* s, *ret = 0;
	for(s=this; s; s=(dpScheduleNode*)s->parent)
	{
	        if((ret = find_available_parent_sub(s, target_depth, this, child_id)))
			return ret;
	}
	return 0;
}

dpScheduleNode* dpScheduleNode::find_available_parent_sub(dpScheduleNode* cur, int target_depth, dpNode* cur_leaf, int& child_id)
{
	if(!cur) return 0;
	if(cur->last_proc - cur->first_proc == 1) return 0;
	if(cur->schedule_depth == target_depth)
	{
		if(cur->internal_joints[0].size() > 0)
		{
			int failed = false;
			joint_list::iterator f;
			for(f=cur->internal_joints[0].begin(); f!=cur->internal_joints[0].end(); f++)
			{
				dpNode* n;
				for(n=cur_leaf; n; n=n->Parent())
				{
					dpScheduleNode* s = (dpScheduleNode*)n;
					if(*f == s->last_joint)
					{
						failed = true;
						break;
					}
				}
				if(failed) break;
			}
			if(!failed)
			{
				child_id = 0;
				return cur;
			}
		}
		if(cur->internal_joints[1].size() > 0)
		{
			int failed = false;
			joint_list::iterator f;
			for(f=cur->internal_joints[1].begin(); f!=cur->internal_joints[1].end(); f++)
			{
				dpNode* n;
				for(n=cur_leaf; n; n=n->Parent())
				{
					dpScheduleNode* s = (dpScheduleNode*)n;
					if(*f == s->last_joint)
					{
						failed = true;
						break;
					}
				}
				if(failed) break;
			}
			if(!failed)
			{
				child_id = 1;
				return cur;
			}
		}
	}
	return 0;
}

static void get_all_joints(Joint* cur, joint_list& all_joints, joint_list& all_vjoints)
{
	if(!cur) return;
	if(cur->real)
	{
		all_vjoints.push_back(cur);
	}
	else
	{
		all_joints.push_back(cur);
	}
	get_all_joints(cur->child, all_joints, all_vjoints);
	get_all_joints(cur->brother, all_joints, all_vjoints);
	return;
}

int pSim::AutoSchedule(int _max_procs)
{
	logfile << "AutoSchedule(max_procs = " << _max_procs << ")" << endl;
	// set static variables ->
	sim = this;
	max_procs = _max_procs;
	// <-
	joint_list init_internal_joints;  // all joints
	p_joint_list init_outer_joints;  // virtual joints only
	joint_list all_vjoints;
	// list all joints
	get_all_joints(root, init_internal_joints, all_vjoints);
	// set init_outer_joints
	for(joint_list::iterator j=all_vjoints.begin(); j!=all_vjoints.end(); j++)
	{
		pJoint* v_pjoints[2];
		GetPJoint(*j, v_pjoints);
		init_outer_joints.push_back(v_pjoints[0]);
		init_outer_joints.push_back(v_pjoints[1]);
	}
	dpMain* dp = new dpMain;
	fVec init_proc_costs(max_procs);
	init_proc_costs.zero();
	dpScheduleNode* start_node = new dpScheduleNode(0, 0, init_proc_costs, 0, 0, 0, max_procs, init_internal_joints, init_outer_joints);

	dp->SetStartNode(start_node);
	dp->Search(-1, 1);
	dpNode* goal = dp->BestGoal();
	if(!goal)
	{
		logfile << "pSim::AutoSchedule: goal not found" << endl;
		delete dp;
		return -1;
	}
	cerr << "goal found" << endl;
	cerr << "cost = " << goal->TotalCost() << endl;
	fVec& proc_costs = ((dpScheduleNode*)goal)->ProcCosts();
	cerr << "proc_costs = " << tran(proc_costs) << endl;
	Joint** joints = new Joint* [n_joint+all_vjoints.size()];
	dpNode* n;
	int count = 0;
	for(n=goal->Parent(); n; n=n->Parent())
	{
		dpScheduleNode* s = (dpScheduleNode*)n;
		if(s->LastJoint())
		{
			cerr << "[" << s->LastJoint()->name << "/" << s->ScheduleDepth() << "] cost = " << n->TotalCost() << ", procs = [" << s->FirstProc() << ", " << s->LastProc() << "]" << endl;
		}
		else
		{
			cerr << "[default/" << s->ScheduleDepth() << "]" << endl;
		}
		joint_list all_internal;
		s->ListAllInternalJoints(all_internal);
		logfile << "joints =  " << endl;
		for(joint_list::iterator j=all_internal.begin(); j!=all_internal.end(); j++)
		{
			logfile << "  " << count << " " << (*j)->name << endl;
			joints[count] = *j;
			count++;
		}
	}
	logfile << "current count = " << count << ", number of vjoints = " << all_vjoints.size() << ", total n_joint = " << n_joint << endl;
	logfile << "joints = " << endl;
	for(int i=0; i<count; i++)
	{
		logfile << "[" << i << "] = " << joints[i]->name << endl;
	}
	for(joint_list::iterator j=all_vjoints.begin(); j!=all_vjoints.end(); j++)
	{
		joints[count] = *j;
		logfile << "[" << count << "] = " << joints[count]->name << endl;
		count++;
	}
#if 1
	Schedule(joints);
#endif
	delete[] joints;
	delete dp;
	return 0;
}

int dpScheduleNode::split_internal_joints(Joint* _last_joint, const joint_list& org_internal_joints, joint_list& _internal_joints_0, joint_list& _internal_joints_1)
{
	_internal_joints_0.clear();
	_internal_joints_1.clear();
	joint_list::const_iterator j;
//	logfile << "-- split at " << _last_joint->name << endl;
	for(j=org_internal_joints.begin(); j!=org_internal_joints.end(); j++)
	{
//		logfile << "    " << (*j)->name << " goes to " << flush;
		if(*j == _last_joint)
		{
//			logfile << " none" << endl;
		}
		else if((*j)->isAscendant(_last_joint))
		{
			_internal_joints_0.push_back(*j);
//			logfile << " 0" << endl;
		}
		else
		{
			_internal_joints_1.push_back(*j);
//			logfile << " 1" << endl;
		}
	}
//	logfile << "split end" << endl;
	return 0;
}

int dpScheduleNode::set_outer_joints(pJoint* last_pjoint, const p_joint_list& org_outer_joints, p_joint_list& new_outer_joints)
{
	new_outer_joints.clear();
	new_outer_joints.push_back(last_pjoint);
	p_joint_list::const_iterator p;
	int child_id = (last_pjoint->ParentSide()) ? 1 : 0;
	for(p=org_outer_joints.begin(); p!=org_outer_joints.end(); p++)
	{
		if((*p)->GetJoint()->isAscendant(last_pjoint->GetJoint()))
		{
			if(child_id == 0)
				new_outer_joints.push_back(*p);
		}
		else if(child_id == 1)
		{
			new_outer_joints.push_back(*p);
		}
	}
	return 0;
}

#ifdef USE_MPI
/**
 * assign processes for MPI
 */
int pSim::AssignProcesses(int max_procs)
{
	size = max_procs;
	subchains->assign_processes(0, max_procs);

	all_acc_types = new MPI_Datatype [max_procs];
	int** lengths = new int* [max_procs];
	MPI_Aint** disps = new MPI_Aint* [max_procs];
	MPI_Datatype** oldtypes = new MPI_Datatype* [max_procs];
	int* n_proc_joints = new int [max_procs];
	int i;
	for(i=0; i<max_procs; i++)
	{
		lengths[i] = new int [3*n_joint];
		disps[i] = new MPI_Aint [3*n_joint];
		oldtypes[i] = new MPI_Datatype [3*n_joint];
		n_proc_joints[i] = 0;
	}
	subchains->create_types(n_proc_joints, lengths, disps, oldtypes);
	for(i=0; i<max_procs; i++)
	{
		MPI_Type_create_struct(n_proc_joints[i], lengths[i], disps[i], oldtypes[i], all_acc_types+i);
		MPI_Type_commit(all_acc_types+i);
		logfile << "[" << rank << "]: all_acc_types[" << i << "] = " << all_acc_types[i] << endl;
	}

	for(i=0; i<max_procs; i++)
	{
		delete[] lengths[i];
		delete[] disps[i];
		delete[] oldtypes[i];
	}
	delete[] lengths;
	delete[] disps;
	delete[] oldtypes;
	delete[] n_proc_joints;
	return 0;
}

// available ranks: start_rank -> end_rank-1
int pSubChain::assign_processes(int start_rank, int end_rank)
{
	rank = start_rank;
	// assign different processes to the children
	if(children[0] && children[1] && children[0] != children[1] &&
	   children[0]->last_joint && children[1]->last_joint &&
	   end_rank > start_rank+1)
	{
		int n_my_procs = end_rank - start_rank;
		int n_half_procs = n_my_procs/2;
		children[0]->assign_processes(start_rank, start_rank+n_half_procs);
		children[1]->assign_processes(start_rank+n_half_procs, end_rank);
	}
	// otherwise assign the same process
	else
	{
		if(children[0])
		{
			children[0]->assign_processes(start_rank, end_rank);
		}
		if(children[0] != children[1] && children[1])
		{
			children[1]->assign_processes(start_rank, end_rank);
		}
	}
	return 0;
}

int pSubChain::create_types(int* n_proc_joints, int** _lengths, MPI_Aint** _disps, MPI_Datatype** _oldtypes)
{
	if(!this) return 0;
	// types for sending to parent
	if(parent && rank != parent->rank)
	{
		logfile << "[" << sim->rank << "] create_types " << last_joint->name << endl;
		int n_mat = n_outer_joints * n_outer_joints;
		int count = 0;
		int* lengths = new int [n_mat];
		MPI_Aint* disps = new int [n_mat];
		MPI_Datatype* oldtypes = new int [n_mat];
		for(int i=0; i<n_outer_joints; i++)
		{
			for(int j=0; j<n_outer_joints; j++)
			{
				lengths[count] = 36;
				oldtypes[count] = MPI_DOUBLE;
				MPI_Get_address(Lambda[i][j].data(), disps+count);
				count++;
			}
		}
		MPI_Type_create_struct(n_mat, lengths, disps, oldtypes, &parent_lambda_type);
		MPI_Type_commit(&parent_lambda_type);
		logfile << "[" << sim->rank << "]: parent_lambda_type = " << parent_lambda_type << endl;
		// acc
		for(int i=0; i<n_outer_joints; i++)
		{
			lengths[i] = 6;
			oldtypes[i] = MPI_DOUBLE;
			MPI_Get_address(acc_temp[i].data(), disps+i);
			count++;
		}
		MPI_Type_create_struct(n_outer_joints, lengths, disps, oldtypes, &parent_acc_type);
		MPI_Type_commit(&parent_acc_type);
		logfile << "[" << sim->rank << "]: parent_acc_type = " << parent_acc_type << endl;
		delete[] lengths;
		delete[] disps;
		delete[] oldtypes;
	}
	if(children[0] && rank != children[0]->rank || children[1] && rank != children[1]->rank)
	{
		int n_array = n_outer_joints + 2;
		int* lengths = new int [n_array];
		MPI_Aint* disps = new int [n_array];
		MPI_Datatype* oldtypes = new int [n_array];
		// force
		for(int i=0; i<n_outer_joints; i++)
		{
			lengths[i] = 6;
			oldtypes[i] = MPI_DOUBLE;
			MPI_Get_address(outer_joints[i]->f_final.data(), disps+i);
		}
		// f_final
		lengths[n_outer_joints] = 6;
		lengths[n_outer_joints+1] = 6;
		oldtypes[n_outer_joints] = MPI_DOUBLE;
		oldtypes[n_outer_joints+1] = MPI_DOUBLE;
		MPI_Get_address(last_pjoints[0]->f_final.data(), disps+n_outer_joints);
		MPI_Get_address(last_pjoints[1]->f_final.data(), disps+n_outer_joints+1);
		MPI_Type_create_struct(n_outer_joints+2, lengths, disps, oldtypes, &parent_force_type);
		MPI_Type_commit(&parent_force_type);
		logfile << "[" << sim->rank << "]: parent_force_type = " << parent_force_type << endl;
		delete[] lengths;
		delete[] disps;
		delete[] oldtypes;
	}
	// acc
	if(last_joint && last_joint->n_dof > 0)
	{
		int index = n_proc_joints[rank];
		_oldtypes[rank][index] = MPI_DOUBLE;
		_oldtypes[rank][index+1] = MPI_DOUBLE;
		_oldtypes[rank][index+2] = MPI_DOUBLE;
		MPI_Get_address(acc_final.data(), _disps[rank]+index);
		MPI_Get_address(last_pjoints[0]->f_final.data(), _disps[rank]+index+1);
		MPI_Get_address(last_pjoints[1]->f_final.data(), _disps[rank]+index+2);
		_lengths[rank][index] = acc_final.size();
		_lengths[rank][index+1] = 6;
		_lengths[rank][index+2] = 6;
		n_proc_joints[rank] += 3;
	}
	children[0]->create_types(n_proc_joints, _lengths, _disps, _oldtypes);
	if(children[0] != children[1]) children[1]->create_types(n_proc_joints, _lengths, _disps, _oldtypes);
	return 0;
}

#endif

/**
 * default schedule: most efficient for serial computation
 */
int pSim::Schedule()
{
	if(subchains) delete subchains;
	subchains = 0;
	// first attach subchains ignoring the virtual links
	subchains = default_schedule(0, root);
	// insert subchains for the virtual links
	default_schedule_virtual(root);
	subchains->init();
	return 0;
}

pSubChain* pSim::default_schedule(pSubChain* p, Joint* j)
{
	if(!j) return 0;
	if(j->i_joint < 0) return 0;  // OK?
	if(j->real) return 0;  // skip virtual links for now
	// skip mass-less end points (but include space)
	if(j->mass < TINY_MASS && j->parent)
	{
		pSubChain* c1 = default_schedule(p, j->brother);
		if(c1) return c1;
		pSubChain* c0 = default_schedule(p, j->child);
		return c0;
	}
	// create subchain for j
	pJoint* pj0, *pj1;
	pj0 = joint_info[j->i_joint].pjoints[0];
	pj1 = joint_info[j->i_joint].pjoints[1];
	pSubChain* sc = new pSubChain(this, p, pj0, pj1);
	// children subchains
	pSubChain* c0 = 0, *c1 = 0;
	// brother or parent link
	if(j->brother)
	{
		c1 = default_schedule(sc, j->brother);
	}
	if(!c1 && j->parent && j->parent->i_joint >= 0)
	{
		pLink* pl = joint_info[j->parent->i_joint].plink;
		c1 = new pSubChain(this, sc, pl);
	}
	// children
	if(j->child && !j->child->real)
	{
		c0 = default_schedule(sc, j->child);
	}
	if(!c0 && j->i_joint >= 0)
	{
		pLink* pl = joint_info[j->i_joint].plink;
		c0 = new pSubChain(this, sc, pl);
	}
	sc->children[0] = c0;
	sc->children[1] = c1;
	return sc;
}

void pSim::default_schedule_virtual(Joint* j)
{
	if(!j) return;
	if(j->i_joint < 0) return;
	// virtual but not contact
	if(j->real && contact_vjoint_index(j) < 0)
	{
		pJoint* pj0, *pj1;
		pj0 = joint_info[j->i_joint].pjoints[0];
		pj1 = joint_info[j->i_joint].pjoints[1];
		pSubChain* sc = new pSubChain(this, subchains, pj0, pj1);
		// insert sc between subchains and its child
		// same child for [0] and [1]
		sc->children[0] = subchains->children[0];
		sc->children[1] = subchains->children[0];
		// subchains->children[1] must be null
		subchains->children[0]->parent = sc;
		subchains->children[0] = sc;
	}
	default_schedule_virtual(j->brother);
	default_schedule_virtual(j->child);
}

void pSubChain::init()
{
	if(!this) return;
	children[0]->init();
	if(children[0] != children[1]) children[1]->init();
	if(Lambda)
	{
		for(int i=0; i<n_outer_joints; i++) delete[] Lambda[i];
		delete[] Lambda;
	}
	Lambda = 0;
	if(acc_temp) delete[] acc_temp;
	acc_temp = 0;
	if(vel_temp) delete[] vel_temp;
	vel_temp = 0;
	n_outer_joints = 0;
	if(outer_joints) delete[] outer_joints;
	if(outer_joints_origin) delete[] outer_joints_origin;
	if(outer_joints_index) delete[] outer_joints_index;
	outer_joints = 0;
	outer_joints_origin = 0;
	outer_joints_index = 0;
	// consists of a single link
	if(!last_joint && n_links == 1)
	{
		int i;
		// count outer joints
		Joint* p = links[0]->joint;
		links[0]->subchain = this;
		n_outer_joints = 1;  // parent
		Joint* cur;
		for(cur=p->child; cur; cur=cur->brother)
		{
			if(cur->real || cur->mass > TINY_MASS)
				n_outer_joints++;  // children
		}
		// virtual links
		for(i=0; i<sim->n_joint; i++)
		{
			if(sim->joint_info[i].pjoints[0]->joint->real == links[0]->joint)
				n_outer_joints++;
		}
		outer_joints = new pJoint* [n_outer_joints];
		outer_joints_origin = new int [n_outer_joints];
		outer_joints_index = new int [n_outer_joints];
		outer_joints[0] = sim->joint_info[p->i_joint].pjoints[0];  // child side
		outer_joints_origin[0] = -1;
		outer_joints_index[0] = -1;
		n_outer_joints = 1;
		for(cur=p->child; cur; cur=cur->brother)
		{
			if(cur->real || cur->mass > TINY_MASS)
			{
				outer_joints[n_outer_joints] = sim->joint_info[cur->i_joint].pjoints[1];  // parent side
				outer_joints[n_outer_joints]->subchain = this;
				outer_joints_origin[n_outer_joints] = -1;
				outer_joints_index[n_outer_joints] = -1;
				n_outer_joints++;
			}
		}
		for(i=0; i<sim->n_joint; i++)
		{
			if(sim->joint_info[i].pjoints[0]->joint->real == links[0]->joint)
			{
				outer_joints[n_outer_joints] = sim->joint_info[i].pjoints[0];  // child side
				outer_joints[n_outer_joints]->subchain = this;
				outer_joints_origin[n_outer_joints] = -1;
				outer_joints_index[n_outer_joints] = -1;
				n_outer_joints++;
			}
		}
	}
	// gather information from children subchains
	else
	{
		int i, count;
		// last_index
		if(children[0]) last_index[0] = children[0]->get_outer_index(last_pjoints[0]);
		if(children[1]) last_index[1] = children[1]->get_outer_index(last_pjoints[1]);
		// outer joints
		n_outer_joints = children[0]->n_outer_joints - 1;
		if(children[0] == children[1])
			n_outer_joints--;
		else if(children[1])
			n_outer_joints += children[1]->n_outer_joints - 1;
		if(n_outer_joints > 0)
		{
			outer_joints = new pJoint* [n_outer_joints];
			outer_joints_origin = new int [n_outer_joints];
			outer_joints_index = new int [n_outer_joints];
			count = 0;
			for(i=0; i<children[0]->n_outer_joints; i++)
			{
				if(children[0]->outer_joints[i]->joint != last_joint)
				{
					outer_joints[count] = children[0]->outer_joints[i];
					outer_joints_origin[count] = 0;
					if(children[0] == children[1] &&
					   outer_joints[count]->joint->real && 
					   outer_joints[count]->joint->real != outer_joints[count]->link_side)  // virtual side
					{
						outer_joints_origin[count] = 1;
					}
					outer_joints_index[count] = i;
					count++;
				}
			}
			if(children[1] && children[0] != children[1])
			{
				for(i=0; i<children[1]->n_outer_joints; i++)
				{
					if(children[1]->outer_joints[i]->joint != last_joint)
					{
						outer_joints[count] = children[1]->outer_joints[i];
						outer_joints_origin[count] = 1;
						outer_joints_index[count] = i;
						count++;
					}
				}
			}
		}
		// links
		n_links = children[0]->n_links;
		if(children[1] && children[0] != children[1])
			n_links += children[1]->n_links;
		if(links) delete[] links;
		links = 0;
		links = new pLink* [n_links];
		count = 0;
		for(i=0; i<children[0]->n_links; i++)
		{
			links[count] = children[0]->links[i];
			count++;
		}
		if(children[1] && children[0] != children[1])
		{
			for(i=0; i<children[1]->n_links; i++)
			{
				links[count] = children[1]->links[i];
				count++;
			}
		}
	}
	// create matrices and vectors
	P.resize(6, 6);
	P.zero();
	da6.resize(6);
	da6.zero();
	W.resize(6, 6);
	W.zero();
	IW.resize(6, 6);
	IW.zero();
#ifdef USE_DCA
	Vhat.resize(6,6);
	Vhat.zero();
	SVS.resize(n_dof, n_dof);
	SVS.zero();
#else
	Gamma.resize(n_const, n_const);
	Gamma.zero();
	Gamma_inv.resize(n_const, n_const);
	Gamma_inv.zero();
#endif
	f_temp.resize(n_const);
	f_temp.zero();
	colf_temp.resize(n_const);
	colf_temp.zero();
	tau.resize(n_dof);
	tau.zero();
	acc_final.resize(n_dof);
	acc_final.zero();
	if(n_outer_joints > 0)
	{
		int i, j;
		Lambda = new fMat* [n_outer_joints];
		acc_temp = new fVec [n_outer_joints];
		vel_temp = new fVec [n_outer_joints];
		for(i=0; i<n_outer_joints; i++)
		{
			Lambda[i] = new fMat [n_outer_joints];
			for(j=0; j<n_outer_joints; j++)
			{
				Lambda[i][j].resize(6, 6);
				Lambda[i][j].zero();
			}
			acc_temp[i].resize(6);
			acc_temp[i].zero();
			vel_temp[i].resize(6);
			vel_temp[i].zero();
		}
	}
}

/**
 * manual schedule: specify the order
 */
int pSim::Schedule(Joint** joints)
{
	if(subchains) delete subchains;
	subchains = 0;
	subchain_list buf;
	build_subchain_tree(n_joint, joints, buf);
	if(buf.size() == 1)
	{
		subchains = *(buf.begin());
		subchains->init();
		return 0;
	}
	cerr << "pSim::Schedule(joints): error- invalid joint order" << endl;
	return -1;
}

int pSim::build_subchain_tree(int _n_joints, Joint** joints, subchain_list& buf)
{
	int i;
	for(i=0; i<_n_joints; i++)
	{
		cerr << "build_subchain_tree " << joints[i]->name << endl;
		build_subchain_tree(joints[i], buf);
	}
	return 0;
}

void pSim::build_subchain_tree(Joint* cur_joint, subchain_list& buf)
{
	JointInfo& jinfo = joint_info[cur_joint->i_joint];
	pJoint* pj0 = jinfo.pjoints[0];
	pJoint* pj1 = jinfo.pjoints[1];
	int pj0_done = false;
	int pj1_done = false;
	pSubChain* to_remove[2] = {0, 0};
	pSubChain* myp = new pSubChain(this, 0, pj0, pj1);
	subchain_list::iterator i;
	for(i=buf.begin(); i!=buf.end(); i++)
	{
		if(!pj0_done && in_subchain(*i, pj0->plink))
		{
			myp->children[0] = *i;
			(*i)->parent = myp;
			pj0_done = true;
			if(!to_remove[0]) to_remove[0] = *i;
			else if(!to_remove[1]) to_remove[1] = *i;
		}
		if(!pj1_done && in_subchain(*i, pj1->plink))
		{
			myp->children[1] = *i;
			(*i)->parent = myp;
			pj1_done = true;
			if(!to_remove[0]) to_remove[0] = *i;
			else if(!to_remove[1]) to_remove[1] = *i;
		}
	}
	if(!pj0_done && pj0->plink)
	{
		pSubChain* newp = new pSubChain(this, myp, pj0->plink);
		myp->children[0] = newp;
	}
	if(!pj1_done && pj1->plink)
	{
		pSubChain* newp = new pSubChain(this, myp, pj1->plink);
		myp->children[1] = newp;
	}
	if(to_remove[0]) buf.remove(to_remove[0]);
	if(to_remove[1]) buf.remove(to_remove[1]);
	buf.push_front(myp);
}

int pSim::in_subchain(pSubChain* sc, pLink* pl)
{
	if(!sc) return 0;
	if(sc->links && sc->links[0] == pl) return 1;
	if(in_subchain(sc->children[0], pl)) return 1;
	if(sc->children[0] != sc->children[1] &&
	   in_subchain(sc->children[1], pl)) return 1;
	return 0;
}
