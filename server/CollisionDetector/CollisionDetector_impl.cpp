// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */
/** @file CollisionDetector/server/CollisionDetector_impl.cpp
 * Implementation of CollisionDetector_impl and CollisionDetectorFactory_impl
 *
 * @version 0.2
 * @date 2002/01/15
 *
 */

#include "CollisionDetector_impl.h"
#include "CdCache.h"

#include <time.h>
#include <iostream>
#include <string>
#include <vector>

using namespace std;
using namespace OpenHRP;

//#define COLLISIONDETECTOR_DEBUG

CollisionDetector_impl::CollisionDetector_impl
(
 CORBA_ORB_ptr        orb,
 CdCharCache*         cache
 ) : orb_(CORBA_ORB::_duplicate(orb))
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::CollisionDetector_impl()" << endl;
#endif
    cache_ = cache;
    scene_ = new CdScene();
}

CollisionDetector_impl::~CollisionDetector_impl()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::~CollisionDetector_impl()" << endl;
#endif
    delete scene_;
}

void
CollisionDetector_impl::destroy()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::destroy()" << endl;
#endif
    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa -> servant_to_id(this);
    poa -> deactivate_object(id);
}




void CollisionDetector_impl::addModel(
	const char* charName,
	BodyInfo_ptr binfo)
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::addModel() " << charName <<endl;
#endif

// #define READTRI_COUNT 1000

    CdModelCache* cachedModel;

    //既にキャッシュに入ってる？
    CORBA::String_var url = binfo->url();

    if( cache_->exist( url ) )
	{
        cachedModel = cache_->getChar( url );
    }
	else
	{
        cerr << "creating Cd object..." << endl;

        //ロード
        LinkInfoSequence_var jList = binfo->links();
        cachedModel = new CdModelCache();

        double p[3][3];
        // DblSequence tris;
		vector<double> triangles;
        CORBA::String_var jName;

        // ジョイントごとに三角形集合追加
		for( unsigned int i = 0 ; i < jList->length() ; ++i )
		{
			jName = jList[i].name;
            int ntri = 0;
	
			// このリンクのShapeInfoの頂点座標群を取得する
			triangles = getTrianglesOfLink( i, binfo );

			// 頂点数が9の倍数でなければ，
			//  ( ∵三角メッシュなので，頂点数が9の倍数であるはず)
			if( triangles.size() % 9 )
			{
				// ##### [TODO] ##### エラーを投げる
				cerr << "There is a probrem in the number of vertices.";
				continue;
			}
			size_t trianglesNumber = triangles.size() / 9;

			CdModelSet* modelSet = 0;

			// 空ジョイントでははないか
			if( 0 < trianglesNumber )
			{
				modelSet = new CdModelSet();
				modelSet->linkIndex = i;

				for( size_t j = 0 ; j < trianglesNumber ; j++ )
				{
					p[0][0] = triangles[j*9+0];
					p[0][1] = triangles[j*9+1];
					p[0][2] = triangles[j*9+2];
					p[1][0] = triangles[j*9+3];
					p[1][1] = triangles[j*9+4];
					p[1][2] = triangles[j*9+5];
					p[2][0] = triangles[j*9+6];
					p[2][1] = triangles[j*9+7];
					p[2][2] = triangles[j*9+8];
// ##### DEBUG
//cout << "( " << p[0][0] << ", " << p[0][1] << ", " << p[0][2] << ") ";
//cout << "( " << p[1][0] << ", " << p[1][1] << ", " << p[1][2] << ") ";
//cout << "( " << p[2][0] << ", " << p[2][1] << ", " << p[2][2] << ") ";
//if( ! ( j % 9 ) ) cout << endl;
// ##### DEBUG
					modelSet->addTriangle( p[0], p[1], p[2] );					

				}
				modelSet->endModel();
			}

			//キャラクタにジョイントを追加
			cachedModel->addModel(jName, modelSet);
			
			cerr << jName << " has "<< trianglesNumber << " triangles." << endl;
        }

		cerr << "finished." << endl;

		//キャッシュにキャラクタを追加
		cache_->addChar(url,cachedModel);

	}

	scene_->addChar(charName, new CdChar(cachedModel,charName) );

}


