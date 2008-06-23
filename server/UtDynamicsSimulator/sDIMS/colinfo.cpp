/**
 * colinfo.cpp
 * Create: Katsu Yamane, 04.03.05
 */

#include "colinfo.h"

//#define VERBOSE

int ColModel::LineIntersection(PQP_CollideResult& cres, const fVec3& p1, const fVec3& p2)
{
	static PQP_REAL T[3], RR[3][3];
	static PQP_REAL P1[3], P2[3];
	fVec3 pos(joint->abs_pos);
	fMat33 att(joint->abs_att);
	T[0] = pos(0);
	T[1] = pos(1);
	T[2] = pos(2);
	RR[0][0] = att(0,0);
	RR[0][1] = att(0,1);
	RR[0][2] = att(0,2);
	RR[1][0] = att(1,0);
	RR[1][1] = att(1,1);
	RR[1][2] = att(1,2);
	RR[2][0] = att(2,0);
	RR[2][1] = att(2,1);
	RR[2][2] = att(2,2);
	P1[0] = p1(0);
	P1[1] = p1(1);
	P1[2] = p1(2);
	P2[0] = p2(0);
	P2[1] = p2(1);
	P2[2] = p2(2);

	return PQP_TriLineIntersect(&cres, RR, T, model, P1, P2);
}

int ColPair::Collide(PQP_CollideResult& cres)
{
	static PQP_REAL R1[3][3];
	static PQP_REAL T1[3];
	static PQP_REAL R2[3][3];
	static PQP_REAL T2[3];
	fVec3 p1(models[0]->joint->abs_pos);
	fMat33 r1(models[0]->joint->abs_att);
	fVec3 p2(models[1]->joint->abs_pos);
	fMat33 r2(models[1]->joint->abs_att);
	// memo: definition of R1 and R2 is row major in PQP
	R1[0][0] = r1(0,0);
	R1[0][1] = r1(0,1);
	R1[0][2] = r1(0,2);
	R1[1][0] = r1(1,0);
	R1[1][1] = r1(1,1);
	R1[1][2] = r1(1,2);
	R1[2][0] = r1(2,0);
	R1[2][1] = r1(2,1);
	R1[2][2] = r1(2,2);
	T1[0] = p1(0);
	T1[1] = p1(1);
	T1[2] = p1(2);

	R2[0][0] = r2(0,0);
	R2[0][1] = r2(0,1);
	R2[0][2] = r2(0,2);
	R2[1][0] = r2(1,0);
	R2[1][1] = r2(1,1);
	R2[1][2] = r2(1,2);
	R2[2][0] = r2(2,0);
	R2[2][1] = r2(2,1);
	R2[2][2] = r2(2,2);
	T2[0] = p2(0);
	T2[1] = p2(1);
	T2[2] = p2(2);

	return PQP_Collide(&cres,
					   R1, T1, models[0]->model,
					   R2, T2, models[1]->model);
}

int ColPair::Distance(PQP_DistanceResult& dres,
					  PQP_REAL rel_error, PQP_REAL abs_error,
					  int qsize)
{
	static PQP_REAL R1[3][3];
	static PQP_REAL T1[3];
	static PQP_REAL R2[3][3];
	static PQP_REAL T2[3];
	fVec3 p1(models[0]->joint->abs_pos);
	fMat33 r1(models[0]->joint->abs_att);
	fVec3 p2(models[1]->joint->abs_pos);
	fMat33 r2(models[1]->joint->abs_att);
	// memo: definition of R1 and R2 is row major in PQP
	R1[0][0] = r1(0,0);
	R1[0][1] = r1(0,1);
	R1[0][2] = r1(0,2);
	R1[1][0] = r1(1,0);
	R1[1][1] = r1(1,1);
	R1[1][2] = r1(1,2);
	R1[2][0] = r1(2,0);
	R1[2][1] = r1(2,1);
	R1[2][2] = r1(2,2);
	T1[0] = p1(0);
	T1[1] = p1(1);
	T1[2] = p1(2);

	R2[0][0] = r2(0,0);
	R2[0][1] = r2(0,1);
	R2[0][2] = r2(0,2);
	R2[1][0] = r2(1,0);
	R2[1][1] = r2(1,1);
	R2[1][2] = r2(1,2);
	R2[2][0] = r2(2,0);
	R2[2][1] = r2(2,1);
	R2[2][2] = r2(2,2);
	T2[0] = p2(0);
	T2[1] = p2(1);
	T2[2] = p2(2);

	return PQP_Distance(&dres,
						R1, T1, models[0]->model,
						R2, T2, models[1]->model,
						rel_error, abs_error,
						qsize);
}

