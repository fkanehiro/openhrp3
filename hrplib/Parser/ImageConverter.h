/*!
  @file ImageConverter.h
  @brief Header file of Image Converter class
  @author K.FUKUDA
*/

#ifndef OPENHRP_PARSER_IMAGECONVERTER_H_INCLUDED
#define OPENHRP_PARSER_IMAGECONVERTER_H_INCLUDED

#include "config.h"

extern "C" {
#define XMD_H
#include "jpeglib.h"
}

#include "png.h"
#include "VrmlNodes.h"

using namespace std;

namespace hrp
{

    class ImageConverter
    {
    private:
        bool    initializeSFImage( SFImage & image );
        bool    loadPNG( string & filePath, SFImage & image );
        bool    loadJPEG( string & filePath, SFImage & image );

    public:
        ImageConverter(void){};
        virtual ~ImageConverter(void){};

        HRP_PARSER_EXPORT bool convert( VrmlImageTexture & imageTexture, VrmlPixelTexture & pixelTexture, string dirPath = "" );
    };

};

#endif