vector<double> CollisionDetector_impl::getTrianglesOfLink(
	int linkIndex,
	BodyInfo_ptr binfo )
{
	vector<double> triangles;

	ShapeInfoSequence_var	shapes				= binfo->shapes(); 
	AllLinkShapeIndices_var	allLinkShapeIndices	= binfo->linkShapeIndices(); 

	// linkIndex番目のリンクのShapeIndices
	ShortSequence shapeIndices = allLinkShapeIndices[linkIndex];
	
	// このLinkのShapeInfoの数分ループする
	for( size_t i = 0 ; i < shapeIndices.length() ; i++ )
	{
		// shapeIndices中で指定された shapeInfo のインデックスは
		short shapeIndex = shapeIndices[i];
		ShapeInfo shapeInfo = shapes[shapeIndex];

		// trianglesが指し示す頂点順にループする
		for( size_t t = 0 ; t < shapeInfo.triangles.length() ; t++ )
		{
			// 頂点番号は，
			long vertexIndex = shapeInfo.triangles[t];

			// 頂点座標は，
			//  [NOTE] verticesには XYZXYZ...のように頂点座標が格納されているので，
			//         頂点番号(vertexIndex)に対応する(vertices中の)頂点座標の配列添字は
			//             X座標 = vertexIndex*3
			//             Y座標 = vertexIndex*3+1
			//             Z座標 = vertexIndex*3+2
			//         で取得(アクセス)可能である。
			double x = static_cast<double>(shapeInfo.vertices[vertexIndex*3+0]);
			double y = static_cast<double>(shapeInfo.vertices[vertexIndex*3+1]);
			double z = static_cast<double>(shapeInfo.vertices[vertexIndex*3+2]);

			triangles.push_back( x );
			triangles.push_back( y );
			triangles.push_back( z );
			
			// 上記の処理は以下記述が可能であるが，解り易くする目的で 3座標にして記述した。
			//	for( int c = 0 ; c < 3 ; c++ )
			//	{
			//		triangles.push_back( static_cast<double>(shapeInfo.vertices[vertexIndex*3+c]) );
			//	}
		}
	}

	return triangles;
}


#if 0 // 改造前

void CollisionDetector_impl::addModel(
	const char* charName,
	BodyInfo_ptr binfo)
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::addModel() " << charName <<endl;
#endif

