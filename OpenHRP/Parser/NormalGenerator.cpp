/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*!
  @file CalculateNormal.cpp
  @author Y.TSUNODA
  @author Shin'ichiro Nakaoka
*/

#include "NormalGenerator.h"

#include <iostream>
#include <math.h>

using namespace std;
using namespace boost;
using namespace OpenHRP;

static const double PI = 3.14159265358979323846;


/*!
  @if jp
  @brief      全ての頂点の法線ベクトルを計算

  引数で与えられた頂点群に対応する法線ベクトルを計算する<BR>
  計算した法線は メンバ変数 _normalsOfVertex に格納する<BR>
  _normalsOfVertexの法線は，引数vertexListの頂点の順と対応している<BR><BR>
  ※ この関数を呼び出した際に，全ての三角メッシュの法線 _normalsOfMesh が計算されて
  いないと見做した場合は，_normalsOfMeshを計算する。<BR>

  @param vertexList 頂点群
  @param triangleList 三角メッシュ群
  @param creaseAngle 折り目角度
  @return bool true:成功 / false:失敗
  @endif
*/
bool CalculateNormal::calculateNormalsOfVertex(
    const vector<Vector3>& vertexList,
    const vector<vector3i>& triangleList,
    double creaseAngle)
{
    size_t normalsOfMeshNum = _normalsOfMesh.size();
    size_t triangleNum = triangleList.size();

    // 三角メッシュの面の法線が未計算ならば
    //   (つまり，三角メッシュの法線数が0以下 或いは 三角メッシュ数と一致していなければ，)
    if( ( normalsOfMeshNum <= 0 ) && ( normalsOfMeshNum != triangleNum ) ) {
        // 全ての三角メッシュの法線 _normalsOfMesh を計算する
        calculateNormalsOfMesh( vertexList, triangleList );
    }
    
    vector<Vector3> tmpNormalsOfVertex;	// 一時作業用 頂点の法線計算結果を格納
    Vector3 normal;
    Vector3 tmpnormal;
    Vector3 sum;
    int count;
    
    // 頂点・三角メッシュ対応リストを順に辿る
    for( size_t vertexIndex = 0 ; vertexIndex < vertexContainedInMeshList_.size() ; ++vertexIndex ){
        sum = 0.0, 0.0, 0.0;
        count = 0;
        
        vector<long> meshIndexList = vertexContainedInMeshList_.at( vertexIndex );
        
        for( size_t i = 0 ; i < meshIndexList.size(); ++i ) {
            long meshIndex = meshIndexList.at( i );
            sum += _normalsOfMesh[meshIndex];
            count++;
        }
        
        // 頂点の法線ベクトルを計算
        tmpnormal = sum[0] / count, sum[1] / count, sum[2] / count;
        normal= tvmet::normalize( tmpnormal );
        
        tmpNormalsOfVertex.push_back( normal );
    }
    
    
    // 頂点群の法線リストをクリアする
    _normalsOfVertex.clear();
    _normalIndex.clear();
    
    // 法線リストのインデックス (法線リストの何番かを示す)
    int normalVertexIndex = 0;
    
    // メッシュを順に辿る
    for( size_t i = 0 ; i < triangleNum ; ++i )	{
        // 三角メッシュリスト i番目のメッシュの面法線
        Vector3 nM = _normalsOfMesh.at( i );
        
        // 三角メッシュリスト i番目のメッシュ
        vector3i triangle = triangleList.at( i );
	
        vector3i normalIndex;		// テンポラリ作業用
        
        // 三角メッシュを構成する頂点を順に辿る
        for( int j = 0 ; j < 3 ; ++j ){
            // 頂点インデックス
            int vertexIndex = triangle[j];
            
            // この頂点の法線は，
            Vector3 nV = tmpNormalsOfVertex.at( vertexIndex );
            
            // 頂点の法線ベクトルと，面の法線ベクトルのなす角は
            double angle = acos( ( nM[0] * nV[0] + nM[1] * nV[1] + nM[2] * nV[2] ) 
                                 / ( norm2(nM) * norm2(nV)));
                
                // 折り目角度と比較する
                if( angle <= creaseAngle ) {
                    // 頂点の法線ベクトルを採用する
                    _normalsOfVertex.push_back( nV );
                } else {
                    // メッシュ(面)の法線ベクトルを採用する
                    _normalsOfVertex.push_back( nM );
                }
            
            //
            normalIndex[j] = normalVertexIndex;
            
            //  
            normalVertexIndex++;
        }
        
        // 
        _normalIndex.push_back( normalIndex );
    }
    
    return true;
}




//==================================================================================================
/*!
  @if jp

    @brief      全ての三角メッシュの法線ベクトルを計算

    @note       引数で与えられた三角メッシュ群に対応する法線ベクトルを計算する<BR>
				計算した法線は メンバ変数 _normalsOfMesh に格納する<BR>
				_normalsOfMeshの法線は，引数triangleListの三角メッシュの順と対応している<BR>

    @date       2008-04-11 Y.TSUNODA <BR>

    @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
CalculateNormal::calculateNormalsOfMesh( 
	const vector<Vector3>& vertexList,		//!< 頂点群
	const vector<vector3i>& triangleList )		//!< 三角メッシュ群
{
	// 三角メッシュ群の法線リスト，頂点・三角メッシュ対応リストをクリアする
	_normalsOfMesh.clear();
	vertexContainedInMeshList_.clear();

	// 頂点・三角メッシュ対応リストのサイズを 頂点数 確保する 
	vertexContainedInMeshList_.resize( vertexList.size() );

	// 三角メッシュ数
	size_t triangleNum = triangleList.size();

	// 三角メッシュを順に辿る
	for( size_t i = 0 ; i < triangleNum ; ++i )
	{
		vector3i triangle = triangleList.at( i );

		// 法線を計算する
		Vector3 normal;
		normal = _calculateNormalOfTraiangleMesh( vertexList[triangle[0]],
												  vertexList[triangle[1]],
												  vertexList[triangle[2]] );

		// 三角メッシュ群の法線リストに追加する
		_normalsOfMesh.push_back( normal );

		// 頂点・三角メッシュ対応リストに追加する
		//   頂点 triangle[j] は，i番目の三角メッシュに含まれる
		for( int j = 0 ; j < 3 ; ++j )
		{
			vertexContainedInMeshList_.at( triangle[j] ).push_back( i );
		}
	}

	return true;
}





//==================================================================================================
/*!
  @if jp

    @brief      三角メッシュの法線ベクトル計算

    @note       引数で与えられた三頂点で構成される三角メッシュの法線ベクトルを計算する<BR>

    @date       2008-04-11 Y.TSUNODA <BR>

    @return     Vector3 法線ベクトル

  @endif
*/
//==================================================================================================
Vector3
CalculateNormal::_calculateNormalOfTraiangleMesh(
	Vector3 a,		//!< 三角メッシュを構成する頂点1
	Vector3 b,		//!< 三角メッシュを構成する頂点2
	Vector3 c )	//!< 三角メッシュを構成する頂点3
{
	Vector3 normal;
	normal = tvmet::normalize( tvmet::cross( b - a, c - a ) );

	return normal;
}



