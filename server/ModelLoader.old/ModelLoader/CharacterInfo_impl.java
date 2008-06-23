package jp.go.aist.hrp.simulator;

// CORBA
import org.omg.CORBA.*;
import org.omg.PortableServer.*;

public final class CharacterInfo_impl extends CharacterInfoPOA
{
    private ORB orb_ = null;
    private POA poa_ = null;

    String name_, url_;
    String[] info_;
    LinkInfo[] linkinfos_;
    
    // accessors
    public String name() { return name_; }
    public String url() { return url_; }
    public String[] info() { return info_; }
    public LinkInfo[] links() { return linkinfos_; }
    
    // constructor
    CharacterInfo_impl(String url, ORB orb, POA poa)
    {
        orb_ = orb;
        poa_ = poa;
        url_ = url;

        ObjectStruct obj = new ObjectStruct(url);

        //Humanoid
        HumanoidInfo hInfo = obj.getHumanoidInfo();
        name_ = hInfo.name_;
        info_ = hInfo.info_;

        LinkInfo_impl[] linfo_impls = obj.getLinkInfos();
        linkinfos_ = new LinkInfo[linfo_impls.length];
        
        org.omg.CORBA.Object o;
        for(int iCount = 0;iCount < linfo_impls.length;iCount++){
            //System.out.println("count is : " + iCount);
            try{
                //SensorInfo
                linfo_impls[iCount].sensors_ 
                    = new SensorInfo[linfo_impls[iCount].sensorInfoSeqImpl_.length];
                for(int j=0;j<linfo_impls[iCount].sensorInfoSeqImpl_.length ; j++){
                    o = poa_.servant_to_reference(linfo_impls[iCount].sensorInfoSeqImpl_[j]);
                    linfo_impls[iCount].sensors_[j] = SensorInfoHelper.narrow(o);
                }
                //LinkInfo
                o = poa_.servant_to_reference(linfo_impls[iCount]);
                linkinfos_[iCount] = LinkInfoHelper.narrow(o);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }
}