#define READTRI_COUNT 1000

    CdModelCache* cachedModel;

    //既にキャッシュに入ってる？
    CORBA::String_var url = binfo->url();

    if(cache_->exist(url)){
        cachedModel = cache_->getChar(url);
    }else{
        cerr << "creating Cd object..." << endl;

        //ロード
        LinkInfoSequence_var jList = binfo->links();
        cachedModel = new CdModelCache();

        double p[3][3];
        DblSequence tris;
        CORBA::String_var jName;

        // ジョイントごとに三角形集合追加
		for( unsigned int i = 0 ; i < jList->length() ; ++i )
		{
			jName = jList[i].name;
            int ntri = 0;

// ###### [TODO] readTriangles は NewModelLoader には無い。
//               ShapeInfo から生成する同等の処理をここに。 
//            tris = jList[i]->readTriangles(ntri, READTRI_COUNT);

			// linkShapeIndices に全リンクに属するSahpeInfo (shapesのIndex)
			//が、リンク順にセットされているので、リンク順を指定して取得する。
			AllLinkShapeIndices_var alllinkShapeIndices = binfo->linkShapeIndices(); 

			//１リンクのShape数
			int shapeCountsByLink = alllinkShapeIndices[i].length();

			//第 i リンクのShapeIndices のシーケンスデータ
			ShortSequence shapeIndicesSeqByLink = alllinkShapeIndices[i];
			short shapeIndex;		// 処理すべき ShapeInfoの shapeIndicesSeqByLink の中のIndex
			int triCountsSum = 0;	// tri データサイズ
			int triInsertIndex = 0;	// tri データ 挿入位置

			// １リンクのShape数分 ループし 三角メッシュデータを tris[] にセットする。
			for(unsigned int k=0; k < shapeCountsByLink; k++){

				// (triangle)面情報の 第 k 頂点の shapes中の Index取得する。
				shapeIndex = shapeIndicesSeqByLink[k];	
				// BodyInfoから shapesデータ列取得
				ShapeInfoSequence_var shapeIndicesByLink = binfo->shapes(); 
				// shapesデータ列から第 k 番目要素の ShapeInfo(k) 取得
				ShapeInfo shapeInfoByLink = shapeIndicesByLink[k];

				// ShapeInfo(k) の三角メッシュ数×３ を取得する
				int meshVertexCounts = shapeInfoByLink.triangles.length();

				// ShapeInfo(k)の三角パッチデータ数（三角メッシュ数×３×３）算出
				int triCounts = meshVertexCounts * 3;

				// tris の長さ設定
				triInsertIndex = triCountsSum;	//tri データ 挿入位置
				triCountsSum += triCounts;		//tri データ リサイズサイズ
				tris.length( triCountsSum );

				// trisに頂点ごとに（XYZ)座標値格納する
				for(unsigned int m=0; m < meshVertexCounts; m++){ 
					int vNo = shapeInfoByLink.triangles[m];
					int vindex_X = vNo * 3;
					tris[triInsertIndex + m + 0] = shapeInfoByLink.vertices[vindex_X];
					tris[triInsertIndex + m + 1] = shapeInfoByLink.vertices[vindex_X + 1];
					tris[triInsertIndex + m + 2] = shapeInfoByLink.vertices[vindex_X + 2];
				}
			}

			CdModelSet* modelSet = 0;
			
            //空ジョイントでははないか
			if(tris.length())
			{
                modelSet = new CdModelSet();
				modelSet->linkIndex = i;
				
				int trisCnts = tris.length()/9;

                while (trisCnts > 0){
                    ntri += tris.length()/9;	//新IDLでは、未使用
					trisCnts -= 1;
                    for(unsigned int j=0;j<tris.length();j+=9){
                        p[0][0]=tris[j+0];
                        p[0][1]=tris[j+1];
                        p[0][2]=tris[j+2];
                        p[1][0]=tris[j+3];
                        p[1][1]=tris[j+4];
                        p[1][2]=tris[j+5];
                        p[2][0]=tris[j+6];
                        p[2][1]=tris[j+7];
                        p[2][2]=tris[j+8];
                        modelSet->addTriangle(p[0],p[1],p[2]);					
                    }
                    //旧trisは自動的に解放される

					// ###### [TODO] readTriangles は NewModelLoader には無い。
					//               ShapeInfo から生成する同等の処理をここに。
                    // tris=jList[i]->readTriangles(ntri, READTRI_COUNT);
                }
                modelSet->endModel();
			}

			//キャラクタにジョイントを追加
			cachedModel->addModel(jName, modelSet);
			
            cerr << jName << " has "<< ntri << " tris." << endl;
        }

        cerr << "finished." << endl;

        //キャッシュにキャラクタを追加
        cache_->addChar(url,cachedModel);

    }

    scene_->addChar(charName, new CdChar(cachedModel,charName) );

}

#endif // 改造前



void CollisionDetector_impl::addCollisionPair(const LinkPair& colPair,
                                              CORBA::Boolean convexsize1,
                                              CORBA::Boolean convexsize2)
{
    const char* charName1 = colPair.charName1;
    const char* charName2 = colPair.charName2;
    const char* jointName1 = colPair.linkName1;
    const char* jointName2 = colPair.linkName2;
    //#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::addCollisionPair("
         << charName1 <<  "::" << jointName1 << ", "
         << charName2 <<  "::" << jointName2 << ")" << endl;
    //#endif

	CdJoint* joint1 = 0;
	CdJoint* joint2 = 0;

    CdChar* char1 = scene_->getChar(charName1);
    if (!char1){
        cerr << "CollisionDetector_impl::addCollisionPair : Character not found("
             << charName1 << ")" << endl;
    } else {
	    joint1 = char1->getJoint(jointName1);
    	if (!joint1){
        	cerr << "CollisionDetector_impl::addCollisionPair : Joint not found("
            	 << charName1 << ","
             	<< jointName1 << ")" << endl;
	    }
    }

    CdChar* char2 = scene_->getChar(charName2);
    if (!char2){
        cerr << "CollisionDetector_impl::addCollisionPair : Character not found("
             << charName2 << ")" << endl;
    } else {
    	joint2 = char2->getJoint(jointName2);
    	if (!joint2){
        	cerr << "CollisionDetector_impl::addCollisionPair : Joint not found("
            	 << charName2 << ","
             	<< jointName2 << ")" << endl;
    	}
    }

    static const bool NOT_SKIP_EMPTY_JOINT = true;

    if(NOT_SKIP_EMPTY_JOINT || (joint1 && joint2)){
    	scene_->addCheckPair(new CdCheckPair(joint2, joint1));
    }

}

