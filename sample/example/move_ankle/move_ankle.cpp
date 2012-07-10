#include <hrpUtil/OnlineViewerUtil.h>
#include <hrpUtil/Eigen3d.h>
#include <hrpModel/Link.h>
#include <hrpModel/LinkTraverse.h>
#include <hrpModel/JointPath.h>
#include <hrpModel/ModelLoaderUtil.h>

using namespace std;
using namespace hrp;
using namespace OpenHRP;

enum {X, Y, Z};
#define deg2rad(x) ( 3.14159265358979 / 180*(x) )

int
main(int argc, char* argv[])
{
    int i;
    string url = "file://";
    // -urlでモデルのURLを指定   //
    for(i=0; i < argc; i++) {
        if( strcmp(argv[i], "-url") == 0 && i+1 < argc) url += argv[i+1];
    }

    // モデルロード  //
    BodyPtr body(new Body());
    if(!loadBodyFromModelLoader(body, url.c_str(), argc, argv)){
        cerr << "ModelLoader: " << url << " cannot be loaded" << endl;
        return 0;
    }

    body->calcForwardKinematics();

    // OnlineViewer設定  //
    OnlineViewer_var olv = getOnlineViewer(argc, argv);
    try {
        olv->load(body->modelName().c_str(), url.c_str());
        olv->setLogName("move_ankle");
    } catch (CORBA::SystemException& ex) {
        cerr << "Failed to connect GrxUI." << endl;
        return 1;
    }

	// 特異点にならないよう最初は曲げておく  //
    body->joint(1)->q = deg2rad(-10);
    body->joint(3)->q = deg2rad(20);
    body->joint(4)->q = deg2rad(-10);
    body->calcForwardKinematics();

    // 腰から足首までのパスを設定  //
    Link* waist = body->link("WAIST");
    Link* ankle = body->link("RLEG_ANKLE_R");
    JointPathPtr path = body->getJointPath(waist, ankle);

    // WorldStateを作成する  //
    WorldState world;
    world.characterPositions.length(1);
    world.collisions.length(0);

    // SampleRobot用CharacterPosition  //
    //world.collisions.length(0);
    CharacterPosition& robot = world.characterPositions[0];
    robot.characterName = CORBA::string_dup("SampleRobot");

    // 時間は0  //
    world.time=0;

    while (1) {
        // 時間を進める  //
        world.time+=0.01;

        // 少し動かす  //
        Vector3 p = ankle->p;
        Matrix33 R = ankle->R;
        p(2) += 0.002;

        // もし逆運動学計算に失敗したら終わり  //
        if (!path->calcInverseKinematics(p, R)) {
            break;
        }

        // LinkをWorldStateにコピーする。  //
        int n = body->numLinks();
        robot.linkPositions.length(n);
        for (int i=0; i<n; i++) {
            Link* link = body->link(i);
            setVector3(link->p, robot.linkPositions[i].p);
            setMatrix33ToRowMajorArray(link->R, robot.linkPositions[i].R);
        }

        // OnlineViewer アップデート  //
        try {       
            olv->update(world);
        } catch (CORBA::SystemException& ex) {
            std::cerr << "OnlineViewer could not be updated." << endl;
            return 1;
        }
    }

    return 0;
}