int ColPair::Tolerance(PQP_ToleranceResult& tres,
					   PQP_REAL tolerance)
{
	static PQP_REAL R1[3][3];
	static PQP_REAL T1[3];
	static PQP_REAL R2[3][3];
	static PQP_REAL T2[3];
	fVec3 p1(models[0]->joint->abs_pos);
	fMat33 r1(models[0]->joint->abs_att);
	fVec3 p2(models[1]->joint->abs_pos);
	fMat33 r2(models[1]->joint->abs_att);
	// memo: definition of R1 and R2 is row major in PQP
	R1[0][0] = r1(0,0);
	R1[0][1] = r1(0,1);
	R1[0][2] = r1(0,2);
	R1[1][0] = r1(1,0);
	R1[1][1] = r1(1,1);
	R1[1][2] = r1(1,2);
	R1[2][0] = r1(2,0);
	R1[2][1] = r1(2,1);
	R1[2][2] = r1(2,2);
	T1[0] = p1(0);
	T1[1] = p1(1);
	T1[2] = p1(2);

	R2[0][0] = r2(0,0);
	R2[0][1] = r2(0,1);
	R2[0][2] = r2(0,2);
	R2[1][0] = r2(1,0);
	R2[1][1] = r2(1,1);
	R2[1][2] = r2(1,2);
	R2[2][0] = r2(2,0);
	R2[2][1] = r2(2,1);
	R2[2][2] = r2(2,2);
	T2[0] = p2(0);
	T2[1] = p2(1);
	T2[2] = p2(2);

	return PQP_Tolerance(&tres,
						 R1, T1, models[0]->model,
						 R2, T2, models[1]->model,
						 tolerance);
}

/**
 * register triangles
 */
static void apply_transform(TransformNode* tnode, fVec3& pos, int scale_only = false)
{
	static float fscale[3];
	// scale
	tnode->getScale(fscale);
	pos(0) *= fscale[0];
	pos(1) *= fscale[1];
	pos(2) *= fscale[2];
	// translation, rotation
	if(!scale_only)
	{
		static fVec3 trans, tmp;
		static fMat33 att;
		static float fpos[3];
		static float frot[4];
		tnode->getTranslation(fpos);
		tnode->getRotation(frot);
		trans(0) = fpos[0];
		trans(1) = fpos[1];
		trans(2) = fpos[2];
		att.rot2mat(fVec3(frot[0], frot[1], frot[2]), frot[3]);
		tmp.mul(att, pos);
		pos.add(trans, tmp);
	}
}

int ColModel::add_triangles(TransformNode* tnode)
{
	if(!tnode) return 0;
	// search all shape nodes
	n_triangle = 0;
	add_triangles(tnode, (Node*)tnode);
	if(n_triangle > 0)
	{
//#ifdef VERBOSE
		cerr << "add_triangles(" << tnode->getName() << ")" << endl;
//#endif
		model->BeginModel();
		create_pqp_model();
		model->EndModel();
//#ifdef VERBOSE
		cerr << " total " << n_triangle << " triangles" << endl;
//#endif
	}
	return 0;
}

int ColModel::add_triangles(TransformNode* tnode, Node* cur)
{
	if(!cur) return -1;
	add_triangles_sub(tnode, cur->getChildNodes());
	return 0;
}

int ColModel::add_triangles_sub(TransformNode* tnode, Node* cur)
{
	if(!cur) return 0;
//	if(cur->getName()) cerr << "   " << cur->getName() << endl;
	if(cur->isIndexedFaceSetNode())
		add_triangles_face(tnode, (IndexedFaceSetNode*)cur);
	else
	{
		add_triangles_sub(tnode, cur->next());
		add_triangles_sub(tnode, cur->getChildNodes());
	}
	return 0;
}

