/*! 
  @file BodyInfo_impl.cpp
  @author S.NAKAOKA
*/

#include "BodyInfo_impl.h"
#include "ViewSimulator.h"
#include "VrmlNodes.h"
#include "CalculateNormal.h"
#include "ImageConverter.h"

#include <iostream>
#include <map>
#include <vector>
#include <boost/bind.hpp>

using namespace std;
using namespace boost;
using namespace OpenHRP;



/*!
  @if jp
  shapeInfo共有のためのマップ
  既に shape_ に格納されている node であれば、対応するインデックスを持つ
  @else
  Map for sharing shapeInfo
   if it is node that has already stored in shape_, it has the corresponding index.
  @endif
*/

// ShapeInfoのindexと，そのshapeを算出したtransformのペア
struct ShapeObject
{
	matrix44d	transform;
	short		index;
};

typedef map<OpenHRP::VrmlNodePtr, ShapeObject> SharedShapeInfoMap;
SharedShapeInfoMap sharedShapeInfoMap;

typedef map<string, string> SensorTypeMap;
SensorTypeMap sensorTypeMap;


bool operator != ( matrix44d& a, matrix44d& b )
{
	for( int i = 0 ; i < 4 ; i++ )
	{
		for( int j = 0 ; j < 4; j++ )
		{
			if( a( i, j ) != b( i, j ) )
				return false;
		}
	}
	return true;
}


BodyInfo_impl::BodyInfo_impl( PortableServer::POA_ptr poa ) :
    poa( PortableServer::POA::_duplicate( poa ) )
{
	lastUpdate_ = 0;
}


BodyInfo_impl::~BodyInfo_impl()
{
}


PortableServer::POA_ptr BodyInfo_impl::_default_POA()
{
    return PortableServer::POA::_duplicate( poa );
}


char* BodyInfo_impl::name()
{
    return CORBA::string_dup(name_.c_str());
}

char* BodyInfo_impl::url()
{
    return CORBA::string_dup(url_.c_str());
}

StringSequence* BodyInfo_impl::info()
{
    return new StringSequence(info_);
}

LinkInfoSequence* BodyInfo_impl::links()
{
    return new LinkInfoSequence(links_);
}

AllLinkShapeIndices* BodyInfo_impl::linkShapeIndices()
{
    return new AllLinkShapeIndices( linkShapeIndices_ );
}

ShapeInfoSequence* BodyInfo_impl::shapes()
{
    return new ShapeInfoSequence( shapes_ );
}

AppearanceInfoSequence* BodyInfo_impl::appearances()
{
    return new AppearanceInfoSequence( appearances_ );
}

MaterialInfoSequence* BodyInfo_impl::materials()
{
    return new MaterialInfoSequence( materials_ );
}

TextureInfoSequence* BodyInfo_impl::textures()
{
    return new TextureInfoSequence( textures_ );
}

void BodyInfo_impl::putMessage( const std::string& message )
{
  cout << message;
}




//==================================================================================================
/*!
  @if jp

    @brief		URLスキーム(file:)文字列を削除

	@note       <BR>

	@return	    string URLスキーム文字列を取り除いた文字列

  @endif
*/
//==================================================================================================
string
BodyInfo_impl::deleteURLScheme(
	string url )	//!< URLパス文字列
{
	// URL scheme を取り除く
    static const string fileProtocolHeader1("file:///");
	static const string fileProtocolHeader2("file://");
	static const string fileProtocolHeader3("file:");

    size_t pos = url.find( fileProtocolHeader1 );
    if( 0 == pos )
	{
		url.erase( 0, fileProtocolHeader1.size() );
    }
	else
	{
	    size_t pos = url.find( fileProtocolHeader2 );
    	if( 0 == pos )
		{
			url.erase( 0, fileProtocolHeader2.size() );
    	}
		else
		{
			size_t pos = url.find( fileProtocolHeader3 );
    		if( 0 == pos )
			{
				url.erase( 0, fileProtocolHeader3.size() );
    		}
		}
    }

	return url;
}

//==================================================================================================
/*!
  @if jp

    @brief		文字列置換

	@note       <BR>

	@return	    str 内の 特定文字列　sb を 別の文字列　sa に置換


  @endif
*/
//==================================================================================================
string&
BodyInfo_impl::replace(string& str, const string sb, const string sa)
{
	string::size_type n, nb = 0;
	
	while ((n = str.find(sb,nb)) != string::npos)
	{
		str.replace(n,sb.size(),sa);
		nb = n + sa.size();
	}
	
	return str;
}




//==================================================================================================
/*!
  @if jp
	@brief		モデルファイルのロード
	@note		BodyInfoを構築する。
	@return	    void
  @else
	@brief		load model file
	@note		Constructs a BodyInfo (a CORBA interface)
	@return	    void
  @endif
*/
//==================================================================================================
void
BodyInfo_impl::loadModelFile(
	const std::string& url )	//!< モデルファイルパス文字列 (URL)
{
	// URL scheme を取り除いたファイルパスをセットする
    string filename( deleteURLScheme( url ) );

	// URL文字列の' \' 区切り子を'/' に置き換え  Windows ファイルパス対応 
	string url2;
	url2 = filename;
	replace( url2, string("\\"), string("/") );
	filename = url2;

    ModelNodeSet modelNodeSet;
    modelNodeSet.signalOnStatusMessage.connect(bind(&BodyInfo_impl::putMessage, this, _1));
	modelNodeSet.setMessageOutput( true );


    try
	{
    	modelNodeSet.loadModelFile( filename );
		cout.flush();
    }
    catch(ModelNodeSet::Exception& ex)
	{
		throw ModelLoader::ModelLoaderException(ex.message.c_str());
    }

	// BodyInfoメンバに値をセットする
    const string& humanoidName = modelNodeSet.humanoidNode()->defName;
    name_ = CORBA::string_dup(humanoidName.c_str());

    url_ = CORBA::string_dup(url2.c_str());

	// JointNode数を取得する
    int numJointNodes = modelNodeSet.numJointNodes();

	// links_, linkShapeIndices_ 配列サイズを確保する
    links_.length(numJointNodes);
	linkShapeIndices_.length( numJointNodes );

    if( 0 < numJointNodes )
	{
		int currentIndex = 0;

		// JointNode を再帰的に辿り，LinkInfoを生成する
		JointNodeSetPtr rootJointNodeSet = modelNodeSet.rootJointNodeSet();
		readJointNodeSet( rootJointNodeSet, currentIndex, -1 );

		// AllLinkShapeIndices を構築する
		// links_中のlinkInfoを辿り，
		for( size_t i = 0 ; i < numJointNodes ; ++i )
		{
			// linkInfoのメンバshapeIndicesをlinkShapeIndicesへ代入する
			linkShapeIndices_[i] = links_[i].shapeIndices;
		}
    }
}