void CollisionDetector_impl::removeCollisionPair(const LinkPair& colPair)
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::removeCollisionPair()" << endl;
#endif
}


//ジョイント位置セット
void CollisionDetector_impl::_setCharacterData
(
 const CharacterPositionSequence& characterPositions
 )
{
    if (joints.size() == 0){ //first time
        for (unsigned int i=0; i < characterPositions.length(); i++){
            const CharacterPosition& characterPosition = characterPositions[i];
            CdChar *rChar = scene_->getChar(characterPosition.characterName);
            if (rChar){
				const LinkPositionSequence& linkPositions = characterPosition.linkPositions;
                for (unsigned int j=0; j < linkPositions.length(); j++){
                    CdJoint *rJoint = rChar->getJoint(j);
                    if (rJoint){
                        joints.push_back(rJoint);
                    }else{
                        joints.push_back(NULL);
                    }
                }
            } else {
                cerr << "CollisionDetector_impl::_setCharacterData : Character not found(" << characterPosition.characterName << ")" << endl;
            }
        }
    }

    //ジョイントの位置姿勢をセット
    vector<CdJoint *>::iterator it = joints.begin();
    for(unsigned int i = 0; i < characterPositions.length(); i++){
		const LinkPositionSequence& linkPositions = characterPositions[i].linkPositions;
        for (unsigned int j=0; j < linkPositions.length(); j++, it++){
			CdJoint* cdJoint = *it;
            if (cdJoint){
                const LinkPosition& linkPosition = linkPositions[j];
				const DblArray3& po = linkPosition.p;
				const DblArray9& Ro = linkPosition.R;
				double* p    = cdJoint->translation_;
				double (*R)[3] = cdJoint->rotation_;
				p[0] = po[0]; p[1] = po[1]; p[2] = po[2];
				R[0][0] = Ro[0]; R[0][1] = Ro[1]; R[0][2] = Ro[2];
				R[1][0] = Ro[3]; R[1][1] = Ro[4]; R[1][2] = Ro[5];
				R[2][0] = Ro[6]; R[2][1] = Ro[7]; R[2][2] = Ro[8];
            }
        }
    }
}

//下請け
int CollisionDetector_impl::_contactIntersection
(
 CdCheckPair* rPair
 )
{
    collision_data* pair;
    int ret;
    int i;

    rPair->collide(&ret,&pair,CD_FIRST_CONTACT);

    // この段階では接触点数分からないので、接触点数をカウントする。
    int npoints = 0;
    for (i = 0; i < ret; ++i) {
        npoints += pair[i].num_of_i_points;
    }
    delete pair;
    return npoints;
}
//下請け
void CollisionDetector_impl::_contactDetermination(CdCheckPair* rPair, Collision&  out_collision)
{
    int ret;

	collision_data *cdata;
    rPair->collide(&ret, &cdata);

    // この段階では接触点数分からないので、接触点数をカウントする。
    int npoints = 0;
    for (int i = 0; i < ret; i++) {
        for (int j = 0; j<cdata[i].num_of_i_points; j++){
            if (cdata[i].i_point_new[j]) npoints ++;
        }
    }
    if (npoints > 0){
        out_collision.points.length(npoints);

        int idx = 0;
        for (int i = 0; i < ret; i++) {
			collision_data& cd = cdata[i];
            for (int j=0; j < cd.num_of_i_points; j++){
                if (cd.i_point_new[j]){
					CollisionPoint& point = out_collision.points[idx];
                    for(int k=0; k < 3; k++){
                        point.position[k] = cd.i_points[j][k];
					}
                    for(int k=0; k < 3; k++){
                        point.normal[k]   = cd.n_vector[k];
                    }
                    point.idepth = cd.depth;
                    idx++;
                }
            }
        }
    }
    if (rPair->joint_[0]){
        out_collision.pair.charName1 = CORBA::string_dup(rPair->joint_[0]->parent_->name_.c_str());    // パス名1
        out_collision.pair.linkName1 = CORBA::string_dup(rPair->joint_[0]->name_.c_str());    // パス名1
    }
    if (rPair->joint_[1]){
        out_collision.pair.charName2 = CORBA::string_dup(rPair->joint_[1]->parent_->name_.c_str());    // パス名2
        out_collision.pair.linkName2 = CORBA::string_dup(rPair->joint_[1]->name_.c_str());    // パス名2
    }
}


