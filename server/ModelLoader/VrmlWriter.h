#ifndef __VRML_WRITER_H__
#define __VRML_WRITER_H__

#include <iostream>
#include <hrpCorba/ModelLoader.hh>

class VrmlWriter
{
public:
    void write(OpenHRP::BodyInfo_var binfo, std::ostream &ofs);
private:
    void writeProtoNodes(std::ostream &ofs);
    void writeHumanoidNode(OpenHRP::BodyInfo_var binfo, std::ostream &ofs);
    void writeLink(int index, std::ostream &ofs);
    void writeShape(OpenHRP::TransformedShapeIndex &tsi, std::ostream &ofs);
    void indent(std::ostream &ofs);
    int m_indent;
    OpenHRP::LinkInfoSequence_var links;
    OpenHRP::ShapeInfoSequence_var shapes;
    OpenHRP::AppearanceInfoSequence_var appearances;
    OpenHRP::MaterialInfoSequence_var materials;
    OpenHRP::TextureInfoSequence_var textures;
};

#endif