//==================================================================================================
/*!
  @if jp
	@brief		read JointNodeSet
	@note		Constructs a BodyInfo (a CORBA interface) <BR>
				During construction of the BodyInfo, LinkInfo, ShapeInfo, AppearanceInfo, 
				MaterialInfo, and TextureInfo structures are constructed.
	@return	    int
  @endif
*/
//==================================================================================================
int BodyInfo_impl::readJointNodeSet(
	JointNodeSetPtr		jointNodeSet,	//!< 対象となる JointNodeSet
	int&				currentIndex,	//!< このJointNodeSetのindex
	int					parentIndex )	//!< 親Nodeのindex
{
	int index = currentIndex;
	currentIndex++;

	LinkInfo_var linkInfo( new LinkInfo() );
	linkInfo->parentIndex = parentIndex;

	// 子JointNode数を取得する
    size_t numChildren = jointNodeSet->childJointNodeSets.size();

	// 子JointNodeを順に辿る
	for( size_t i = 0 ; i < numChildren ; ++i )
	{
		// 親子関係のリンクを生成する
		JointNodeSetPtr childJointNodeSet = jointNodeSet->childJointNodeSets[i];
		int childIndex = readJointNodeSet( childJointNodeSet, currentIndex, index );

		// chidlIndices に childIndex を追加する
		long childIndicesLength = linkInfo->childIndices.length();
		linkInfo->childIndices.length( childIndicesLength + 1 );
		linkInfo->childIndices[childIndicesLength] = childIndex;
    }

	// links_ の適切な位置(index)へ格納する
	links_[index] = linkInfo;

	try
	{
		matrix44d unit4d( tvmet::identity<matrix44d>() );

		// JointNodeSet の segmentNode
		traverseShapeNodes( index, jointNodeSet->segmentNode->fields["children"].mfNode(), unit4d );

		setJointParameters( index, jointNodeSet->jointNode );
		setSegmentParameters( index, jointNodeSet->segmentNode );
		setSensors( index, jointNodeSet );
    }

	catch( ModelLoader::ModelLoaderException& ex )
	{
		//string name = linkInfo->name;
		CORBA::String_var cName = linkInfo->name;
		string name( cName );
		string error = name.empty() ? "Unnamed JoitNode" : name;
		error += ": ";
		error += ex.description;
		throw ModelLoader::ModelLoaderException( error.c_str() );
    }

    return index;
}




//==================================================================================================
/*!
  @if jp

    @brief		LinkInfoにJointNodeのパラメータ設定

	@note       <BR>

    @date       2008-03-11 Y.TSUNODA <BR>

    @return     void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::setJointParameters(
	int linkInfoIndex,					//!< LinkInfoインデックス (links_のインデックス)
	VrmlProtoInstancePtr jointNode )	//!< JointNodeオブジェクトへのポインタ
{
	// 対象となる linkInfoインスタンスへの参照
	LinkInfo& linkInfo = links_[linkInfoIndex];

	linkInfo.name =  CORBA::string_dup( jointNode->defName.c_str() );

    TProtoFieldMap& fmap = jointNode->fields;

	CORBA::Long jointId;
	copyVrmlField( fmap, "jointId", jointId );
	linkInfo.jointId = (CORBA::Short)jointId; 

	linkInfo.jointAxis[0] = 0.0;
	linkInfo.jointAxis[1] = 0.0;
	linkInfo.jointAxis[2] = 0.0;
    
    VrmlVariantField& fJointAxis = fmap["jointAxis"];

    switch( fJointAxis.typeId() )
	{

    case SFSTRING:
		{
			SFString& axisLabel = fJointAxis.sfString();
			if( axisLabel == "X" )		{ linkInfo.jointAxis[0] = 1.0; }
			else if( axisLabel == "Y" )	{ linkInfo.jointAxis[1] = 1.0; }
			else if( axisLabel == "Z" ) { linkInfo.jointAxis[2] = 1.0; }
		}
		break;
		
    case SFVEC3F:
		copyVrmlField( fmap, "jointAxis", linkInfo.jointAxis );
		break;

    default:
		break;
    }

	std::string jointType;
    copyVrmlField( fmap, "jointType", jointType );
	linkInfo.jointType = CORBA::string_dup( jointType.c_str() );

	copyVrmlField( fmap, "translation", linkInfo.translation );
    copyVrmlRotationFieldToDblArray4( fmap, "rotation", linkInfo.rotation );

    copyVrmlField( fmap, "ulimit",  linkInfo.ulimit );
    copyVrmlField( fmap, "llimit",  linkInfo.llimit );
    copyVrmlField( fmap, "uvlimit", linkInfo.uvlimit );
    copyVrmlField( fmap, "lvlimit", linkInfo.lvlimit );

    copyVrmlField( fmap, "gearRatio",     linkInfo.gearRatio );
    copyVrmlField( fmap, "rotorInertia",  linkInfo.rotorInertia );
	copyVrmlField( fmap, "rotorResistor", linkInfo.rotorResistor );
    copyVrmlField( fmap, "torqueConst",   linkInfo.torqueConst );
    copyVrmlField( fmap, "encoderPulse",  linkInfo.encoderPulse );
	copyVrmlField( fmap, "jointValue",    linkInfo.jointValue );

	// equivalentInertia は廃止
}





//==================================================================================================
/*!
  @if jp

    @brief		LinkInfoにSegmentNodeのパラメータ設定

	@note       <BR>

    @date       2008-03-11 Y.TSUNODA <BR>

    @return     void

  @endif
*/
//==================================================================================================
void BodyInfo_impl::setSegmentParameters(
	int linkInfoIndex,					//!< LinkInfoインデックス (links_のインデックス)
	VrmlProtoInstancePtr segmentNode )	//!< SegmentNodeオブジェクトへのポインタ
{
	// 対象となる linkInfoインスタンスへの参照
	LinkInfo& linkInfo = links_[linkInfoIndex];

	if( segmentNode )
	{
		TProtoFieldMap& fmap = segmentNode->fields;
		
		copyVrmlField( fmap, "centerOfMass",     linkInfo.centerOfMass );
		copyVrmlField( fmap, "mass",             linkInfo.mass );
		copyVrmlField( fmap, "momentsOfInertia", linkInfo.inertia );
	}
	else
	{
		linkInfo.mass = 0.0;
		// set zero to centerOfMass and inertia
		for( int i = 0 ; i < 3 ; ++i )
		{
			linkInfo.centerOfMass[i] = 0.0;
			for( int j = 0 ; j < 3 ; ++j )
			{
				linkInfo.inertia[i*3 + j] = 0.0;
			}
		}
	}
}





