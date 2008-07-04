/**
   \file
   \brief This file defines a macro for exporting functions from a DLL of Windows
   \author M.YASUKAWA
*/
#define HRPMODELUNIFORM_MAKE_DLL

#ifndef MODEL_UNIFORM_CONFIG_H_INCLUDED
#define MODEL_UNIFORM_CONFIG_H_INCLUDED

// for Windows DLL export 
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
# ifdef HRPMODELUNIFORM_MAKE_DLL
#   define MODELUNIFORM_EXPORT __declspec(dllexport)
# else 
#   define MODELUNIFORM_EXPORT __declspec(dllimport)
# endif
#else 
# define MODELUNIFORM_EXPORT 
#endif /* Windows */

#endif
