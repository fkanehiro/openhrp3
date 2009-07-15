/*!
  @file ImageConverter.h
  @brief Header file of Image Converter class
  @author K.FUKUDA
*/

#ifndef OPENHRP_UTIL_IMAGECONVERTER_H_INCLUDED
#define OPENHRP_UTIL_IMAGECONVERTER_H_INCLUDED

#include "config.h"

#include "VrmlNodes.h"

using namespace std;

namespace hrp
{

    class ImageConverter
    {
    private:
        bool    initializeSFImage();
        bool    loadPNG( string & filePath );
        bool    loadJPEG( string & filePath );

    public:
        SFImage* image;
        ImageConverter(void){
            image = new SFImage;
        };
        virtual ~ImageConverter(void){
            delete image;
        };

        HRP_UTIL_EXPORT SFImage* convert( string url );
    };

};

#endif