//==================================================================================================
/*!
  @if jp

    @brief		SensorInfo生成

	@note       <BR>

    @date       2008-03-11 Y.TSUNODA <BR>

    @return     void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::setSensors(
	int linkInfoIndex,				//!< LinkInfoインデックス (links_のインデックス)
	JointNodeSetPtr jointNodeSet )	//!< JointNodeSetオブジェクトへのポインタ
{
	// 対象となる linkInfoインスタンスへの参照
	LinkInfo& linkInfo = links_[linkInfoIndex];

	vector<VrmlProtoInstancePtr>& sensorNodes = jointNodeSet->sensorNodes;

	int numSensors = sensorNodes.size();
	linkInfo.sensors.length( numSensors );

	for( int i = 0 ; i < numSensors ; ++i )
	{
		SensorInfo_var sensorInfo( new SensorInfo() );

		readSensorNode( linkInfoIndex, sensorInfo, sensorNodes[i] );

		linkInfo.sensors[i] = sensorInfo;
	}
}





//==================================================================================================
/*!
  @if jp

    @brief		SensorInfoにSensorNodeのパラメータ設定

	@note       <BR>

    @date       2008-03-11 Y.TSUNODA <BR>

    @return     void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::readSensorNode(
	int linkInfoIndex,					//!< LinkInfoインデックス (links_のインデックス)
	SensorInfo& sensorInfo,				//!< SensorInfoオブジェクト(パラメータを代入する先)
	VrmlProtoInstancePtr sensorNode )	//!< SensorNodeオブジェクトへのポインタ
{
	if( sensorTypeMap.empty() )
	{
		// initSensorTypeMap();
		sensorTypeMap["ForceSensor"]		= "Force";
		sensorTypeMap["Gyro"]				= "RateGyro";
		sensorTypeMap["AccelerationSensor"]	= "Acceleration";
		sensorTypeMap["PressureSensor"]		= "";
		sensorTypeMap["PhotoInterrupter"]	= "";
		sensorTypeMap["VisionSensor"]		= "Vision";
		sensorTypeMap["TorqueSensor"]		= "";
	}

	try
	{
		sensorInfo.name = CORBA::string_dup( sensorNode->defName.c_str() );

		TProtoFieldMap& fmap = sensorNode->fields;

		copyVrmlField( fmap, "sensorId", sensorInfo.id );

		copyVrmlField( fmap, "translation", sensorInfo.translation );
		copyVrmlRotationFieldToDblArray4( fmap, "rotation", sensorInfo.rotation );

		SensorTypeMap::iterator p = sensorTypeMap.find( sensorNode->proto->protoName );
		std::string sensorType;
		if( p != sensorTypeMap.end() )
		{
			sensorType = p->second;
			sensorInfo.type = CORBA::string_dup( sensorType.c_str() );
		}
		else
		{
			throw ModelLoader::ModelLoaderException("Unknown Sensor Node");
		}

		if( sensorType == "Force" )
		{
			sensorInfo.specValues.length( CORBA::ULong(6) );
			DblArray3 maxForce, maxTorque;
			copyVrmlField( fmap, "maxForce", maxForce );
			copyVrmlField( fmap, "maxTorque", maxTorque );
			sensorInfo.specValues[0] = maxForce[0];
			sensorInfo.specValues[1] = maxForce[1];
			sensorInfo.specValues[2] = maxForce[2];
			sensorInfo.specValues[3] = maxTorque[0];
			sensorInfo.specValues[4] = maxTorque[1];
			sensorInfo.specValues[5] = maxTorque[2];

		}
		else if( sensorType == "RateGyro" )
		{
			sensorInfo.specValues.length( CORBA::ULong(3) );
			DblArray3 maxAngularVelocity;
			copyVrmlField(fmap, "maxAngularVelocity", maxAngularVelocity);
			sensorInfo.specValues[0] = maxAngularVelocity[0];
			sensorInfo.specValues[1] = maxAngularVelocity[1];
			sensorInfo.specValues[2] = maxAngularVelocity[2];

		}
		else if( sensorType == "Acceleration" )
		{
			sensorInfo.specValues.length( CORBA::ULong(3) );
			DblArray3 maxAcceleration;
			copyVrmlField(fmap, "maxAcceleration", maxAcceleration);
			sensorInfo.specValues[0] = maxAcceleration[0];
			sensorInfo.specValues[1] = maxAcceleration[1];
			sensorInfo.specValues[2] = maxAcceleration[2];

		}
		else if( sensorType == "Vision" )
		{
			sensorInfo.specValues.length( CORBA::ULong(6) );

			CORBA::Double specValues[3];
			copyVrmlField( fmap, "frontClipDistance", specValues[0] );
			copyVrmlField( fmap, "backClipDistance", specValues[1] );
			copyVrmlField( fmap, "fieldOfView", specValues[2] );
			sensorInfo.specValues[0] = specValues[0];
			sensorInfo.specValues[1] = specValues[1];
			sensorInfo.specValues[2] = specValues[2];

			std::string sensorTypeString;
			copyVrmlField( fmap, "type", sensorTypeString );
		    
			if( sensorTypeString=="NONE" )				{ sensorInfo.specValues[3] = Camera::NONE;		}
			else if( sensorTypeString=="COLOR" )		{ sensorInfo.specValues[3] = Camera::COLOR;		}
			else if( sensorTypeString=="MONO" )			{ sensorInfo.specValues[3] = Camera::MONO;		}
			else if( sensorTypeString=="DEPTH" )		{ sensorInfo.specValues[3] = Camera::DEPTH;		}
			else if( sensorTypeString=="COLOR_DEPTH" )	{ sensorInfo.specValues[3] = Camera::COLOR_DEPTH; }
			else if( sensorTypeString=="MONO_DEPTH" )	{ sensorInfo.specValues[3] = Camera::MONO_DEPTH; }
			else
			{
				throw ModelLoader::ModelLoaderException("Sensor node has unkown type string");
			}

			CORBA::Long width, height;
			copyVrmlField( fmap, "width", width );
			copyVrmlField( fmap, "height", height );

			sensorInfo.specValues[4] = static_cast<CORBA::Double>(width);
			sensorInfo.specValues[5] = static_cast<CORBA::Double>(height);
		}


		// rotationロドリゲスの回転軸
		vector3d vRotation( sensorInfo.rotation[0], sensorInfo.rotation[1], sensorInfo.rotation[2] );

		// ロドリゲスrotationを3x3行列に変換する
		matrix33d mRotation;
		PRIVATE::rodrigues( mRotation, vRotation, sensorInfo.rotation[3] );

		// rotation, translation を4x4行列に代入する
		matrix44d mTransform( tvmet::identity<matrix44d>() );
		mTransform =
			mRotation(0,0), mRotation(0,1), mRotation(0,2), sensorInfo.translation[0],
			mRotation(1,0), mRotation(1,1), mRotation(1,2), sensorInfo.translation[1],
			mRotation(2,0), mRotation(2,1), mRotation(2,2), sensorInfo.translation[2],
			0.0,            0.0,            0.0,		    1.0;

		// 
		if( NULL != sensorNode->getField( "children" ) )
		{
			traverseShapeNodes( linkInfoIndex, sensorNode->fields["children"].mfNode(), mTransform );
		}
    }
    catch(ModelLoader::ModelLoaderException& ex)
	{
		string error = name_.empty() ? "Unnamed sensor node" : name_;
		error += ": ";
		error += ex.description;
		throw ModelLoader::ModelLoaderException( error.c_str() );
    }
}




//==================================================================================================
/*!
  @if jp

    @brief		Shape ノード探索のための再帰関数

	@note       子ノードオブジェクトを辿り ShapeInfoを生成する。<BR>
                生成したShapeInfoはBodyInfoのshapes_に追加する。<BR>
                shapes_に追加した位置(index)を LinkInfoのshapeIndicesに追加する。<BR>

    @date       2008-03-11 Y.TSUNODA <BR>

	@return	    void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::traverseShapeNodes(
	int linkInfoIndex,				//!< links_ のindex (このlinkInfoに該当するShapeInfoである)
	MFNode& childNodes,				//!< 子Node
	matrix44d mTransform )			//!< 回転・並進 4x4行列
{
	// 対象となる linkInfoインスタンスへの参照
	LinkInfo& linkInfo = links_[linkInfoIndex];

	for( size_t i = 0 ; i < childNodes.size() ; ++i )
	{
		VrmlNodePtr node = childNodes[i];

		// Groupノードとそれを継承したノードの場合を、子ノードを辿っていく
		if( node->isCategoryOf( GROUPING_NODE ) )
		{
			VrmlGroupPtr group = static_pointer_cast<VrmlGroup>( node );

			matrix44d mCurrentTransform( tvmet::identity<matrix44d>() );	// Transformで設定された回転・並進成分を合成した行列

			// Transformノードであるかの判定。
			//   GROUPING_NODEなど、ノードの基本となるカテゴリは isCategoryOf() で判定できるようにしているが、
			//   今のところTransformであるかどうかはそのような基本カテゴリとしていない
			if( VrmlTransformPtr transform = dynamic_pointer_cast<VrmlTransform>( group ) )
			{
				// このノードで設定された transform (scaleも含む)を計算し，4x4の行列に代入する
				matrix44d mThisTransform;
				_calcTransform( transform, mThisTransform );

				// 親ノードで設定された回転・並進成分と合成する
				mCurrentTransform = mTransform * mThisTransform;
			}
			
			// 子ノードの探索
			traverseShapeNodes( linkInfoIndex, group->children, mCurrentTransform );
		}
		// Shapeノードであるかの判定
		else if( node->isCategoryOf( SHAPE_NODE ) )
		{
			short shapeInfoIndex;		// shapeInfoVec(shape_)中のindex

			// shapeInfo共有マップにこのnodeが登録されているか検索する
			SharedShapeInfoMap::iterator itr = sharedShapeInfoMap.find( node );

			// 同じnodeで，transformが同じものが登録されていれば
			if( sharedShapeInfoMap.end() != itr 
			&& ( itr->second.transform != mTransform ) )
			{
				// インデックスを取得する
				shapeInfoIndex = itr->second.index;
			}
			// 登録されていなければ，
			else
			{
				// 整形処理，ShapeInfo を生成する
				UniformedShape uniformShape;
				uniformShape.signalOnStatusMessage.connect( bind( &BodyInfo_impl::putMessage, this, _1 ) );
				uniformShape.setMessageOutput( true );

				if( !uniformShape.uniform( node ) )
                {
                    // 整形処理に失敗したのでShapeInfoは生成しない
                    continue;
                };

                // 整形処理結果を格納
                ShapeInfo_var   shapeInfo( new ShapeInfo );

				// 頂点・メッシュを代入する
                _setVertices( shapeInfo, uniformShape.getVertexList(), mTransform );
				_setTriangles( shapeInfo, uniformShape.getTriangleList() );

				// PrimitiveTypeを代入する
				_setShapeInfoType( shapeInfo, uniformShape.getShapeType() );

				// AppearanceInfo
				{
					VrmlShapePtr shapeNode = static_pointer_cast<VrmlShape>( node );
					VrmlAppearancePtr appearanceNode = shapeNode->appearance;
					if( NULL != appearanceNode )
					{
						AppearanceInfo_var appearance( new AppearanceInfo() );
						//appearance->creaseAngle = 0.0;
						appearance->creaseAngle = 3.14;		// 2008.05.11 Changed. プリミティブ形状 CreaseAngleデフォルト値

						// IndexedFaceSetの場合
						if( UniformedShape::S_INDEXED_FACE_SET == uniformShape.getShapeType() )
						{
							VrmlIndexedFaceSetPtr faceSet = static_pointer_cast<VrmlIndexedFaceSet>( shapeNode->geometry );

							appearance->coloerPerVertex = faceSet->colorPerVertex;
							
							if( NULL != faceSet->color )
							{
								size_t colorNum = faceSet->color->color.size();
								appearance->colors.length( colorNum * 3 );
								for( size_t i = 0 ; i < colorNum ; ++i )
								{
									SFColor color = faceSet->color->color[i];
									appearance->colors[3*i+0] = color[0];
									appearance->colors[3*i+1] = color[1];
									appearance->colors[3*i+2] = color[2];
								}
							}

							size_t colorIndexNum = faceSet->colorIndex.size();
							appearance->colorIndices.length( colorIndexNum );
							for( size_t i = 0 ; i < colorIndexNum ; ++i )
							{
								appearance->colorIndices[i] = faceSet->colorIndex[i];
							}

							appearance->normalPerVertex = faceSet->normalPerVertex;
							appearance->solid = faceSet->solid;
							appearance->creaseAngle = faceSet->creaseAngle;

							// ##### [TODO] #####
							//appearance->textureCoordinate = faceSet->texCood;

							_setNormals( appearance, uniformShape.getVertexList(), uniformShape.getTriangleList(), mTransform );

						}
						// ElevationGridの場合
						else if( UniformedShape::S_ELEVATION_GRID == uniformShape.getShapeType() )
						{
							VrmlElevationGridPtr elevationGrid = static_pointer_cast<VrmlElevationGrid>( shapeNode->geometry );

							appearance->coloerPerVertex = elevationGrid->colorPerVertex;
							
							if( NULL != elevationGrid->color )
							{
								size_t colorNum = elevationGrid->color->color.size();
								appearance->colors.length( colorNum * 3 );
								for( size_t i = 0 ; i < colorNum ; ++i )
								{
									SFColor color = elevationGrid->color->color[i];
									appearance->colors[3*i+0] = color[0];
									appearance->colors[3*i+1] = color[1];
									appearance->colors[3*i+2] = color[2];
								}
							}

							// appearance->colorIndices // ElevationGrid のメンバには無し

							appearance->normalPerVertex = elevationGrid->normalPerVertex;
							appearance->solid = elevationGrid->solid;
							appearance->creaseAngle = elevationGrid->creaseAngle;

							// ##### [TODO] #####
							//appearance->textureCoordinate = elevationGrid->texCood;

							_setNormals( appearance, uniformShape.getVertexList(), uniformShape.getTriangleList(), mTransform );
						}
						// Boxの場合
						else if( UniformedShape::S_BOX == uniformShape.getShapeType() )
						{
							appearance->creaseAngle = (float)(3.14 / 2);
						}
						// Coneの場合
						else if( UniformedShape::S_CONE == uniformShape.getShapeType() )
						{
							appearance->creaseAngle = (float)(3.14 / 2);
						}
						// Cylinderの場合
						else if( UniformedShape::S_CYLINDER == uniformShape.getShapeType() )
						{
							appearance->creaseAngle = (float)(3.14 / 2);
						}
						// Sphereの場合
						else if( UniformedShape::S_SPHERE == uniformShape.getShapeType() )
						{
							appearance->creaseAngle = (float)(3.14 / 2);
						}
						// Extrusionの場合
						else if( UniformedShape::S_EXTRUSION == uniformShape.getShapeType() )
						{
							appearance->creaseAngle = 3.14;
						}


						// MaterialInfo 
						//   materialノードが存在すれば，MaterialInfoを生成，materials_に格納する
						appearance->materialIndex = _createMaterialInfo( appearanceNode->material );


                        // TextureInfo
						//   textureノードが存在すれば，TextureInfoを生成，textures_に格納する
						appearance->textureIndex = _createTextureInfo( appearanceNode->texture );


						long appearancesLength	= appearances_.length();
						appearances_.length( appearancesLength + 1 );
						appearances_[appearancesLength] = appearance;

						// ShapeInfoのappearanceIndexにインデックスを代入
						shapeInfo->appearanceIndex = appearancesLength;
					}
					else
					{
						shapeInfo->appearanceIndex = -1;
					}
				}

				// shapes_の最後に追加する
				int shapesLength = shapes_.length();
				shapes_.length( shapesLength + 1 );
				shapes_[shapesLength] = shapeInfo;

				// shapes_中のindex は
				shapeInfoIndex = shapesLength;

				// shapeInfo共有マップに このshapeInfo(node)とindex,transformの情報をを登録(挿入)する
				ShapeObject shapeObject;
				shapeObject.index = shapeInfoIndex;
				shapeObject.transform = mTransform;
				sharedShapeInfoMap.insert( pair<OpenHRP::VrmlNodePtr, ShapeObject>( node, shapeObject ) );
			}

			// indexを LinkInfo の shapeIndices に追加する
			long shapeIndicesLength = linkInfo.shapeIndices.length();
			linkInfo.shapeIndices.length( shapeIndicesLength + 1 );
			linkInfo.shapeIndices[shapeIndicesLength] = shapeInfoIndex;
		}
	}
}




//==================================================================================================
/*!
  @if jp

    @brief      頂点座標をShapeInfo.verticesに代入

    @note       頂点リストに格納されている頂点座標をShapeInfo.verticesに代入する <BR>
                mTransformとして与えられた回転・並進成分を全ての頂点に反映する <BR>

    @date       2008-03-10 Y.TSUNODA <BR>
	            2008-04-11 Y.TSUNODA 頂点座標を4次元ベクトルにして計算 <BR>

    @return     void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::_setVertices(
    ShapeInfo_var& shape,           //!< 詰め込み対象
	vector<vector3d> vList,			//!< 頂点座標リスト
	matrix44d mTransform )			//!< 回転・並進成分
{
	// 頂点数を取得する
	size_t vertexNumber = vList.size();

	// 頂点座標を格納する配列サイズを指定する
	shape->vertices.length( vertexNumber * 3 );

	int i = 0;
	for( size_t v = 0 ; v < vertexNumber ; v++ )
	{
		vector4d vertex4;			// 回転・並進前のベクトル(座標)	
		vector4d transformed;		// 回転・並進後のベクトル(座標)
		
		// 頂点座標を4次元ベクトルに代入する
		vertex4 = vList.at( v )[0], vList.at( v )[1], vList.at( v )[2], 1; 
		
		// 回転・並進計算
		transformed = mTransform * vertex4;

		// ShapeInfoのverticesに代入する
		shape->vertices[i] = transformed[0]; i++;
		shape->vertices[i] = transformed[1]; i++;
		shape->vertices[i] = transformed[2]; i++;
	}
}




//==================================================================================================
/*!
  @if jp

    @brief      三角メッシュ情報をShapeInfo.trianglesに代入

    @note       三角メッシュリストに格納されている三角メッシュ情報をShapeInfo.trianglesに代入する <BR>

    @date       2008-03-10 Y.TSUNODA <BR>

    @return     void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::_setTriangles(
	ShapeInfo_var& shape,				//!< 対象のShapeInfo
	vector<vector3i> tList )			//!< メッシュリスト
{
	// メッシュ数を取得する
	size_t triangleNumber = tList.size();

	// メッシュを格納する配列サイズを指定する
	shape->triangles.length( triangleNumber * 3 );
	
	int i = 0;
	for( size_t t = 0 ; t < triangleNumber ; t++ )
	{
		shape->triangles[i] = ( tList.at( t ) )[0]; i++;
		shape->triangles[i] = ( tList.at( t ) )[1]; i++;
		shape->triangles[i] = ( tList.at( t ) )[2]; i++;
	}
}




//==================================================================================================
/*!
  @if jp

    @brief      法線を計算しAppearanceInfoに代入

    @note       頂点リスト・三角メッシュリストから法線を計算し，AppearanceInfoに代入する<BR>

    @date       2008-04-11 Y.TSUNODA <BR>

    @return     void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::_setNormals(
	AppearanceInfo_var& appearance,		//!< 計算結果の法線を代入するAppearanceInfo
	vector<vector3d> vertexList,		//!< 頂点リスト
	vector<vector3i> traiangleList,		//!< 三角メッシュリスト
	matrix44d mTransform )				//!< 回転・並進成分
{
	// 頂点リスト中の頂点座標それぞれに，回転・並進成分を掛ける
	vector<vector3d> transformedVertexList;	// 回転・並進計算後の頂点リスト
	vector4d vertex4;						// 回転・並進前のベクトル(座標)	
	vector4d transformed4;					// 回転・並進後のベクトル(座標)
	vector3d transformed;					//    〃

	for( size_t v = 0 ; v < vertexList.size() ; ++v )
	{
		// 頂点座標を4次元ベクトルに代入する
		vertex4 = vertexList.at( v )[0], vertexList.at( v )[1], vertexList.at( v )[2], 1; 
		
		// 回転・並進計算
		transformed4 = mTransform * vertex4;

		transformed = transformed4[0], transformed4[1], transformed4[2];
		transformedVertexList.push_back( transformed );
	}

	CalculateNormal calculateNormal;

	// メッシュの法線(面の法線)を計算する
	calculateNormal.calculateNormalsOfMesh( transformedVertexList, traiangleList );


	// normalPerVertex == TRUE なので，頂点の法線
	if( true == appearance->normalPerVertex )
	{
		calculateNormal.calculateNormalsOfVertex( transformedVertexList, traiangleList, appearance->creaseAngle );

		vector<vector3d> normalsVertex = calculateNormal.getNormalsOfVertex();
		vector<vector3i> normalIndex = calculateNormal.getNormalIndex();

		// 法線データを代入する
		size_t normalsVertexNum = normalsVertex.size();
		appearance->normals.length( normalsVertexNum * 3 );

		for( size_t i = 0 ; i < normalsVertexNum ; ++i )
		{
			// 法線ベクトルを正規化する
			vector3d normal = static_cast<vector3d>( tvmet::normalize( normalsVertex.at( i ) ) );

			// AppearanceInfo のメンバに代入する
			appearance->normals[3*i+0] = normal[0];
			appearance->normals[3*i+1] = normal[1];
			appearance->normals[3*i+2] = normal[2];
		}

		// 法線対応付けデータ(インデックス列)を代入する
		size_t normalIndexNum = normalIndex.size();
		appearance->normalIndices.length( normalIndexNum * 4 );

		for( size_t i = 0 ; i < normalIndexNum ; ++i )
		{
			appearance->normalIndices[4*i+0] = normalIndex.at( i )[0];
			appearance->normalIndices[4*i+1] = normalIndex.at( i )[1];
			appearance->normalIndices[4*i+2] = normalIndex.at( i )[2];
			appearance->normalIndices[4*i+3] = -1;
		}
	}
	// 面の法線
	else
	{
		// 算出した面の法線(のvector:配列)を取得する
		vector<vector3d> normalsMesh = calculateNormal.getNormalsOfMesh();

		// 面の法線データ数を取得する
		size_t normalsMeshNum = normalsMesh.size();

		// 代入する法線，法線インデックスのvector(配列)サイズを指定する
		appearance->normals.length( normalsMeshNum * 3 );
		appearance->normalIndices.length( normalsMeshNum );

		for( size_t i = 0 ; i < normalsMeshNum ; ++i )
		{
			// 法線ベクトルを正規化する
			vector3d normal = static_cast<vector3d>( tvmet::normalize( normalsMesh.at( i ) ) );

			// AppearanceInfo のメンバに代入する
			appearance->normals[3*i+0] = normal[0];
			appearance->normals[3*i+1] = normal[1];
			appearance->normals[3*i+2] = normal[2];

			appearance->normalIndices[i] = i;
		}
	}
}




//==================================================================================================
/*!
  @if jp

    @brief      ShapeInfoにPrimitiveTypeを代入

    @note       <BR>

    @date       2008-04-11 Y.TSUNODA <BR>

    @return     void

  @endif
*/
//==================================================================================================
void
BodyInfo_impl::_setShapeInfoType(
	ShapeInfo_var& shapeInfo,					//!< 対象のShapeInfo
	UniformedShape::ShapePrimitiveType type )	//!< ShapeType
{
    switch( type )
    {
    case UniformedShape::S_BOX:
        shapeInfo->type = BOX;
        break;
    case UniformedShape::S_CONE:
        shapeInfo->type = CONE;
        break;
    case UniformedShape::S_CYLINDER:
        shapeInfo->type = CYLINDER;
        break;
    case UniformedShape::S_SPHERE:
        shapeInfo->type = SPHERE;
        break;
    case UniformedShape::S_INDEXED_FACE_SET:
    case UniformedShape::S_ELEVATION_GRID:
    case UniformedShape::S_EXTRUSION:
        shapeInfo->type = MESH;
        break;
    }
}




