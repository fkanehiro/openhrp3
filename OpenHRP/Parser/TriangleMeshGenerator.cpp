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
  @file TriangleMeshGenerator.cpp
  @author Y.TSUNODA
  @author Shin'ichiro Nakaoka
*/

#include "TriangleMeshGenerator.h"

#include <iostream>
#include <math.h>

using namespace std;
using namespace boost;
using namespace OpenHRP;

static const double PI = 3.14159265358979323846;


TriangleMeshGenerator::TriangleMeshGenerator()
{
    type_ = S_UNKNOWN_TYPE;
    vertexList_.clear();
    triangleList_.clear();
    flgUniformIndexedFaceSet_ = true;
    flgMessageOutput_ = true;
}



/*!
  @if jp
  IndexedFaceSet整形フラグ設定

  IndexedFaceSet(入力データ)が三角メッシュであることが保障されたデータの場合，
  整形処理をせず入力データをそのまま出力する。

  @param val true : 整形処理をする，false : 整形処理をしない<BR>
  @return bool フラグに設定された値
  @endif
*/
bool TriangleMeshGenerator::setFlgUniformIndexedFaceSet(bool val)
{
    return (flgUniformIndexedFaceSet_ = val);
}




//==================================================================================================
/*!
  @if jp

  @brief      整形処理 (ModelNodeSet内のShape)

  @note       整形対象として引数で与えられたModelNodeSet中の，Shape(VRMLプリミティブ形状)を
  整形処理にて三角形メッシュベースの統一的な幾何形状に変換する<BR>

  @date       2008-04-03 Y.TSUNODA <BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniform(
    ModelNodeSet& modelNodeSet )
{
    // JointNode数を取得する
    int numJointNodes = modelNodeSet.numJointNodes();
	
    if( 0 < numJointNodes )
	{
            int currentIndex = 0;

            // JointNode を再帰的に辿り，LinkInfoを生成する
            JointNodeSetPtr rootJointNodeSet = modelNodeSet.rootJointNodeSet();
            _traverseJointNode( rootJointNodeSet, currentIndex, -1 );
        }

    return true;
}




//==================================================================================================
/*!
  @if jp

  @brief      ModelNodeSet内のShapeの整形処理の下請け関数

  @note       ModelNodeSet中のJointNodeを再帰的に辿る

  @date       2008-04-03 Y.TSUNODA <BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
int TriangleMeshGenerator::_traverseJointNode(
    JointNodeSetPtr		jointNodeSet,	//!< 対象となる JointNodeSet
    int&				currentIndex,	//!< このJointNodeSetのindex
    int					parentIndex )	//!< 親Nodeのindex
{
    int index = currentIndex;
    currentIndex++;

    // 子JointNode数を取得する
    size_t numChildren = jointNodeSet->childJointNodeSets.size();

    // 子JointNodeを順に辿る
    for( size_t i = 0 ; i < numChildren ; ++i )
	{
            // 親子関係のリンクを辿る
            JointNodeSetPtr childJointNodeSet = jointNodeSet->childJointNodeSets[i];
            _traverseJointNode( childJointNodeSet, currentIndex, index );
        }

    // JointNodeSet の segmentNode
    _traverseShapeNodes( jointNodeSet->segmentNode->fields["children"].mfNode() );

    return index;
}




//==================================================================================================
/*!
  @if jp

  @brief      ModelNodeSet内のShapeの整形処理の下請け関数

  @note       JointNodeSet中のNodeを再帰的に辿る

  @date       2008-04-03 Y.TSUNODA <BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
void TriangleMeshGenerator::_traverseShapeNodes(
    MFNode& childNodes )			//!< 子Node
{
    vector<SFNode>::iterator itr = childNodes.begin();

    while( itr != childNodes.end() )
	{
            VrmlNodePtr node = *itr;

            // Groupノードとそれを継承したノードの場合を、子ノードを辿っていく
            if( node->isCategoryOf( GROUPING_NODE ) )
		{
                    VrmlGroupPtr group = static_pointer_cast<VrmlGroup>( node );
                    _traverseShapeNodes( group->children );

                    ++itr;
		}
            // SHAPEノードならば
            else if( node->isCategoryOf( SHAPE_NODE ) )
		{
                    cout << "SHAPENODE " << node->defName << endl;

                    // 整形処理
                    if( this->uniform( node ) )
			{
                            VrmlIndexedFaceSetPtr uniformedNode( new VrmlIndexedFaceSet );
			
                            // 整形処理後の頂点配列を VrmlIndexedFaceSetへ代入する
                            VrmlCoordinatePtr coordinate( new VrmlCoordinate );
                            vector<Vector3> vertexList = this->getVertexList();
                            for( size_t i = 0 ; i < vertexList.size() ; ++i )
				{
                                    SFVec3f point;
                                    Vector3 vertex = vertexList[i];
                                    point[0] = vertex[0];
                                    point[1] = vertex[1];
                                    point[2] = vertex[2];
                                    coordinate->point.push_back( point );
				}
                            uniformedNode->coord = coordinate;

                            // 整形処理後の三角メッシュ配列を VrmlIndexedFaceSetへ代入する
                            vector<vector3i> triangleList = this->getTriangleList();
                            for( size_t i = 0 ; i < triangleList.size() ; ++i )
				{
                                    vector3i triangle = triangleList[i];
                                    uniformedNode->coordIndex.push_back( triangle[0] );
                                    uniformedNode->coordIndex.push_back( triangle[1] );
                                    uniformedNode->coordIndex.push_back( triangle[2] );
                                    uniformedNode->coordIndex.push_back( -1 );
				}

                            // Geometryノードを入れ替える
                            VrmlShapePtr shapeNode = static_pointer_cast<VrmlShape>( node );
                            shapeNode->geometry = uniformedNode;

                            ++itr;
			}
                    else
			{
                            // 整形に失敗したノードは削除する
                            itr = childNodes.erase( itr );
			}
		}
	}
}




//==================================================================================================
/*!
  @if jp

  @brief      整形処理

  @note       整形対象として引数で与えられたnodeを判定し，適切な形状の整形処理に渡す <BR>
  整形処理にて三角形メッシュベースの統一的な幾何形状で表現されたShapeInforを返す<BR>

  @date       2008-03-19 Y.TSUNODA <BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniform( 
    VrmlNodePtr node )		//!< 整形対象のNode (ShapeNode)
{
    bool ret = false;

    // Shape ノードを抽出する (既にnodeはShapeノードと判っているので)
    VrmlShapePtr shapeNode = static_pointer_cast<VrmlShape>( node );

    VrmlGeometryPtr geometry = shapeNode->geometry;
    if( VrmlBoxPtr box = dynamic_pointer_cast<VrmlBox>( geometry ) )
	{
            ret = uniformBox( box );
	}
    else if( VrmlConePtr cone = dynamic_pointer_cast<VrmlCone>( geometry ) )
	{
            ret = uniformCone( cone );
	}
    else if( VrmlCylinderPtr cylinder = dynamic_pointer_cast<VrmlCylinder>( geometry ) )
	{
            ret = uniformCylinder( cylinder );
	}
    else if( VrmlSpherePtr sphere = dynamic_pointer_cast<VrmlSphere>( geometry ) )
	{
            ret = uniformSphere( sphere );
	}
    else if( VrmlIndexedFaceSetPtr faceSet = dynamic_pointer_cast<VrmlIndexedFaceSet>( geometry ) )
	{
            ret = uniformIndexedFaceSet( faceSet );
	}
    else if( VrmlElevationGridPtr elevationGrid = dynamic_pointer_cast<VrmlElevationGrid>( geometry ) )
	{
            ret = uniformElevationGrid( elevationGrid );
	}
    else if( VrmlExtrusionPtr extrusion = dynamic_pointer_cast<VrmlExtrusion>( geometry ) )
	{
            ret = uniformExtrusion( extrusion );
	}
    else
	{
            // ##### [TODO] #####
            ;
	}

    return ret;
}




//==================================================================================================
/*!
  @if jp

  @brief      Box整形処理

  @note       VRMLプリミティブ形状BOXを，三角形メッシュベースの統一的な幾何形状表現へ変換する<BR>

  @date       2008-03-19 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  ShapeInfo廃止<BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniformBox(
    VrmlBoxPtr vrmlBox )	//!< 整形対象の Box node
{
    // 頂点リストと三角メッシュリストをクリアする
    type_ = S_BOX;
    vertexList_.clear();
    triangleList_.clear();

    // BOXサイズ取得
    double width  = vrmlBox->size[0];
    double height = vrmlBox->size[1];
    double depth  = vrmlBox->size[2];

    // エラーチェック
    if( width < 0.0 || height < 0.0 || depth < 0.0 )
        {
            this->putMessage( "BOX : wrong value." );
            return false;
        }

    // BOX頂点生成
    Vector3 vertex;

    vertex = -width/2.0, -height/2.0, -depth/2.0;	// 頂点No.0
    _addVertexList( vertex );
	
    vertex = -width/2.0, -height/2.0, depth/2.0;	// 頂点No.1
    _addVertexList( vertex );

    vertex = -width/2.0, height/2.0, -depth/2.0;	// 頂点No.2
    _addVertexList( vertex );

    vertex = -width/2.0, height/2.0, depth/2.0;		// 頂点No.3
    _addVertexList( vertex );

    vertex = width/2.0, -height/2.0, -depth/2.0;	// 頂点No.4
    _addVertexList( vertex );

    vertex = width/2.0, -height/2.0, depth/2.0;		// 頂点No.5
    _addVertexList( vertex );

    vertex = width/2.0, height/2.0, -depth/2.0;		// 頂点No.6
    _addVertexList( vertex );

    vertex = width/2.0, height/2.0, depth/2.0;		// 頂点No.7
    _addVertexList( vertex );


    // BOX 三角メッシュを生成する
    const int triangles[] =	{	5, 7, 3,	// Triangle No.0
                                        5, 3, 1,	// Triangle No.1
                                        0, 2, 6,	// Triangle No.2
                                        0, 6, 4,	// Triangle No.3
                                        4, 6, 7,	// Triangle No.4
                                        4, 7, 5,	// Triangle No.5
                                        1, 3, 2,	// Triangle No.6
                                        1, 2, 0,	// Triangle No.7
                                        7, 6, 2,	// Triangle No.8
                                        7, 2, 3,	// Triangle No.9
                                        4, 5, 1,	// Triangle No.10
                                        4, 1, 0,	// Triangle No.11
    };

    // triangleList_ に代入する
    const int triangleNumber = 12;
    for( int i = 0 ; i < triangleNumber ; i++ )
	{
            _addTriangleList( triangles[i*3+0], triangles[i*3+1], triangles[i*3+2] );
	}

    return true;
}




//==================================================================================================
/*!
  @if jp

  @brief      Cone整形処理

  @note       VRMLプリミティブ形状CONEを，三角形メッシュベースの統一的な幾何形状表現へ変換する<BR>

  @date       2008-03-19 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  ShapeInfo廃止<BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniformCone(
    VrmlConePtr vrmlShapeConeNode,	//!< 整形対象の Cone node
    int divisionNumber )			//!< 分割数
{
    // 頂点リストと三角メッシュリストをクリアする
    type_ = S_CONE;
    vertexList_.clear();
    triangleList_.clear();

    // CONEサイズ取得
    double height = vrmlShapeConeNode->height;
    double radius = vrmlShapeConeNode->bottomRadius;

    // エラーチェック
    if( height < 0.0 || radius < 0.0 )
        {
            this->putMessage( "CONE : wrong value." );
            return false;
        }

    vector<int>			circularList;			// 底面円周上の頂点を格納するリスト(作業用)
    Vector3			v;						// 頂点
    vector3i			tr;						// 三角メッシュ

    // CONE TOP ( = 0 番目の頂点 )
    v = 0.0, height, 0.0;
    _addVertexList( v );

    // CONE 底面中心 ( = 1 番目の頂点 )
    v = 0.0, 0.0, 0.0;
    _addVertexList( v );

    // Cone 底面円周上に頂点を生成する
    for( int i = 0 ;  i < divisionNumber ; i++ )
	{
            v =  radius * cos( i * 2.0 * PI / divisionNumber ),	// X
                0.0,											// Y
                -radius * sin( i * 2.0 * PI / divisionNumber );	// Z

            // 頂点リストに追加する，追加した頂点番号は vertexIndex
            size_t vertexIndex;		// 頂点番号
            vertexIndex = _addVertexList( v );

            // 底面円周上の頂点リストに追加する
            circularList.push_back( static_cast<int>( vertexIndex ) );
	}

    size_t cListSize = circularList.size();		// 上面・底面の頂点数
    //   理論上，divisionNumber と同じであるが

    // 三角メッシュを生成する
    for( size_t i = 0 ; i < cListSize ; i++ )
	{
            // CONE 側面の三角メッシュ ( TOP - 底面円周上の頂点1 - 底面円周上の頂点2 )
            tr = 0,
                circularList.at(   i       % cListSize ),
                circularList.at( ( i + 1 ) % cListSize );
            _addTriangleList( tr );

            // CONE 底面部分の三角メッシュ ( 底面中心 - 底面円周上の頂点2 - 底面円周上の頂点1 )
            tr = 1,
                circularList.at( ( i + 1 ) % cListSize ),
                circularList.at(   i       % cListSize );
            _addTriangleList( tr );
	}

    return true;
}




//==================================================================================================
/*!
  @if jp

  @brief      Cylinder整形処理

  @note       VRMLプリミティブ形状CYLINDERを，三角形メッシュベースの統一的な幾何形状表現へ変換する<BR>

  @date       2008-03-20 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  ShapeInfo廃止<BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniformCylinder(
    VrmlCylinderPtr vrmlShapeCylinderNode,	//!< 整形対象の Cylinder node
    int divisionNumber )					//!< 分割数
{
    // 頂点リストと三角メッシュリストをクリアする
    type_ = S_CYLINDER;
    vertexList_.clear();
    triangleList_.clear();

    // CYLINDERサイズ取得
    double height = vrmlShapeCylinderNode->height;
    double radius = vrmlShapeCylinderNode->radius;

    // エラーチェック
    if( height < 0.0 || radius < 0.0 )
        {
            this->putMessage( "CYLINDER : wrong value." );
            return false;
        }

    vector<int>			uCircularList;		// 上面円周上の頂点を格納するリスト(作業用)
    vector<int>			lCircularList;		// 底面円周上の頂点を格納するリスト(作業用)

    Vector3			v;					// 頂点
    vector3i			tr;					// 三角メッシュ

    // CYLINDER 上面中心 ( = 0 番目の頂点 )
    v =  0.0, height / 2.0, 0.0;
    _addVertexList( v );

    // CYLINDER 底面中心 ( = 1 番目の頂点 )
    v = 0.0, -height / 2.0, 0.0;
    _addVertexList( v );


    // CYLINDER 上面・底面円周上に頂点を生成する
    for( int i = 0 ; i < divisionNumber ; i++ )
	{
            size_t uVertexIndex;	// 上面円周上の頂点番号
            size_t lVertexIndex;	// 底面円周上の頂点番号

            // 上面円弧上の頂点
            v =  radius * cos( i * 2.0 * PI / divisionNumber ),
                height / 2.0,
                -radius * sin( i * 2.0 * PI / divisionNumber );
            uVertexIndex = _addVertexList( v );

            // 上面円周上の頂点リストに追加する
            uCircularList.push_back( static_cast<int>( uVertexIndex ) );
		
            // 底面円弧上の頂点
            v =  radius * cos( i * 2.0 * PI / divisionNumber ),
                -height / 2.0,
                -radius * sin( i * 2.0 * PI/ divisionNumber );
            lVertexIndex = _addVertexList( v );

            // 上面円周上の頂点リストに追加する
            lCircularList.push_back( static_cast<int>( lVertexIndex ) );
	}

    size_t cListSize = uCircularList.size();	// 上面・底面の頂点数
    //   理論上，divisionNumber と同じであるが

    // 三角メッシュを生成する
    for( size_t i = 0 ; i < cListSize ; i++ )
	{
            // 上面の三角メッシュ
            tr = 0,
                uCircularList.at(   i       % cListSize ),
                uCircularList.at( ( i + 1 ) % cListSize );
            _addTriangleList( tr );

            // 底面の三角メッシュ
            tr = 1,
                lCircularList.at( ( i + 1 ) % cListSize ),
                lCircularList.at(   i       % cListSize );
            _addTriangleList( tr );

            // 側面の(上尖)三角メッシュ
            tr = uCircularList.at(   i       % cListSize ),
                lCircularList.at(   i       % cListSize ),
                lCircularList.at( ( i + 1 ) % cListSize );
            _addTriangleList( tr );

            // 側面の(上尖)三角メッシュ
            tr = uCircularList.at(   i       % cListSize ),
                lCircularList.at( ( i + 1 ) % cListSize ),
                uCircularList.at( ( i + 1 ) % cListSize );
            _addTriangleList( tr );
	}

    return true;
}




//==================================================================================================
/*!
  @if jp

  @brief      IndexedFacedSet整形処理

  @note       VRMLプリミティブ形状INDEXEDFACESETを，三角形メッシュベースの統一的な幾何形状表現へ
  変換する<BR>

  @date       2008-03-20 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  ShapeInfo廃止<BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniformIndexedFaceSet(
    VrmlIndexedFaceSetPtr vrmlIndexedFaceSetNode )	//!< 整形対象の IndexedFacedSet node
{
    // 頂点リストと三角メッシュリストをクリアする
    type_ = S_INDEXED_FACE_SET;
    vertexList_.clear();
    triangleList_.clear();

    // coord field 頂点群
    VrmlCoordinatePtr coodinate = vrmlIndexedFaceSetNode->coord;
    MFVec3f points = coodinate->point;		// 頂点座標群を格納した std::vector。中身は boost::array<SFFloat,3>

    // 頂点群 points(MFVec3f)を vertexList_に入れる (コピー)
    for( size_t i = 0 ; i < points.size() ; i++ )
	{
            Vector3 v;
            v = ( points.at( i ) )[0], ( points.at( i ) )[1], ( points.at( i ) )[2];
            _addVertexList( v );
	}

    // 整形処理をする場合
    if( flgUniformIndexedFaceSet_ )
	{

            // メッシュ配列を取得する(coordListに代入する)
            MFInt32 coordList = vrmlIndexedFaceSetNode->coordIndex;

            vector<int>			mesh;				// 作業用テンポラリ
            //   モデルファイルで指定された coordIndexから順に，
            //    一つのメッシュを構成する頂点番号を格納するvector

            for( size_t i = 0 ; i < vrmlIndexedFaceSetNode->coordIndex.size() ; i++ )
		{
                    // [MEMO] まず，-1が出現するまでの，1つのメッシュの頂点群をmeshに入れる
                    //        -1 が出現したら，三角メッシュを生成して traiangleListに追加する
                    //        処理が終わり不要になった(テンポラリの)頂点群 meshをクリアする

                    int		index = vrmlIndexedFaceSetNode->coordIndex.at( i );
                    if( index == -1 )
			{
                            // 三角メッシュを生成し，triangleList_に追加する
                            //   生成失敗時( 頂点数が5以上の場合 ) 処理中断し falseを返す
                            if( _createTriangleMesh( mesh, vrmlIndexedFaceSetNode->ccw ) < 0 )	return false;

                            // メッシュをクリア
                            mesh.clear();
			}
                    else
			{
                            // 頂点番号をメッシュに追加
                            mesh.push_back( index );
			}
		}
		
            // meshにデータが残っていれば，
            //   → 格納出来ていない場合も考えられるので，(coordinateIndexが-1で終わっていない場合)
            if( 0 < mesh.size() )
		{
                    // 三角メッシュを生成し，triangleList_に追加する
                    //   生成失敗時( 頂点数が5以上の場合 ) 処理中断し falseを返す
                    if( _createTriangleMesh( mesh, vrmlIndexedFaceSetNode->ccw ) < 0 ) return false;

                    // メッシュをクリア
                    mesh.clear();
		}
	}
    // 整形処理をしない(三角メッシュであることが保証されている)場合
    else
	{
            // メッシュ数を計算する
            //   coordIndexは -1を区切り子としているので size() / 4 がメッシュ数となる
            //   また，coordIndexの最後に-1が無い可能性もあるので size() + 1 としている 
            size_t meshNum = static_cast<size_t>( ( vrmlIndexedFaceSetNode->coordIndex.size() + 1 ) / 4 );

            for( size_t i = 0 ; i < meshNum ; i++ )
		{
                    // [MEMO]
                    // 念のため vrmlIndexedFaceSetNode->coordIndex.at( # ) が妥当な数値であるか
                    // チェックすべきかもしれないが，「三角メッシュであることが保証されている」
                    // ということで，一切チェックはしていない。

                    // triangleListに追加する
                    _addTriangleList( vrmlIndexedFaceSetNode->coordIndex.at( 4*i+0 ),
                                      vrmlIndexedFaceSetNode->coordIndex.at( 4*i+1 ),
                                      vrmlIndexedFaceSetNode->coordIndex.at( 4*i+2 ) );
		}
	}

    return true;
}




//==================================================================================================
/*!
  @if jp

  @brief      Sphere整形処理

  @note       VRMLプリミティブ形状SPHEREを，三角形メッシュベースの統一的な幾何形状表現へ変換する<BR>

  @date       2008-03-20 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  ShapeInfo廃止<BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniformSphere(
    VrmlSpherePtr vrmlShapeSphereNode,		//!< 整形対象の Sphere node
    int vDivisionNumber,					//!< 分割数 緯度方向
    int hDivisionNumber )					//!< 分割数 経度方向
{
    // 頂点リストと三角メッシュリストをクリアする
    type_ = S_SPHERE;
    vertexList_.clear();
    triangleList_.clear();

    // CYLINDERサイズ取得
    double radius = vrmlShapeSphereNode->radius;

    // エラーチェック
    if( radius < 0.0 )
        {
            this->putMessage( "SPHERE : wrong value." );
            return false;
        }

    Vector3			v;					// 頂点
    vector3i			tr;					// 三角メッシュ

    // SPHERE 天頂座標 ( = 0 番目の頂点 )
    v =  0.0, radius, 0.0;
    _addVertexList( v );

    // SPHERE 天底座標 ( = 1 番目の頂点 )
    v = 0.0, -radius, 0.0;
    _addVertexList( v );

    vector< vector<int> >	vertexIndexMatrix;	// 頂点インデックスを格納したマトリクス(一時作業用)
    //   →天頂・天底を除く頂点を格納するマトリクス
    //     これを元にして三角メッシュリストtraiangleListを生成する
    vector<int>			circularList;			// 円周上の頂点リスト(一時作業用)
    //   →球をY軸に垂直な平面でスライスして出来る面・円周上の頂点リスト

    // 緯度(縦)方向のループ
    for( int i = 1 ; i < vDivisionNumber ; i++ )
	{
            double radVDivison = i * PI / vDivisionNumber;				// 緯度方向の分割角(ラジアン)

            // 一円周上の頂点(index)リストをクリアする
            circularList.clear();

            // 経度(横)方向のループ
            for( int j = 0 ; j < hDivisionNumber ; j++ )
		{
                    double radHDivision = j * 2.0 * PI / hDivisionNumber;	// 経度方向の分割角(ラジアン)

                    // 頂点座標を計算する
                    v =  radius * sin( radVDivison ) * cos( radHDivision ),
                        radius * cos( radVDivison ),
                        -radius * sin( radVDivison ) * sin( radHDivision );

                    // 頂点リストに追加する
                    size_t vertexIndex;		// 頂点インデックス
                    vertexIndex = _addVertexList( v );

                    // 一円周上の頂点(index)リストに追加する
                    circularList.push_back( static_cast<int>( vertexIndex ) );
		}

            // 一円周上の頂点(index)リストを，頂点(index)マトリクスに追加する
            vertexIndexMatrix.push_back( circularList );
	}


    // 最上・最下帯のメッシュを追加する
    vector<int> uCircularList = vertexIndexMatrix.at( 0 );
    vector<int> lCircularList = vertexIndexMatrix.at( vertexIndexMatrix.size() - 1 );

    size_t cListSize = uCircularList.size();	// 一円周上の頂点数
    for( size_t i = 0 ; i < cListSize ; i++ )
	{
            // 天頂側
            tr = 0,
                uCircularList.at( i         % cListSize ),
                uCircularList.at( ( i + 1 ) % cListSize );
            _addTriangleList( tr );

            // 天底側
            tr = 1,
                lCircularList.at( ( i + 1 ) % cListSize ),
                lCircularList.at( i         % cListSize );
            _addTriangleList( tr );
	}

    // 側面帯のメッシュを追加する
    for( int i = 0 ; i < static_cast<int>( vertexIndexMatrix.size() ) - 1 ; i++ )
	{
            // ある側面帯の上下円周の頂点リストを取得する
            uCircularList = vertexIndexMatrix.at( i );
            lCircularList = vertexIndexMatrix.at( i + 1 );

            cListSize = uCircularList.size();
            for( size_t j = 0 ; j < cListSize ; j++ )
		{
                    // 上尖三角メッシュ
                    tr = uCircularList.at(   j       % cListSize ),
                        lCircularList.at(   j       % cListSize ),
                        lCircularList.at( ( j + 1 ) % cListSize );
                    _addTriangleList( tr );

                    // 下尖三角メッシュ
                    tr = uCircularList.at(   j       % cListSize ),
                        lCircularList.at( ( j + 1 ) % cListSize ),
                        uCircularList.at( ( j + 1 ) % cListSize );
                    _addTriangleList( tr );
		}
	}

    return true;
}




//==================================================================================================
/*!
  @if jp

  @brief      ElevationGrid整形処理

  @note       VRMLプリミティブ形状ELEVATIONGRIDを，三角形メッシュベースの統一的な幾何形状表現へ
  変換する<BR>

  @date       2008-03-20 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  ShapeInfo廃止<BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniformElevationGrid(
    VrmlElevationGridPtr elevationGrid )	//!< 整形対象の ElevationGrid node
{
    type_ = S_ELEVATION_GRID;

    // 格子数と高度指定の個数が一致していなければ， 
    if( elevationGrid->xDimension * elevationGrid->zDimension
        != static_cast<SFInt32>( elevationGrid->height.size() ) )
	{	
            this->putMessage( "ELEVATIONGRID : wrong value." );
            return false;
	}

    Vector3			v;						// 頂点
    vector3i			tr;						// 三角メッシュ

    vector< vector<int> >	vertexIndexMatrix;	// 頂点インデックスを格納したマトリクス(一時作業用)
    vector<int>			lineList;				// 格子一列の頂点リスト(一時作業用)

    for( int z = 0 ; z < elevationGrid->zDimension ; z++ )
	{
            for( int x = 0 ; x < elevationGrid->xDimension ; x++ )
		{
                    v = x * elevationGrid->xSpacing,
                        elevationGrid->height[z * elevationGrid->xDimension + x],
                        z * elevationGrid->zSpacing;

                    // 頂点リストに追加する
                    size_t vertexIndex;		// 頂点インデックス
                    vertexIndex = _addVertexList( v );

                    lineList.push_back( static_cast<int>( vertexIndex ) );
		}
		
            vertexIndexMatrix.push_back( lineList );
            lineList.clear();
	}


    // メッシュ生成する
    for( int z = 0 ; z < static_cast<int>( vertexIndexMatrix.size() ) - 1 ; z++ )
	{
            vector<int> currentLine	= vertexIndexMatrix.at( z );
            vector<int> nextLine	= vertexIndexMatrix.at( z + 1 );

            for( int x = 0 ; x < static_cast<int>( currentLine.size() ) - 1 ; x++ )
		{
                    // 上尖三角形
                    _addTriangleList( currentLine.at( x ), nextLine.at( x ), nextLine.at( x + 1 ), elevationGrid->ccw );

                    // 下尖三角形
                    _addTriangleList( currentLine.at( x ), nextLine.at( x + 1 ), currentLine.at( x + 1 ), elevationGrid->ccw );
		}
	}

    return true;
}



//==================================================================================================
/*!
  @if jp

  @brief      Extrusion整形処理

  @note       VRMLプリミティブ形状EXTRUSIONを，三角形メッシュベースの統一的な幾何形状表現へ変換
  する<BR>

  @date       2008-03-20 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  ShapeInfo廃止<BR>

  @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
TriangleMeshGenerator::uniformExtrusion(
    VrmlExtrusionPtr extrusion )	//!< 整形対象の Extrusion node
{
    // 頂点リストと三角メッシュリストをクリアする
    type_ = S_EXTRUSION;
    vertexList_.clear();
    triangleList_.clear();

    return true;
}





/*!
  @if jp
  n角形のメッシュを三角形メッシュに分割する.
  現時点では，分割対象のメッシュは三角形・四角形のメッシュのみに対応.
  @param mesh 1つのメッシュを構成する頂点群リスト
  @parm ccw CounterClockWise (反時計まわり)指定
  @return int 分割した三角形の数 エラー時は負の整数
  @endif
*/
int TriangleMeshGenerator::_createTriangleMesh(const vector<int>& mesh, bool ccw)
{
    int triangleCount = 0;		// 分割した三角形の数
    size_t vertexNumber = mesh.size();	// メッシュを構成する頂点数

    if( vertexNumber == 3 ){
        triangleCount = 1;
        // そのまま triangleListに追加する
        _addTriangleList( mesh[0], mesh[1], mesh[2], ccw );
        
    } else if( vertexNumber == 4 ){
        triangleCount = 2;

        // 対角線の長さが短い方で分割する
        double distance02 = norm2(vertexList_[mesh[0]] - vertexList_[mesh[2]]);
        double distance13 = norm2(vertexList_[mesh[1]] - vertexList_[mesh[3]]);

        if(distance02 < distance13){
            _addTriangleList( mesh[0], mesh[1], mesh[2], ccw );
            _addTriangleList( mesh[0], mesh[2], mesh[3], ccw );
        } else {
            _addTriangleList( mesh[0], mesh[1], mesh[3], ccw );
            _addTriangleList( mesh[1], mesh[2], mesh[3], ccw );
        }
    } else {
        this->putMessage( "The number of vertex is 5 or more." );
        triangleCount = -1;
    }
    
    return triangleCount;
}




