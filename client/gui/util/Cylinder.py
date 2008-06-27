#!/usr/bin/env python
#/*
# * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
# * All rights reserved. This program is made available under the terms of the
# * Eclipse Public License v1.0 which accompanies this distribution, and is
# * available at http://www.eclipse.org/legal/epl-v10.html
# * Contributors:
# * General Robotix Inc.
# * National Institute of Advanced Industrial Science and Technology (AIST) 
# */

#
# create inline-able cylinder
#
#
import os
import math

def outputFile(coords, coordIndex, fname, l, r, n, a):
    
    outfile = open(fname, 'w')

    outfile.write('#VRML V2.0 utf8\n')
    outfile.write('# called with\n')
    outfile.write('# radius = %f, length = %f, numfaces = %d, axis = %c\n' \
                  %(l,r,n,chr(a+ord('X'))))
    outfile.write('Shape {\n')
    outfile.write('appearance Appearance {\n')
    outfile.write('material Material {\n')
    outfile.write('diffuseColor 0.0 0.0 1.0\n')
    outfile.write('}\n')
    outfile.write('}\n')
    outfile.write('geometry IndexedFaceSet {\n')
    outfile.write('coord Coordinate {\n')
    outfile.write('point [\n')
    
    for item in coords:
        outfile.write('%f %f %f,\n'%(item[0], item[1], item[2]))
        
    outfile.write(']\n')
    outfile.write('}\n')
    outfile.write('coordIndex [\n')
    
    for item in coordIndex:
        outfile.write('%d, %d, %d, -1,\n'%(item[0], item[1], item[2]))

    outfile.write(']\n')
    outfile.write('solid FALSE\n')
    outfile.write('}\n')
    outfile.write('}\n')
    outfile.close()

#
# Switch axes
#
def rotateCoords(c, a):

    if   a == 0: # z, y, x
        return [[item[2], item[1], item[0]] for item in c]
    
    elif a == 1: # x, z, y
        return [[item[0], item[2], item[1]] for item in c]
        
    return c


def calcCylinder(l, r, n):
    
    zz = l/2
    drad = 2*math.pi/n

    coords = [[0.0, 0.0,  zz]] \
             + [[r*math.cos(drad*i), r*math.sin(drad*i),  zz]
                for i in range(n)] \
             + [[0.0, 0.0, -zz]] \
             + [[r*math.cos(drad*i), r*math.sin(drad*i), -zz]
                for i in range(n)]
    
    # top
    coordIndex = [[0, i, i+1] for i in range(n)] + [[0, n, 1]]
    
    # bottom
    offset = n+1
    coordIndex += [[offset, i+offset, i+1+offset] for i in range(1,n)] \
                  + [[offset, n+offset, offset+1]]

    # sides
    coordIndex += [[i, i+1, i+offset] for i in range(1,n)] \
                  + [[n, 1, n+offset]] \
                  + [[i+offset, i+1+offset, i+1] for i in range(1,n-1)] \
                  + [[2*n, n+offset, n]] \
                  + [[n+offset, offset+1, 1]]

    return coords, coordIndex


def parseArgs():

    length = None
    radius = None
    nElem = None
    fname = None
    axis = None
    
    import getopt
    try:
        opts, args = getopt.getopt(os.sys.argv[1:], 'f:hl:r:n:xyz')
        
    except getopt.error, msg:
        print msg
        return length, radius, nElem

    for o, a in opts:
        if o == '-l':
            length = float(a)
        elif o == '-n':
            nElem = int(a)
        elif o == '-r':
            radius = float(a)
        elif o == '-f':
            fName = a
        elif o == '-x': # z, y, x
            axis = 0
        elif o == '-y': # x, z, y
            axis = 1
        elif o == '-z': # x, y, z
            axis = 2
        elif o == '-h':
            print """
Cylinder.py:
-r : radius
-l : length
-n : number of faces(circle)
-f : file name (.wrl suffix added automatically)
-x : cylinder around X axis
-y : cylinder around Y axis
-z : cylinder around Z axis
-h : print this help
"""
            os.sys.exit(0)
        else:
            print 'Unknown Option:', o

    if length == None:
        length = 1.0

    if nElem == None:
        nElem = 16

    if radius == None:
        radius = 1.0

    if axis == None:
        axis = 2

    return length,radius,nElem,fName,axis
            
def main():
    l,r,n,f,a = parseArgs()
    c, cI = calcCylinder(l, r, n)
    c = rotateCoords(c, a)
    outputFile(c, cI, f+'.wrl',l,r,n,a)

if __name__ == '__main__':
    main()