CORBA::Boolean
CollisionDetector_impl::queryContactDeterminationForDefinedPairs
(
 const CharacterPositionSequence& characterPositions,
 CollisionSequence_out collisions
 )
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::queryContactDeterminationForDefinedPairs()" << endl;
#endif
    _setCharacterData(characterPositions);

    long unsigned int numPair = scene_->getNumCheckPairs();

    collisions = new CollisionSequence;
    collisions->length(numPair);
    bool flag = false;
    for(long unsigned int pCount = 0 ; pCount < numPair ; ++pCount){
        CdCheckPair* rPair = scene_->getCheckPair(pCount);
        _contactDetermination(rPair, collisions[pCount]);
        if (collisions[pCount].points.length() > 0) flag = true;
    }

    //cerr << "CollisionDetector_impl::checkCollision(2)" << endl;

//###### DEBUG
	if( flag )
	{
		cout << "##### Collision!!!!" << endl;
	}

    return flag;
}

CORBA::Boolean
CollisionDetector_impl::queryContactDeterminationForGivenPairs
(
 const LinkPairSequence& checkPairs,
 const CharacterPositionSequence& characterPositions,
 CollisionSequence_out collisions
 )
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::queryContactDeterminationForGivenPairs()" << endl;
#endif

    _setCharacterData(characterPositions);

    vector<CdCheckPair*> rPairSeq;

    //臨時ペア生成
    for(unsigned int i=0;i<checkPairs.length();++i){
        const char* charName1 = checkPairs[i].charName1;
        const char* charName2 = checkPairs[i].charName2;
        const char* jointName1 = checkPairs[i].linkName1;
        const char* jointName2 = checkPairs[i].linkName2;

        CdChar* char1 = scene_->getChar(charName1);
        CdChar* char2 = scene_->getChar(charName2);

        if(char1 && char2){
            CdJoint* joint1 = char1->getJoint(jointName1);
            CdJoint* joint2 = char2->getJoint(jointName2);
            if(joint1 && joint2){
                rPairSeq.push_back(
                                   new CdCheckPair(
												   joint2,
												   joint1
												   )
                                   );
            }
        }

    }

    collisions = new CollisionSequence;
    collisions->length(rPairSeq.size());
    bool flag = false;
    // check
    unsigned long int pCount;
    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        CdCheckPair* rPair = rPairSeq[pCount];
        _contactDetermination(rPair,collisions[pCount]);
        if (collisions[pCount].points.length() > 0) flag = true;
    }
    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        // delete
        CdCheckPair* rPair = rPairSeq[pCount];
        delete rPair;
    }

    //cerr << "CollisionDetector_impl::checkCollision(2)" << endl;

    return flag;
}