//==================================================================================================
/*!
  @if jp

    @brief		TextureInfo生成

	@note       textureノードが存在すれば，TextureInfoを生成，textures_ に追加する。<BR>
				なお，ImageTextureノードの場合は，PixelTextureに変換し TextureInfoを生成する。<BR>
                textures_に追加した位置(インデックス)を戻り値として返す。<BR>

    @date       2008-04-18 Y.TSUNODA <BR>

	@return	    long  TextureInfo(textures_)のインデックス，textureノードが存在しない場合は -1

  @endif
*/
//==================================================================================================
long
BodyInfo_impl::_createTextureInfo( 
	VrmlTexturePtr textureNode )
{
	if( ! textureNode )	return -1;

	VrmlPixelTexturePtr pixelTextureNode = NULL;
	VrmlImageTexturePtr imageTextureNode = dynamic_pointer_cast<VrmlImageTexture>( textureNode );

	// ImageTextureかどうかの判断
	if( imageTextureNode )
	{
		ImageConverter  converter;

		VrmlPixelTexture* tempTexture = new VrmlPixelTexture;

		// PixelTextureに変換する
		if( converter.convert( *imageTextureNode, *tempTexture, _getModelFileDirPath() ) )
		{
			pixelTextureNode = tempTexture;
		}
	}
	// ImageTextureでなければPixelTextureかどうか
	else
	{
		pixelTextureNode = dynamic_pointer_cast<VrmlPixelTexture>( textureNode );
	}

	if( pixelTextureNode )
	{
		TextureInfo_var texture( new TextureInfo() );

		texture->height = pixelTextureNode->image.height;
		texture->width =pixelTextureNode->image.width;
		texture->numComponents = pixelTextureNode->image.numComponents;
		
		size_t pixelsLength =  pixelTextureNode->image.pixels.size();
		texture->image.length( pixelsLength );
		for( size_t j = 0 ; j < pixelsLength ; j++ )
		{
			texture->image[j] = pixelTextureNode->image.pixels[j];
		}
		texture->repeatS = pixelTextureNode->repeatS;
		texture->repeatT = pixelTextureNode->repeatT;

		long texturesLength = textures_.length();
		textures_.length( texturesLength + 1 );
		textures_[texturesLength] = texture;

		return texturesLength;
	}
	else
	{
		return -1;
	}
}




