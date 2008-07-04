/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * The University of Tokyo
 */
/**
 * dp.cpp
 * Create: Katsu Yamane, 05.11.16
 */

#include "dp.h"
//#include <dims_timer.h>

int dpMain::Search(int _max_nodes, int _max_goals)
{
	if(!start_node)
	{
		cerr << "dpMain error: start node not set" << endl;
		return -1;
	}
	dpQueue* q = top_queue;
	while(q)
	{
		if(q->node->open(_max_nodes, _max_goals)) break;
		remove_node_single(q->node);
		q = smallest_queue();
	}
	return 0;
}

#if 0
int dpMain::Search(double max_time)
{
	if(!start_node)
	{
		cerr << "dpMain error: start node not set" << endl;
		return -1;
	}
	dpQueue* q = top_queue;
	LongInteger start_tick = GetTick();
	while(q)
	{
		if(q->node->open(-1, -1)) break;
		remove_node_single(q->node);
		q = smallest_queue();
		LongInteger cur_tick = GetTick();
		if(ExpiredTime(start_tick, cur_tick) > max_time) return 1;
	}
	return 0;
}
#endif

int dpMain::SearchDepthFirst(int _max_nodes, int _max_goals)
{
	if(!start_node)
	{
		cerr << "dpMain error: start node not set" << endl;
		return -1;
	}
	dpNode* node = start_node;
	while(node)
	{
		DumpAll(cerr);
		if(node->open(_max_nodes, _max_goals)) break;
		remove_node_single(node);
		node = node->next_depth();
	}
	return 0;
}

int dpMain::SearchBreadthFirst(int _max_nodes, int _max_goals)
{
	if(!start_node)
	{
		cerr << "dpMain error: start node not set" << endl;
		return -1;
	}
	dpNode* node = start_node;
	while(node)
	{
		if(node->open(_max_nodes, _max_goals)) break;
		remove_node_single(node);
		node = next_breadth(node);
		DumpAll(cerr);
	}
	return 0;
}

int dpNode::open(int _max_nodes, int _max_goals)
{
	int i, n_nodes = 0;
	dpNode** nodes = 0;
	n_nodes = create_child_nodes(nodes);
	if(n_nodes == 0) return 0;
	if(!nodes) return 0;
	for(i=0; i<n_nodes; i++)
	{
		dpNode* n = nodes[i];
		if(!n) continue;
		n->brother = child; // added by nishimura
		n->parent = this; // added by nishimura
		n->depth = depth+1; // added by nishimura
		n->cost = n->calc_cost();
		n->astar_cost = n->calc_astar_cost(this);
		n->total_cost = total_cost + n->cost;
		n->total_astar_cost = n->total_cost + n->astar_cost;
		if(!dp_main->is_best(dp_main->start_node, n))  // check if there is better node
		{
			n->brother = 0;
			delete n;
			nodes[i] = 0;
			continue;
		}
//		add_child(n); // removed by nishimura
		child = n; // added by nishimura
		dp_main->add_node(n); // added by nishimura
		
		if(n->is_goal()) dp_main->n_goals++;
	}
	delete[] nodes;
	if(_max_nodes >= 0 && dp_main->n_nodes >= _max_nodes){
		return -1;
	}
	if(_max_goals >= 0 && dp_main->n_goals >= _max_goals){
		return -1;
	}
	return 0;
}

int dpMain::is_best(dpNode* ref, dpNode* target)
{
	if(!ref) return true;
	int ret = true;
	if(ref != target && target->same_state(ref))
	{
//		if(target->total_astar_cost <= ref->total_astar_cost)
		if(target->total_cost <= ref->total_cost)
		{
			// target is better: remove ref
			remove_node(ref);
		}
		else
		{
			// ref is better: remove target
			ret = false;
		}
	}
	else
	{
		dpNode* n;
		int myret = true;
		for(n=ref->child; n && myret; n=n->brother)
		{
			myret = is_best(n, target);
		}
		ret = myret;
	}
	return ret;
}

void dpMain::DumpTrajectory(ostream& ost, dpNode* goal)
{
	goal->dump_trajectory(ost);
}

void dpNode::add_child(dpNode* n)
{
	n->brother = child;
	child = n;
	n->parent = this;
	n->depth = depth+1;
	dp_main->add_node(n);
}

void dpNode::remove_single()
{
	active = false;
	queue->active = false;
}

void dpNode::remove()
{
	dpNode* n;
	for(n=child; n; n=n->brother) n->remove();
	active = false;
	queue->active = false;
}