int ColModel::add_triangles_face(TransformNode* tnode, IndexedFaceSetNode* inode)
{
	CoordinateNode* cnode = inode->getCoordinateNodes();
	if(!cnode) return -1;
	int n_my_vertex = cnode->getNPoints();
	int n_index = inode->getNCoordIndexes();
	if(n_my_vertex == 0 || n_index == 0) return 0;
	int i;
	int vertex_id_base = n_vertex;
	n_vertex += n_my_vertex;
	// reallocate memory for saving vertices and save current vertices
#if 1
	fVec3* tmp_vertices = vertices;
	vertices = new fVec3 [n_vertex];
	if(tmp_vertices)
	{
		for(i=0; i<vertex_id_base; i++)
			vertices[i].set(tmp_vertices[i]);
		delete[] tmp_vertices;
	}
#else
	fVec3* tmp_vertices = 0;
	if(vertices)
	{
		tmp_vertices = new fVec3 [vertex_id_base];
		for(i=0; i<vertex_id_base; i++)
			tmp_vertices[i].set(vertices[i]);
		delete[] vertices;
	}
	vertices = new fVec3 [n_vertex];
	for(i=0; i<vertex_id_base; i++)
		vertices[i].set(tmp_vertices[i]);
	if(tmp_vertices) delete[] tmp_vertices;
#endif
	float fp[3];
	for(i=0; i<n_my_vertex; i++)
	{
		cnode->getPoint(i, fp);
		vertices[i+vertex_id_base](0) = fp[0];
		vertices[i+vertex_id_base](1) = fp[1];
		vertices[i+vertex_id_base](2) = fp[2];
		apply_all_transforms(tnode, (Node*)cnode, vertices[i+vertex_id_base]);
	}
	// process polygons (all changed to ccw)
	int ccw = inode->getCCW();
	for(i=0; i<n_index; i++)
	{
		int c1, c2, c3;
		c1 = inode->getCoordIndex(i);
		i++;
		while( (c2 = inode->getCoordIndex(i)) != -1 &&
			   (c3 = inode->getCoordIndex(i+1)) != -1 )
		{
			TriangleInfo* ti = 0;
			if(ccw) ti = new TriangleInfo(c1+vertex_id_base, c2+vertex_id_base, c3+vertex_id_base);
			else ti = new TriangleInfo(c1+vertex_id_base, c3+vertex_id_base, c2+vertex_id_base);
			triangles.append(ti);
			n_triangle++;
			i++;
		}
		i += 1;
	}
	return 0;
}

void ColModel::apply_all_transforms(TransformNode* tnode, Node* cur, fVec3& pos)
{
	Node* node;
	int scale_only = false;
	for(node=cur; node; node=node->getParentNode())
	{
		if(node->isTransformNode())
		{
			// if node is the top transform node, apply scale only and break
			if(node == tnode)
				scale_only = true;
			apply_transform((TransformNode*)node, pos, scale_only);
		}
	}
}

int ColModel::create_pqp_model()
{
	PQP_REAL p1[3], p2[3], p3[3];
	PQP_REAL q1[3], q2[3], q3[3];
	tListElement<TriangleInfo>* ti;
	for(ti=triangles.first(); ti; ti=ti->next())
	{
		TriangleInfo* tri = ti->body();
		tri->neighbor_vertex[0] = -1;
		tri->neighbor_vertex[1] = -1;
		tri->neighbor_vertex[2] = -1;
		tri->neighbor_tri[0] = -1;
		tri->neighbor_tri[1] = -1;
		tri->neighbor_tri[2] = -1;
		int c1 = tri->index[0];
		int c2 = tri->index[1];
		int c3 = tri->index[2];
		tListElement<TriangleInfo>* a;
		int vert;
		for(a=triangles.first(); a; a=a->next())
		{
			if(a != ti)
			{
				if((vert = a->body()->have_edge(c1, c2)) >= 0)
				{
					tri->neighbor_vertex[0] = vert;
					tri->neighbor_tri[0] = a->id();
				}
				else if((vert = a->body()->have_edge(c2, c3)) >= 0)
				{
					tri->neighbor_vertex[1] = vert;
					tri->neighbor_tri[1] = a->id();
				}
				else if((vert = a->body()->have_edge(c3, c1)) >= 0)
				{
					tri->neighbor_vertex[2] = vert;
					tri->neighbor_tri[2] = a->id();
				}
			}
			if(tri->neighbor_vertex[0] >= 0 &&
			   tri->neighbor_vertex[1] >= 0 &&
			   tri->neighbor_vertex[2] >= 0)
				break;
		}
		p1[0] = (PQP_REAL)vertices[c1](0);
		p1[1] = (PQP_REAL)vertices[c1](1);
		p1[2] = (PQP_REAL)vertices[c1](2);
		p2[0] = (PQP_REAL)vertices[c2](0);
		p2[1] = (PQP_REAL)vertices[c2](1);
		p2[2] = (PQP_REAL)vertices[c2](2);
		p3[0] = (PQP_REAL)vertices[c3](0);
		p3[1] = (PQP_REAL)vertices[c3](1);
		p3[2] = (PQP_REAL)vertices[c3](2);
		if(tri->neighbor_vertex[0] >= 0)
		{
			q1[0] = (PQP_REAL)vertices[tri->neighbor_vertex[0]](0);
			q1[1] = (PQP_REAL)vertices[tri->neighbor_vertex[0]](1);
			q1[2] = (PQP_REAL)vertices[tri->neighbor_vertex[0]](2);
		}
		if(tri->neighbor_vertex[1] >= 0)
		{
			q2[0] = (PQP_REAL)vertices[tri->neighbor_vertex[1]](0);
			q2[1] = (PQP_REAL)vertices[tri->neighbor_vertex[1]](1);
			q2[2] = (PQP_REAL)vertices[tri->neighbor_vertex[1]](2);
		}
		if(tri->neighbor_vertex[2] >= 0)
		{
			q3[0] = (PQP_REAL)vertices[tri->neighbor_vertex[2]](0);
			q3[1] = (PQP_REAL)vertices[tri->neighbor_vertex[2]](1);
			q3[2] = (PQP_REAL)vertices[tri->neighbor_vertex[2]](2);
		}
		int vertex_id[3] = { c1, c2, c3 };
		model->AddTri(p1, p2, p3, q1, q2, q3, vertex_id, tri->neighbor_tri, ti->id());
#ifdef VERBOSE
		cerr << "triangle " << ti->id() << ": [" << c1 << ", " << c2 << ", " << c3 << "], neighbors = [" << tri->neighbor_tri[0] << ", " << tri->neighbor_tri[1] << ", " << tri->neighbor_tri[2] << "]" << endl;
		cerr << vertices[c1] << endl;
		cerr << vertices[c2] << endl;
		cerr << vertices[c3] << endl;
#endif
	}
	return 0;
}