/*!
  @if jp
  頂点リストに頂点を追加し，追加した位置(index)を返す.
  @return size_t 頂点を頂点リストに追加した位置(index)
  @endif
*/
size_t TriangleMeshGenerator::_addVertexList(const Vector3& v)
{
    vertexList_.push_back(v);
    return (vertexList_.size() - 1);
}




//==================================================================================================
/*!
  @if jp

  @brief      三角メッシュリストに三角メッシュを追加する(構成する頂点index指定)

  @note		三角メッシュリストに頂点を追加し，追加した位置(index)を返す<BR>
  但し，ccwフラグがfalseの場合，時計回り(裏面)の三角メッシュも生成する<BR>
  よって 2つのメッシュがここで生成・登録されることになる。<BR>
  戻り値は，通常の(反時計回り)の三角メッシュが格納された位置のindex<BR>
  ccwフラグがfalseの場合，時計回りの三角メッシュが格納された位置はindex+1で得られる<BR>

  @date       2008-03-10 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  triangleList_ メンバ変数化に伴う変更<BR>

  @return		size_t 三角メッシュを三角メッシュリストに追加した位置(index)

  @endif
*/
//==================================================================================================
size_t TriangleMeshGenerator::_addTriangleList(
    int v1,						//!< メッシュを構成する頂点インデックス1
    int v2,						//!< メッシュを構成する頂点インデックス2
    int v3,						//!< メッシュを構成する頂点インデックス3
    bool ccw )					//!< CounterClockWise (反時計まわり)指定
{
    vector3i triangle;
    size_t	index;

    // 通常は，指定された頂点順に三角メッシュを生成する
    triangle = v1, v2, v3;
    index = _addTriangleList( triangle );

    // ccwフラグが false ならば，
    if( false == ccw )
	{
            // 時計回り(裏向き)の三角メッシュを生成する
            triangle = v1, v3, v2;
            _addTriangleList( triangle );
	}

    return( index );
}




//==================================================================================================
/*!
  @if jp

  @brief      三角メッシュリストに三角メッシュを追加

  @note		三角メッシュリストに三角メッシュを追加し，追加した位置(index)を返す<BR>

  @date       2008-03-10 Y.TSUNODA <BR>
  2008-03-27 K.FUKUDA  triangleList_ メンバ変数化に伴う変更<BR>

  @return		size_t 三角メッシュを三角メッシュリストに追加した位置(index)

  @endif
*/
//==================================================================================================
size_t TriangleMeshGenerator::_addTriangleList(
    vector3i t )				//!< 三角メッシュ
{
    triangleList_.push_back( t );

    return( triangleList_.size() - 1 );
}





//==================================================================================================
/*!
  @if jp

  @brief      メッセージ出力

  @note		<BR>

  @date       2008-04-18 Y.TSUNODA <BR>

  @return		void

  @endif
*/
//==================================================================================================
void TriangleMeshGenerator::putMessage(
    const std::string& message )
{
    if( flgMessageOutput_ )
	{
            signalOnStatusMessage( message + "\n" );
	}
}
