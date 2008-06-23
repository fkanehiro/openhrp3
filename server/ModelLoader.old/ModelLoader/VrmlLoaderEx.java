package jp.go.aist.hrp.simulator;

import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.ParsingErrorException;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.lang.String;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.Reader;

/**
 * This is an implementation of the Java3D utility loader interface.
 * 
 * A VrmlSceneEx can be loaded via URL, filesystem pathname or a 
 * Reader.
 *
 * The "base" URL or pathname is the pathname used for relative paths in the 
 * WRL.   If the base is not specified it will be derived from the main URL or
 * pathname.
 *
 * @see Scene
 * @see com.sun.j3d.loaders.Loader
 * @see com.sun.j3d.loaders.Scene
 *
 * @Author: Rick Goldberg
 * @Author: Doug Gehringer
 *
 *
 */
public class VrmlLoaderEx extends com.sun.j3d.loaders.LoaderBase {

    com.sun.j3d.loaders.vrml97.impl.Loader loader;

    public VrmlLoaderEx(int flags) {
    super(flags);
    loader = new com.sun.j3d.loaders.vrml97.impl.Loader(null,
        com.sun.j3d.loaders.vrml97.impl.Loader.LOAD_STATIC);
    }

    public VrmlLoaderEx() {
    this(0);
    }
    
    // don't null references, we are using this for a converter if true.
    public VrmlLoaderEx(boolean b) {
    super(0);
    loader= new com.sun.j3d.loaders.vrml97.impl.Loader(null,
    com.sun.j3d.loaders.vrml97.impl.Loader.LOAD_CONVERTER);
    }

    public com.sun.j3d.loaders.Scene load(Reader reader) 
        throws FileNotFoundException, IncorrectFormatException,
        ParsingErrorException {
    com.sun.j3d.loaders.vrml97.impl.Scene scene;
    //loader.setWorldURL(baseUrl,  null);
    try {
        scene = loader.load(reader);
    } catch (Exception e) {
        // TODO: make a better handler
        System.err.println("Exception loading URL: " + e);
        e.printStackTrace(System.err);
        throw new ParsingErrorException();
    }
    return new VrmlSceneEx(scene);
    }

    public com.sun.j3d.loaders.Scene load(String pathname)
        throws FileNotFoundException, IncorrectFormatException,
        ParsingErrorException {
    
    URL url = pathToURL(pathname);
    loader.setWorldURL(pathToURL(basePath), url);
    return doLoad(url);
    }

    public com.sun.j3d.loaders.Scene load(URL url) 
        throws FileNotFoundException, IncorrectFormatException,
        ParsingErrorException {
    loader.setWorldURL(baseUrl, url);
    return doLoad(url);
    }

    private com.sun.j3d.loaders.Scene doLoad(URL url) {
    com.sun.j3d.loaders.vrml97.impl.Scene scene;
    try {
        scene = loader.load(url);
    } catch (Exception e) {
        // TODO: make a better handler
        System.err.println("Exception loading URL: " + e);
        e.printStackTrace(System.err);
        throw new ParsingErrorException();
    }
    return new VrmlSceneEx(scene);
    }

    // this method should only be used by methods that expect to only operate
    // on filesystem resident files, and those methods should not be used.
    private URL pathToURL(String path) {
    URL retval = null;
    if (path == null) {
        return null;
    }
    
    // hack: for win32 we check drive specifier
    //       for solaris we check startsWith /

    // this really should all take a clean look at URL(context,spec) 

    if (!path.startsWith(java.io.File.separator) && (path.indexOf(':') != 1)){
        path = System.getProperties().getProperty("user.dir") + '/' + path;
    }

    // switch from file separator to URL separator
    path = path.replace(java.io.File.separatorChar, '/');
    try {
        retval = new URL("file:" + path);
    } catch (MalformedURLException me) {
        System.out.println("Can't make a URL from path: " + path);
    }
    return retval;
    }

}
