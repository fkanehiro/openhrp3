package jp.go.aist.hrp.simulator;

// CORBA
import org.omg.CORBA.*;
import org.omg.PortableServer.*;

import jp.go.aist.hrp.simulator.ModelLoaderPackage.*;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

/**
 * CharacterInfoを生成するファクトリ
 */
public class ModelLoader_impl extends ModelLoaderPOA 
{
    private ORB orb_;
    private POA poa_;

    //urlをキーにしたキャッシュ
    private Hashtable<String, CharacterInfo_impl> cache_;
	private Map<String, Date> lastModified_;

    /**
     * コンストラクタ
     *
     * @param   orb     ORBへの参照
     * @param   poa     POAへの参照
     */
    public ModelLoader_impl(ORB orb, POA poa) {
        orb_ = orb;
        poa_ = poa;
        cache_ = new Hashtable<String, CharacterInfo_impl>();
		lastModified_ = new HashMap<String, Date>();
    }

    /**
     * CharacterInfo生成
     *
     * @param   url
     * @return  CharacterInfo
     */
    public CharacterInfo loadURL(String url) throws ModelLoaderException
    {
        CharacterInfo_impl cinfo_impl = cache_.get(url);
        if (cinfo_impl == null)
			return reloadURL(url);
	
		Date lm = _getLastModified(url);
		if (lm != null && lm.after(lastModified_.get(url))) {
       		System.out.println("\nupdate cache for " + url);
			return reloadURL(url);
		}

        System.out.println("cache found for "+url);
        try{
            return CharacterInfoHelper.narrow(poa_.servant_to_reference(cinfo_impl));
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }


    public CharacterInfo reloadURL(String url) throws ModelLoaderException
	{
	    cache_.remove(url);
        CharacterInfo_impl cinfo_impl = new CharacterInfo_impl(url, orb_, poa_);
        cache_.put(url, cinfo_impl);

		Date lm = _getLastModified(url); 
		lastModified_.remove(url);
		lastModified_.put(url, lm);
		System.out.println("Last modified : " + lm);

        try{
            return CharacterInfoHelper.narrow(poa_.servant_to_reference(cinfo_impl));
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
	}
    
    public void clearData(){
        cache_.clear();
        lastModified_.clear();
    }
    
    public void shutdown(){
        System.exit(0);
      //orb_.shutdown(false);
    }

	private Date _getLastModified(String url) {
        try {
            File f = new File(new URL(url).getFile());
			return new Date(f.lastModified());
        } catch (Exception ex) {
            ex.printStackTrace(); 
        }
		return null;
	}
}