//==================================================================================================
/*!
  @if jp

    @brief		MaterialInfo生成

	@note       materialノードが存在すれば，MaterialInfoを生成，materials_に追加する。<BR>
                materials_に追加した位置(インデックス)を戻り値として返す。<BR>

    @date       2008-04-18 Y.TSUNODA <BR>

	@return	    long  MaterialInfo (materials_)のインデックス，materialノードが存在しない場合は -1

  @endif
*/
//==================================================================================================
long
BodyInfo_impl::_createMaterialInfo(
	VrmlMaterialPtr materialNode )		//!< MaterialNodeへのポインタ
{
	// materialノードが存在すれば
	if( materialNode )
	{
		MaterialInfo_var material( new MaterialInfo() );

		material->ambientIntensity = materialNode->ambientIntensity;
		material->shininess = materialNode->shininess;
		material->transparency = materialNode->transparency;
		for( int j = 0 ; j < 3 ; j++ )
		{
			material->diffuseColor[j] = materialNode->diffuseColor[j];
			material->emissiveColor[j] = materialNode->emissiveColor[j];
			material->specularColor[j] = materialNode->specularColor[j];
		}

		// materials_に追加する
		long materialsLength = materials_.length();
		materials_.length( materialsLength + 1 );
		materials_[materialsLength] = material;

		// 追加した位置(materials_)のインデックスを返す
		return materialsLength;
	}
	else
	{
		return -1;
	}
}