/**
 * ColInfo functions
 */
void ColInfo::add_pair(ColPair* p)
{
	if(n_pairs == n_allocated_pairs) allocate_pair(n_allocated_pairs + 10);
	pairs[n_pairs] = p;
	n_pairs++;
}

void ColInfo::add_model(ColModel* m)
{
	if(n_models == n_allocated_models) allocate_model(n_allocated_models + 10);
	models[n_models] = m;
	n_models++;
	n_total_tri += m->n_triangle;
}

int ColInfo::AddCharPairs(const char* char1, const char* char2, Chain* chain, SceneGraph* sg)
{
	cerr << "AddCharPairs(" << char1 << ", " << char2 << ")" << endl;
	add_char_pairs(chain->Root(), char1, char2, chain, sg);
	return n_pairs;
}

int ColInfo::add_char_pairs(Joint* cur, const char* char1, const char* char2, Chain* chain, SceneGraph* sg)
{
	if(!cur) return 0;
	char* charname;
	if((charname = cur->CharName()) && !strcmp(charname, char1))
	{
		add_char_pairs(cur, chain->Root(), char2, sg);
	}
	add_char_pairs(cur->brother, char1, char2, chain, sg);
	add_char_pairs(cur->child, char1, char2, chain, sg);
	return 0;
}

int ColInfo::add_char_pairs(Joint* j1, Joint* j2, const char* char2, SceneGraph* sg)
{
	if(!j2) return 0;
	char* charname;
	if((charname = j2->CharName()) && !strcmp(charname, char2) && j1 != j2)
	{
		add_joint_pair(j1, j2, sg);
	}
	add_char_pairs(j1, j2->brother, char2, sg);
	add_char_pairs(j1, j2->child, char2, sg);
	return 0;
}

int ColInfo::AddJointPair(const char* joint1, const char* joint2, Chain* chain, SceneGraph* sg)
{
	Joint* j1, *j2;
	j1 = chain->FindJoint(joint1);
	j2 = chain->FindJoint(joint2);
	if(!j1 || !j2) return 0;
	if(j1 == j2) return 0;
	add_joint_pair(j1, j2, sg);
	return n_pairs;
}

int ColInfo::add_joint_pair(Joint* j1, Joint* j2, SceneGraph* sg)
{
	if(Pair(j1, j2)) return 0;
	ColModel* m1, *m2;
	m1 = Model(j1);
	if(!m1)
	{
		m1 = new ColModel(j1, sg);
		add_model(m1);
	}
	m2 = Model(j2);
	if(!m2)
	{
		m2 = new ColModel(j2, sg);
		add_model(m2);
	}
	if(m1->n_triangle == 0 || m2->n_triangle == 0) return -1;
	if( (j1->parent == j2 && j1->n_dof == 0) || (j2->parent == j1 && j2->n_dof == 0) )
		return 0;
	cerr << "add joint pair: " << j1->name << ", " << j2->name << endl;
	ColPair* p = new ColPair(m1, m2);
	add_pair(p);
	return 0;
}

ColModel* ColInfo::AddModel(Joint* jref, SceneGraph* sg)
{
	ColModel* ret = Model(jref);
	if(!ret)
	{
		ret = new ColModel(jref, sg);
		if(ret->n_triangle > 0)
			add_model(ret);
		else
		{
			delete ret;
			ret = 0;
		}
	}
	return ret;
}
