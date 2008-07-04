/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/*!
 * @file   dp.h
 * @author Katsu Yamane
 * @date   11/16/2005
 * @brief  Base class for generic discrete search.
 */

#ifndef __DP_H__
#define __DP_H__

#include <iostream>
using namespace std;

class dpQueue;
class dpMain;

/*!
 * @class dpNode
 * Base class for search nodes.
 */
class dpNode 
{
	friend class dpQueue;
	friend class dpMain;
public:
	dpNode() {
		cost = 0.0;
		astar_cost = 0.0;
		total_cost = 0.0;
		dp_main = 0;
		parent = 0;
		child = 0;
		brother = 0;
		queue = 0;
		id = -1;
		depth = 0;
		active = true;
	}
	virtual ~dpNode() {
		if(brother) delete brother;
		if(child) delete child;
	}

	dpNode* Parent() {
		return parent;
	}
	dpNode* Brother() {
		return brother;
	}
	dpNode* Child() {
		return child;
	}
	
	int Depth(dpNode* target_p = 0) {
		int ret;
		dpNode* p;
		for(p=this, ret=0; p && p!=target_p; p=p->parent, ret++);
		if(target_p) ret++;
		return ret;
	}
	dpNode* GetParent(int id) {
		int i;
		dpNode* cur;
		for(i=0, cur=this; cur && i<id; i++, cur=cur->parent);
		return cur;
	}
	
	double TotalCost() {
		return total_cost;
	}
	double Cost() {
		return cost;
	}
	double TotalAstarCost() {
		return total_astar_cost;
	}
	double AstarCost() {
		return astar_cost;
	}

	dpQueue* Queue() {
		return queue;
	}

	int ID() {
		return id;
	}
	
protected:
	/*!
	 * @name Virtual functions to be defined by subclass.
	 */
	/*@{*/
	//! Create (potential) child nodes.
	/*!
	 * Create (potential) child nodes.
	 * @param[out] nodes  array of pointers to the child nodes
	 */
	virtual int create_child_nodes(dpNode**& nodes) = 0;
	//! Compute the local cost at the node.
	virtual double calc_cost() = 0;
	//! Compute the A-star cost at the node (optional).
	virtual double calc_astar_cost(dpNode* potential_parent) {
		return 0.0;
	}
	//! Check if the node's state is the same as @c refnode. (to remove duplicates)
	virtual int same_state(dpNode* refnode) = 0;
	//! Check if the state is goal
	virtual int is_goal() = 0;
	/*@}*/

	int open(int _max_nodes, int _max_goals);
	void add_child(dpNode* n);
	void remove_single();
	void remove();

	dpNode* next_depth(dpNode* first_child = 0) {
		dpNode* c, *ret = 0;
		for(c = first_child ? first_child->brother : child; c && !ret; c=c->brother)
		{
			if(c->active) return c;
			else if((ret = c->next_depth())) return ret;
		}
		if(parent) return parent->next_depth(this);
		return 0;
	}

	dpNode* next_breadth(dpNode* refnode) {
		if(!this) return 0;
		if(depth >= refnode->depth && active) return this;
		dpNode* ret = 0;
		if(ret = brother->next_breadth(refnode)) return ret;
		return child->next_breadth(refnode);
	}

	virtual void dump(ostream& ost) {
	}
	void dump_trajectory(ostream& ost) {
		if(!this) return;
		parent->dump_trajectory(ost);
		dump(ost);
	}
	void dump_all(ostream& ost) {
		if(!this) return;
		for(int i=0; i<depth; i++) ost << " " << flush;
		dump(ost);
		child->dump_all(ost);
		brother->dump_all(ost);
	}

	double cost;  // cost from parent to this node
	double astar_cost;

	// automatically set
	dpNode* parent;
	dpNode* brother;
	dpNode* child;
	double total_cost; // total cost from start to this node
	double total_astar_cost;
	dpMain* dp_main;
	dpQueue* queue;
	int id;
	int depth;
	int active;
};

/*!
 * @class dpQueue
 * Nodes for the node queue.
 * Forms a binary tree for sorting the search nodes in the order of the total cost.
 */
class dpQueue 
{
	friend class dpNode;
	friend class dpMain;
public:
	dpQueue(dpNode* n) {
		node = n;
		parent = 0;
		smaller_child = 0;
		larger_child = 0;
		active = true;
	}
	~dpQueue() {
		if(larger_child) delete larger_child;
		if(smaller_child) delete smaller_child;
	}

protected:
	void add_queue(dpQueue* q) {
		if(q->node->total_astar_cost >= node->total_astar_cost)
		{
			if(!larger_child)
			{
				larger_child = q;
				q->parent = this;
			}
			else larger_child->add_queue(q);
		}
		else
		{
			if(!smaller_child)
			{
				smaller_child = q;
				q->parent = this;
			}
			else smaller_child->add_queue(q);
		}
		
	}
	dpQueue* smallest() {
		dpQueue* ret = 0;
//		cerr << "smallest: " << flush;
//		node->dump(cerr);
		if(smaller_child) ret = smaller_child->smallest();
		if(!ret)
		{
			if(active)
			{
				ret = this;
			}
			else if(larger_child) ret = larger_child->smallest();
		}
		return ret;
	}
	dpQueue* larger_goal() {
		dpQueue* ret = larger_goal_sub();
		dpQueue* q = this;
		while(!ret && q->parent)
		{
			if(q->parent->node->is_goal() &&
			   q->parent->smaller_child == q)
			{
				ret = q->parent;
			}
			else
			{
				if(q->parent->smaller_child == q)
					ret = q->parent->larger_goal_sub();
			}
			q = q->parent;
		}
		return ret;
	}
	dpQueue* larger_goal_sub() {
		if(larger_child) return larger_child->smallest_goal();
		return 0;
	}

