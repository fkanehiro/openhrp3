/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
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
   他関節モデルの中の個々の剛体（リンク）を表すクラス。
   
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
    index = -1;
    jointId = -1;
    parent = 0;
    sibling = 0;
    child = 0;
    
    isHighGainMode = false;

    defaultJointValue = 0.0;
}


Link::Link(const Link& org)
    : name(org.name)
{
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
        coldetModel.reset(new ColdetModel(*org.coldetModel));
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
    }

    return removed;
}


std::ostream& operator<<(std::ostream &out, Link& link)
{
    link.putInformation(out);
    return out;
}


void Link::putInformation(std::ostream& os)
{
    os << "Link " << name << " Link Index = " << index << ", Joint ID = " << jointId << "\n";

    os << "Joint Type: ";

    switch(jointType) {
    case FREE_JOINT:
        os << "Free Joint\n";
        break;
    case FIXED_JOINT:
        os << "Fixed Joint\n";
        break;
    case ROTATIONAL_JOINT:
        os << "Rotational Joint\n";
        os << "Axis = " << a << "\n";
        break;
    case SLIDE_JOINT:
        os << "Slide Joint\n";
        os << "Axis = " << d << "\n";
        break;
    }

    os << "parent = " << (parent ? parent->name : "null") << "\n";

    os << "child = ";
    if(child){
        Link* link = child;
        while(true){
            os << link->name;
            link = link->sibling;
            if(!link){
                break;
            }
            os << ", ";
        }
    } else {
        os << "null";
    }
    os << "\n";

    os << "b = "  << b << "\n";
    os << "Rs = " << Rs << "\n";
    os << "c = "  << c << "\n";
    os << "m = "  << m << "\n";
    os << "Ir = " << Ir << "\n";
    os << "I = "  << I << "\n";
    os << "torqueConst = " << torqueConst << "\n";
    os << "encoderPulse = " << encoderPulse << "\n";
    os << "gearRatio = " << gearRatio << "\n";
    os << "gearEfficiency = " << gearEfficiency << "\n";
    os << "Jm2 = " << Jm2 << "\n";
    os << "ulimit = " << ulimit << "\n";
    os << "llimit = " << llimit << "\n";
    os << "uvlimit = " << uvlimit << "\n";
    os << "lvlimit = " << lvlimit << "\n";

    if(false){
        os << "R = " << R << "\n";
        os << "p = " << p << ", wc = " << wc << "\n";
    	os << "v = " << v << ", vo = " << vo << ", dvo = " << dvo << "\n";
    	os << "w = " << w << ", dw = " << dw << "\n";

    	os << "u = " << u << ", q = " << q << ", dq = " << dq << ", ddq = " << ddq << "\n";

    	os << "fext = " << fext << ", tauext = " << tauext << "\n";

    	os << "sw = " << sw << ", sv = " << sv << "\n";
    	os << "Ivv = " << Ivv << "\n";
    	os << "Iwv = " << Iwv << "\n";
    	os << "Iww = " << Iww << "\n";
    	os << "cv = " << cv << ", cw = " << cw << "\n";
    	os << "pf = " << pf << ", ptau = " << ptau << "\n";
    	os << "hhv = " << hhv << ", hhw = " << hhw << "\n";
    	os << "uu = " << uu << ", dd = " << dd << "\n";

    	os << std::endl;
    }
}