CORBA::Boolean CollisionDetector_impl::queryIntersectionForDefinedPairs
(
 CORBA::Boolean checkAll,
 const CharacterPositionSequence& characterPositions,
 LinkPairSequence_out collidedPairs
 )
{
    _setCharacterData(characterPositions);

    int numPair = scene_->getNumCheckPairs();


    vector<CdCheckPair*> contactPairs;

    bool flag = false;
    for(int pCount = 0 ; pCount < numPair ; ++pCount){
        CdCheckPair* rPair = scene_->getCheckPair(pCount);
        int ret = _contactIntersection(rPair);
        if (ret){
            contactPairs.push_back(rPair);
        }
        flag = (flag || ret);
        if(!checkAll && flag){
            break;
        }
    }

    //情報作成

    //cerr << "CollisionDetector_impl::checkCollision(2)" << endl;


    collidedPairs = new LinkPairSequence();
    collidedPairs->length(contactPairs.size());

    for(unsigned int i=0;i<contactPairs.size();++i){
        CdCheckPair* rPair = contactPairs[i];
        (*collidedPairs)[i].charName1 = CORBA::string_dup(rPair->joint_[0]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName1 = CORBA::string_dup(rPair->joint_[0]->name_.c_str());
        (*collidedPairs)[i].charName2 = CORBA::string_dup(rPair->joint_[1]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName2 = CORBA::string_dup(rPair->joint_[1]->name_.c_str());
    }

    return flag;
}

CORBA::Boolean CollisionDetector_impl::queryIntersectionForGivenPairs
(
 CORBA::Boolean checkAll,
 const LinkPairSequence& checkPairs,
 const CharacterPositionSequence& characterPositions,
 LinkPairSequence_out collidedPairs
 )
{
#ifdef COLLISIONDETECTOR_DEBUG
    //cerr << "CollisionDetector_impl::queryIntersectionForGivenPairs()" << endl;
#endif
    _setCharacterData(characterPositions);

    vector<CdCheckPair*> rPairSeq;

    //臨時ペア生成
    unsigned int i;
    for(i=0;i<checkPairs.length();++i){
        const char* charName1  = checkPairs[i].charName1;
        const char* charName2  = checkPairs[i].charName2;
        const char* jointName1 = checkPairs[i].linkName1;
        const char* jointName2 = checkPairs[i].linkName2;

        //cerr << checkPairs[i].charName1 << "." <<checkPairs[i].jointName1<< "-";
        //cerr << checkPairs[i].charName2 << "." <<checkPairs[i].jointName2<<endl;

        CdChar* char1 = scene_->getChar(charName1);
        CdChar* char2 = scene_->getChar(charName2);

        if(char1 && char2){
            CdJoint* joint1 = char1->getJoint(jointName1);
            CdJoint* joint2 = char2->getJoint(jointName2);
            if(joint1 && joint2){
                rPairSeq.push_back(
                                   new CdCheckPair(
												   joint2,
												   joint1
												   )
                                   );
            }
            //cerr << "create" ;
        }
        //cerr << endl;

    }


    vector<CdCheckPair*> contactPairs;

    bool flag = false;
    unsigned int pCount;
    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        CdCheckPair* rPair = rPairSeq[pCount];
        int ret = _contactIntersection(rPair);
        if (ret){
            contactPairs.push_back(rPair);
            //cerr << "insects! "<< endl;
        }
        flag = (flag || ret);
        if(!checkAll && flag){
            break;
        }
    }

    //情報作成

    //cerr << "CollisionDetector_impl::checkCollision(2)" << endl;


    collidedPairs = new LinkPairSequence();
    collidedPairs->length(contactPairs.size());

    for(i=0;i<contactPairs.size();++i){
        CdCheckPair* rPair = contactPairs[i];
        (*collidedPairs)[i].charName1 = CORBA::string_dup(rPair->joint_[0]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName1 = CORBA::string_dup(rPair->joint_[0]->name_.c_str());
        (*collidedPairs)[i].charName2 = CORBA::string_dup(rPair->joint_[1]->parent_->name_.c_str());
        (*collidedPairs)[i].linkName2 = CORBA::string_dup(rPair->joint_[1]->name_.c_str());
    }

    for(pCount = 0 ; pCount < rPairSeq.size() ; ++pCount){
        // delete
        CdCheckPair* rPair = rPairSeq[pCount];
        delete rPair;
    }

    return flag;

}


void CollisionDetector_impl::clearCache(const char* url)
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::clearCache()" << endl;
#endif
    cache_->removeChar(url);
}

void CollisionDetector_impl::clearAllCache()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetector_impl::clearAllCache()" << endl;
    cache_->removeAllChar();
#endif
}

CollisionDetectorFactory_impl::CollisionDetectorFactory_impl
(
 CORBA_ORB_ptr   orb
 ) : orb_(CORBA_ORB::_duplicate(orb))
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetectorFactory_impl::CollisionDetectorFactory_impl()" << endl;
#endif
    cache_ = new CdCharCache();
}

CollisionDetectorFactory_impl::~CollisionDetectorFactory_impl()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetectorFactory_impl::~CollisionDetectorFactory_impl()" << endl;
#endif
    PortableServer::POA_var poa = _default_POA();
    PortableServer::ObjectId_var id = poa -> servant_to_id(this);
    poa -> deactivate_object(id);
    delete cache_;
}

CollisionDetector_ptr
CollisionDetectorFactory_impl::create()
{
#ifdef COLLISIONDETECTOR_DEBUG
    cerr << "CollisionDetectorFactory_impl::create()" << endl;
#endif

    CollisionDetector_impl* collisionDetectorImpl = new CollisionDetector_impl(orb_,cache_);

    PortableServer::ServantBase_var collisionDetectorrServant = collisionDetectorImpl;
    PortableServer::POA_var poa_ = _default_POA();
    PortableServer::ObjectId_var id =
        poa_ -> activate_object(collisionDetectorImpl);
    return collisionDetectorImpl -> _this();
}

void CollisionDetectorFactory_impl::shutdown()
{
    orb_->shutdown(false);
}
