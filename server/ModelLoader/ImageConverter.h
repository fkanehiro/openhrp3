/*!
  @file ImageConverter.h
  @brief Header file of Image Converter class
  @author K.FUKUDA
*/

#ifndef IMAGECONVERTER_H_INCLUDED
#define IMAGECONVERTER_H_INCLUDED

extern "C" {
#define XMD_H
#include "jpeglib.h"
}

#include "png.h"
#include <VrmlNodes.h>

using namespace std;

namespace OpenHRP
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

        bool convert( VrmlImageTexture & imageTexture, VrmlPixelTexture & pixelTexture, string dirPath = "" );
    };

};

#endif	// IMAGECONVERTER_H_INCLUDED
