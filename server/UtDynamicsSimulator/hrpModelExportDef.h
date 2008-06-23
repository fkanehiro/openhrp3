// -*- mode: c++; indent-tabs-mode: t; tab-width: 4; c-basic-offset: 4; -*-
#ifndef OPENHRP_CONFIG_H_INCLUDED
#define OPENHRP_CONFIG_H_INCLUDED

// for Windows DLL export 
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
# ifdef HRPMODEL_MAKE_DLL
#   define HRPMODEL_EXPORT __declspec(dllexport)
# else 
#   define HRPMODEL_EXPORT __declspec(dllimport)
# endif
#else 
# define HRPMODEL_EXPORT 
#endif /* Windows */


#endif
