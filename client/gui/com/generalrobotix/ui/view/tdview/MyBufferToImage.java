// Decompiled by Jad v1.5.7f. Copyright 2000 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/SiliconValley/Bridge/8617/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BufferToImage.java

package com.generalrobotix.ui.view.tdview;

import java.awt.*;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.util.Vector;
import javax.media.*;
import javax.media.format.*;

public class MyBufferToImage
{

    public MyBufferToImage(VideoFormat format)
    {
        converterNotRequired = false;
        if((format instanceof YUVFormat) || (format instanceof RGBFormat))
        {
            this.format = format;
            size = format.getSize();
            prefFormat = new RGBFormat(size, size.width * size.height, Format.intArray, format.getFrameRate(), 32, -1, -1, -1, 1, -1, 0, -1);
            if(format.matches(prefFormat))
            {
                converterNotRequired = true;
                return;
            }
            Codec codec = findCodec(format, prefFormat);
            if(codec != null)
                converter = codec;
            outputBuffer = new Buffer();
        }
    }

    private Codec findCodec(VideoFormat input, VideoFormat output)
    {
        Vector codecList = PlugInManager.getPlugInList(input, output, 2);
        if(codecList == null || codecList.size() == 0)
            return null;
        for(int i = 0; i < codecList.size(); i++)
        {
            String codecName = (String)codecList.elementAt(i);
            Class codecClass = null;
            Codec codec = null;
            try
            {
                codecClass = Class.forName(codecName);
                if(codecClass != null)
                    codec = (Codec)codecClass.newInstance();
            }
            catch(ClassNotFoundException _ex) { }
            catch(IllegalAccessException _ex) { }
            catch(InstantiationException _ex) { }
            catch(ClassCastException _ex) { }
            if(codec != null && codec.setInputFormat(input) != null)
            {
                Format outputs[] = codec.getSupportedOutputFormats(input);
                if(outputs != null && outputs.length != 0)
                {
                    for(int j = 0; j < outputs.length; j++)
                        if(outputs[j].matches(output))
                        {
                            Format out = codec.setOutputFormat(outputs[j]);
                            if(out != null && out.matches(output))
                                try
                                {
                                    codec.open();
                                    return codec;
                                }
                                catch(ResourceUnavailableException _ex) { }
                        }

                }
            }
        }

        return null;
    }

    public Image createImage(Buffer buffer)
    {
        if(buffer == null || converter == null && !converterNotRequired || prefFormat == null || buffer.getFormat() == null || !buffer.getFormat().matches(format) || buffer.getData() == null || buffer.isEOM() || buffer.isDiscard())
            return null;
        int outputData[];
        RGBFormat vf;
        try
        {
            if(converterNotRequired)
            {
                outputData = (int[])buffer.getData();
                vf = (RGBFormat)buffer.getFormat();
            } else
            {
                int retVal = converter.process(buffer, outputBuffer);
                if(retVal != 0)
                    return null;
                outputData = (int[])outputBuffer.getData();
                vf = (RGBFormat)outputBuffer.getFormat();
            }
        }
        catch(Exception ex)
        {
            System.err.println("Exception " + ex);
            return null;
        }
        Image outputImage = null;
        
        int redMask = vf.getRedMask();
        int greenMask = vf.getGreenMask();
        int blueMask = vf.getBlueMask();
        DirectColorModel dcm = new DirectColorModel(32, redMask, greenMask, blueMask);
        MemoryImageSource sourceImage = new MemoryImageSource(size.width, size.height, dcm, outputData, 0, size.width);
        outputImage = Toolkit.getDefaultToolkit().createImage(sourceImage);
        return outputImage;
    }

    private VideoFormat format;
    private Codec converter;
    private RGBFormat prefFormat;
    private Buffer outputBuffer;
    private Dimension size;
    private boolean converterNotRequired;
}