	dpQueue* smallest_goal() {
		dpQueue* ret = 0;
		if(smaller_child) ret = smaller_child->smallest_goal();
		if(!ret)
		{
			if(node->is_goal()) ret = this;
			else if(larger_child) ret = larger_child->smallest_goal();
		}
		return ret;
	}
	
	void dump(ostream& ost) {
		if(!this) return;
		ost << "--- id = " << node->id << endl;
		ost << " cost = " << node->total_cost << ", goal = " << node->is_goal() << endl;
		ost << " smaller = " << (smaller_child ? smaller_child->node->id : -1) << ", larger = " << (larger_child ? larger_child->node->id : -1) << endl;
		smaller_child->dump(ost);
		larger_child->dump(ost);
	}
	
	dpNode* node;
	dpQueue* parent;
	dpQueue* smaller_child;
	dpQueue* larger_child;

	int active;
};

/*!
 * @class dpMain
 * Main class for the search.
 */
class dpMain
{
	friend class dpQueue;
	friend class dpNode;
public:
	dpMain() {
		start_node = 0;
		top_queue = 0;
		n_goals = 0;
		n_nodes = 0;
	}
	virtual ~dpMain() {
		if(start_node) delete start_node;
		if(top_queue) delete top_queue;
	}

	//! Set the initial node for search.
	void SetStartNode(dpNode* _n) {
		start_node = _n;
		add_node(_n);
		if(_n->is_goal()) n_goals++;
	}

	/*!
	 * @name Search functions.
	 */
	/*@{*/
	//! Dijkstra or A* search: find the node with the smallest cost.
	/*!
	 * Dijkstra or A* search: find the node with the smallest cost until
	 * the number of nodes exceeds @c _max_nodes or the number of
	 * goalds exceeds @c _max_goals.
	 */
	int Search(int _max_nodes = -1, int _max_goals = -1);
	//! Dijkstra or A* search with maximum search time.
	int Search(double max_time);
	//! Depth-first search.
	int SearchDepthFirst(int _max_nodes = -1, int _max_goals = -1);
	//! Breadth-first search.
	int SearchBreadthFirst(int _max_nodes = -1, int _max_goals = -1);
	/*@}*/

	// clear all nodes and edges
	virtual void ClearAll() {
		reset();
	}

	void DumpTrajectory(ostream& ost, dpNode* goal);

	//! Extract the goal with the smallest cost (if any).
	dpNode* BestGoal(dpNode* ref = 0) {
		if(!top_queue) return 0;
		dpQueue* g = 0;
		if(ref) g = ref->queue->larger_goal();
		else g = top_queue->smallest_goal();
		if(g) return g->node;
		return 0;
	}
	
	int NumNodes() {
		return n_nodes;
	}
	int NumGoals() {
		return n_goals;
	}
	void DumpAll(ostream& ost) {
		cerr << "--" << endl;
		start_node->dump_all(ost);
		cerr << "--" << endl;
	}
	void DumpQueue(ostream& ost) {
		top_queue->dump(ost);
	}

protected:
	void reset() {
		if(start_node) delete start_node;
		if(top_queue) delete top_queue;
		n_nodes = 0;
		n_goals = 0;
	}
	void add_node(dpNode* _n) {
		_n->dp_main = this;
		_n->id = n_nodes;
		n_nodes++;
		dpQueue* q = new dpQueue(_n);
		add_queue(q);
		_n->queue = q;
	}
	void add_queue(dpQueue* _q) {
		if(!top_queue)
		{
			top_queue = _q;
			return;
		}
		top_queue->add_queue(_q);
	}
	dpQueue* smallest_queue() {
		if(!top_queue) return 0;
		return top_queue->smallest();
	}
	void remove_node(dpNode* node) {
		node->queue->active = false;
		node->remove();  // remove all child nodes
	}
	void remove_node_single(dpNode* node) {
		node->queue->active = false;
		node->remove_single();  // remove all child nodes
	}
	int is_best(dpNode* ref, dpNode* target);
	
	dpNode* next_breadth(dpNode* refnode) {
		return start_node->next_breadth(refnode);
	}

	dpQueue* top_queue;
	dpNode* start_node;
	int n_goals;
	int n_nodes;
};


#endif
