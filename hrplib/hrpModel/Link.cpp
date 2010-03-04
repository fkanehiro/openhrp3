/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 */

/**
   \file
   \brief Implementations of Link class
   \author Shin'ichiro Nakaoka
*/


/**
   \ifnot jp
   \class OpenHRP::Link
   A class for representing a rigid body that consists of an articulated body.
   
   \var int Link::index
   Index among all the links in the body.
   
   \endif
*/


/**
   \if jp
   \class OpenHRP::Link
   多関節モデルの中の個々の剛体（リンク）を表すクラス。
   
   \var int Link::index
   全リンクを対象としたリンクのインデックス。
   
   モデルファイルにおけるJointノード定義の出現順（ルートからの探索順）に対応する。
   なお、関節となるリンクのみを対象としたインデックスとして、jointId がある。
   \endif
*/


#include "Link.h"
#include <stack>
#include <hrpCollision/ColdetModel.h>

using namespace std;
using namespace hrp;


Link::Link()
{
    body = 0;
    index = -1;
    jointId = -1;
    jointType = FIXED_JOINT;
    parent = 0;
    sibling = 0;
    child = 0;
    
    isHighGainMode = false;

    defaultJointValue = 0.0;
}


Link::Link(const Link& org)
    : name(org.name)
{
    body = 0;
    index = -1; // should be set by a Body object
    jointId = org.jointId;
    jointType = org.jointType;

    p = org.p;
    R = org.R;
    v = org.v;
    w = org.w;
    dv = org.dv;
    dw = org.dw;

    q = org.q;
    dq = org.dq;
    ddq = org.ddq;
    u = org.u;

    a = org.a;
    d = org.d;
    b = org.b;
    Rs = org.Rs;
    m = org.m;
    I = org.I;
    c = org.c;

    fext = org.fext;
    tauext = org.tauext;

    Jm2 = org.Jm2;

    ulimit = org.ulimit;
    llimit = org.llimit;
    uvlimit = org.uvlimit;
    lvlimit = org.lvlimit;

    defaultJointValue = org.defaultJointValue;
    torqueConst = org.torqueConst;
    encoderPulse = org.encoderPulse;
    Ir = org.Ir;
    gearRatio = org.gearRatio;
    gearEfficiency = org.gearEfficiency;
    rotorResistor = org.rotorResistor;

    isHighGainMode = org.isHighGainMode;

    if(org.coldetModel){
        coldetModel = new ColdetModel(*org.coldetModel);
    }

    parent = child = sibling = 0;

    if(org.child){
        stack<Link*> children;
        for(Link* orgChild = org.child; orgChild; orgChild = orgChild->sibling){
            children.push(orgChild);
        }
        while(!children.empty()){
            addChild(new Link(*children.top()));
            children.pop();
        }
    }

}


Link::~Link()
{
    Link* link = child;
    while(link){
        Link* linkToDelete = link;
        link = link->sibling;
        delete linkToDelete;
    }
}


void Link::addChild(Link* link)
{
    if(link->parent){
        link->parent->detachChild(link);
    }

    link->sibling = child;
    link->parent = this;
    child = link;

    link->setBodyIter(body);
}


/**
   A child link is detached from the link.
   The detached child link is *not* deleted by this function.
   If a link given by the parameter is not a child of the link, false is returned.
*/
bool Link::detachChild(Link* childToRemove)
{
    bool removed = false;

    Link* link = child;
    Link* prevSibling = 0;
    while(link){
        if(link == childToRemove){
            removed = true;
            if(prevSibling){
                prevSibling->sibling = link->sibling;
            } else {
                child = link->sibling;
            }
            break;
        }
        prevSibling = link;
        link = link->sibling;
    }

    if(removed){
        childToRemove->parent = 0;
        childToRemove->sibling = 0;
        childToRemove->setBodyIter(0);
    }

    return removed;
}


void Link::setBodyIter(Body* body)
{
    this->body = body;
    for(Link* link = child; link; link = link->sibling){
        link->setBodyIter(body);
    }
}


std::ostream& operator<<(std::ostream &out, Link& link)
{
    link.putInformation(out);
    return out;
}

void Link::putInformation(std::ostream& out)
{
    out << "Link " << name << " Link Index = " << index << ", Joint ID = " << jointId << "\n";

    out << "Joint Type: ";

    switch(jointType) {
    case FREE_JOINT:
        out << "Free Joint\n";
        break;
    case FIXED_JOINT:
        out << "Fixed Joint\n";
        break;
    case ROTATIONAL_JOINT:
        out << "Rotational Joint\n";
        out << "Axis = " << a << "\n";
        break;
    case SLIDE_JOINT:
        out << "Slide Joint\n";
        out << "Axis = " << d << "\n";
        break;
    }

    out << "parent = " << (parent ? parent->name : "null") << "\n";

    out << "child = ";
    if(child){
        Link* link = child;
        while(true){
            out << link->name;
            link = link->sibling;
            if(!link){
                break;
            }
            out << ", ";
        }
    } else {
        out << "null";
    }
    out << "\n";

    out << "b = "  << b << "\n";
    out << "Rs = " << Rs << "\n";
    out << "c = "  << c << "\n";
    out << "m = "  << m << "\n";
    out << "Ir = " << Ir << "\n";
    out << "I = "  << I << "\n";
    out << "torqueConst = " << torqueConst << "\n";
    out << "encoderPulse = " << encoderPulse << "\n";
    out << "gearRatio = " << gearRatio << "\n";
    out << "gearEfficiency = " << gearEfficiency << "\n";
    out << "Jm2 = " << Jm2 << "\n";
    out << "ulimit = " << ulimit << "\n";
    out << "llimit = " << llimit << "\n";
    out << "uvlimit = " << uvlimit << "\n";
    out << "lvlimit = " << lvlimit << "\n";

    if(false){
        out << "R = " << R << "\n";
        out << "p = " << p << ", wc = " << wc << "\n";
    	out << "v = " << v << ", vo = " << vo << ", dvo = " << dvo << "\n";
    	out << "w = " << w << ", dw = " << dw << "\n";

    	out << "u = " << u << ", q = " << q << ", dq = " << dq << ", ddq = " << ddq << "\n";

    	out << "fext = " << fext << ", tauext = " << tauext << "\n";

    	out << "sw = " << sw << ", sv = " << sv << "\n";
    	out << "Ivv = " << Ivv << "\n";
    	out << "Iwv = " << Iwv << "\n";
    	out << "Iww = " << Iww << "\n";
    	out << "cv = " << cv << ", cw = " << cw << "\n";
    	out << "pf = " << pf << ", ptau = " << ptau << "\n";
    	out << "hhv = " << hhv << ", hhw = " << hhw << "\n";
    	out << "uu = " << uu << ", dd = " << dd << "\n";

    	out << std::endl;
    }
}
