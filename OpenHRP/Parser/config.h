/*
 * Copyright (c) 2008, AIST, the University of Tokyo and General Robotix Inc.
 * All rights reserved. This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * National Institute of Advanced Industrial Science and Technology (AIST)
 * General Robotix Inc. 
 */

/*! @file
  @author Shin'ichiro Nakaoka
*/

#ifndef OPENHRP_PARSER_CONFIG_H_INCLUDED
#define OPENHRP_PARSER_CONFIG_H_INCLUDED

// for Windows DLL export 
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
# ifdef HRP_PARSER_MAKE_DLL
#   define HRP_PARSER_EXPORT __declspec(dllexport)
# else 
#   define HRP_PARSER_EXPORT __declspec(dllimport)
# endif
#else 
# define HRP_PARSER_EXPORT
#endif /* Windows */

#endif
