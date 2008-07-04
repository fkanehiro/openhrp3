/**
   \file
   \brief This file defines a macro for exporting functions from a DLL of Windows
   \author S.NAKAOKA
*/

#ifndef MODEL_PARSER_CONFIG_H_INCLUDED
#define MODEL_PARSER_CONFIG_H_INCLUDED

// for Windows DLL export 
#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__) || defined(__NT__)
# ifdef HRPMODELPARSER_MAKE_DLL
#   define MODELPARSER_EXPORT __declspec(dllexport)
# else 
#   define MODELPARSER_EXPORT __declspec(dllimport)
# endif
#else 
# define MODELPARSER_EXPORT 
#endif /* Windows */

#endif
