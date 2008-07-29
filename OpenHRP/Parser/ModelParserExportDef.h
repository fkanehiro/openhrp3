// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
#define HRPMODELPARSER_MAKE_DLL


#ifndef OPENHRP_CONFIG_H_INCLUDED
#define OPENHRP_CONFIG_H_INCLUDED


// for Windows DLL export 
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
# ifdef HRPMODELPARSER_MAKE_DLL
#   define HRPMODELPARSER_EXPORTS __declspec(dllexport)
# else 
#   define HRPMODELPARSER_EXPORTS __declspec(dllimport)
# endif
#else 
# define HRPMODELPARSER_EXPORTS 
#endif /* Windows */

#endif