//==================================================================================================
/*!
  @if jp

    @brief      transform計算

    @note       transformノードで指定されたrotation,translation,scaleを計算し，4x4行列に代入する<BR>
				計算結果は第2引数に代入する<BR>

    @date       2008-04-07 Y.TSUNODA <BR>

    @return     bool true:成功 / false:失敗

  @endif
*/
//==================================================================================================
bool
BodyInfo_impl::_calcTransform(
	VrmlTransformPtr transform,		//!< transformノード
	matrix44d&		mOutput )		//!< 計算結果を代入する4x4の行列
{
	// rotationロドリゲスの回転軸
	vector3d vRotation( transform->rotation[0], transform->rotation[1], transform->rotation[2] );

	// ロドリゲスrotationを3x3行列に変換する
	matrix33d mRotation;
	PRIVATE::rodrigues( mRotation, vRotation, transform->rotation[3] );

	// rotation, translation を4x4行列に代入する
	matrix44d mTransform;
	mTransform =
		mRotation(0,0), mRotation(0,1), mRotation(0,2), transform->translation[0],
		mRotation(1,0), mRotation(1,1), mRotation(1,2), transform->translation[1],
		mRotation(2,0), mRotation(2,1), mRotation(2,2), transform->translation[2],
		0.0,            0.0,            0.0,		    1.0;


	// ScaleOrientation
	vector3d scaleOrientation;
	scaleOrientation =	transform->scaleOrientation[0],
						transform->scaleOrientation[1],
						transform->scaleOrientation[2];

	// ScaleOrientationを3x3行列に変換する
	matrix33d mSO;
	PRIVATE::rodrigues( mSO, scaleOrientation, transform->scaleOrientation[3] );

	// スケーリング中心 平行移動
	matrix44d mTranslation;
	mTranslation = 1.0, 0.0, 0.0, transform->center[0],
				   0.0, 1.0, 0.0, transform->center[1],
				   0.0, 0.0, 1.0, transform->center[2],
				   0.0, 0.0, 0.0, 1.0;

	// スケーリング中心 逆平行移動
	matrix44d mTranslationInv;
	mTranslationInv = 1.0, 0.0, 0.0, -transform->center[0],
					  0.0, 1.0, 0.0, -transform->center[1],
				  	  0.0, 0.0, 1.0, -transform->center[2],
					  0.0, 0.0, 0.0, 1.0;

	// ScaleOrientation 回転
	matrix44d mScaleOrientation;
	mScaleOrientation =	mSO(0,0), mSO(0,1), mSO(0,2), 0,
						mSO(1,0), mSO(1,1), mSO(1,2), 0,
						mSO(2,0), mSO(2,1), mSO(2,2), 0,
						0,        0,        0,        1;

	// スケール(拡大・縮小率)
	matrix44d mScale;
	mScale = transform->scale[0],                 0.0,                 0.0, 0.0,
		 	                 0.0, transform->scale[1],                 0.0, 0.0,
			                 0.0,                 0.0, transform->scale[2], 0.0,
			                 0.0,                 0.0,                 0.0, 1.0;

	// ScaleOrientation 逆回転
	matrix44d mScaleOrientationInv;
	mScaleOrientationInv =	mSO(0,0), mSO(1,0), mSO(2,0), 0,
							mSO(0,1), mSO(1,1), mSO(2,1), 0,
							mSO(0,2), mSO(1,2), mSO(2,2), 0,
							0,        0,        0,        1; 

	// transform, scale, scaleOrientation で設定された回転・並進成分を合成する
	mOutput = mTransform
			* mScaleOrientation * mTranslationInv * mScale * mTranslation * mScaleOrientationInv;

	return true;
}




//==================================================================================================
/*!
  @if jp

    @brief      ModelFile(.wrl)のディレクトリパスを取得

    @note       url_のパスからURLスキーム，ファイル名を除去したディレクトリパス文字列を返す<BR>

    @date       2008-04-19 Y.TSUNODA <BR>

    @return     string ModelFile(.wrl)のディレクトリパス文字列

  @endif
*/
//==================================================================================================
string
BodyInfo_impl::_getModelFileDirPath()
{
	// BodyInfo::url_ から URLスキームを削除する
	string filepath = deleteURLScheme( url_ );

	// '/' または '\' の最後の位置を取得する
	size_t pos = filepath.find_last_of( "/\\" );

	string dirPath = "";

	// 存在すれば，
	if( pos != string::npos )
	{
		// ディレクトリパス文字列
		dirPath = filepath;
		dirPath.resize( pos + 1 );
	}

	return dirPath;
}

