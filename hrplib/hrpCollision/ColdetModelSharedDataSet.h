
/**
   @author Shin'ichiro Nakaoka
*/


#include "ColdetModel.h"
#include "Opcode.h"
#include <vector>

using namespace std;
using namespace hrp;

namespace hrp {

    class ColdetModelSharedDataSet
    {
    public:
        ColdetModelSharedDataSet();

        void build();

        // need two instances ?
        Opcode::Model model;

	Opcode::MeshInterface iMesh;

	vector<IceMaths::Point> vertices;
	vector<IceMaths::IndexedTriangle> triangles;

        double translation[3];
	double rotation[3][3];

      private:
        int refCounter;

        friend class ColdetModel;
    };
}
