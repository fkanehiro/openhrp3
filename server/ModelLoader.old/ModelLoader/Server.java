package jp.go.aist.hrp.simulator;

// CORBA
import org.omg.CORBA.*;
import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.PortableServer.*;

/**
 * Server start up class
 * @author  Ichitaro Kohara, MSTC
 * @version 1.01(2001.02.09)
 *          Server class is defined as final class
 */
public final class Server
{
    /**
     * Server start up
     * @param   args    command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            String context="ModelLoader";
            for (int i=0; i<args.length; i++){
                if (args[i].equals("-context")){
                context = args[++i];
                }
            }

            // initialize ORB and POA
            java.util.Properties props = System.getProperties();
            ORB orb = ORB.init(args, props);
            POAManager manager = null;
	    POA rootPOA = null;
            try
            {
                if(orb == null)
                {
                    System.out.println("orb is nil");
                }
                org.omg.CORBA.Object CORBA_obj = orb.resolve_initial_references("RootPOA");
                rootPOA = POAHelper.narrow(CORBA_obj);
                manager = rootPOA.the_POAManager();

            }catch(org.omg.CORBA.ORBPackage.InvalidName ex)
            {
                ex.printStackTrace();
                return;
            }catch(Exception ex)
            {
                ex.printStackTrace();
                return;
            }


            // getting reference of nameserver
            org.omg.CORBA.Object nameserver = orb.resolve_initial_references("NameService");
            NamingContext rootnc = NamingContextHelper.narrow(nameserver);

            // bind modelloder
            ModelLoader_impl loder = new ModelLoader_impl(orb, rootPOA);
            ModelLoader loder_obj = loder._this(orb);
            NameComponent nc = new NameComponent(context, "");
            NameComponent[] path = {nc};
		rootnc.rebind(path, loder_obj);
            System.out.println("context="+context);
            // waiting connection fro client
            System.out.println("ready");
            try
            {
                manager.activate();
            }catch(Exception ex)
            {
                ex.printStackTrace();
                return;
            }
            orb.run();
            System.out.println("loop end!");
            if(orb != null)
            {
                //
                // Since the standard ORB.destroy() method is not present in
                // JDK 1.2.x, we must cast to com.ooc.CORBA.ORB so that this
                // will compile with all JDK versions
                //
                try
                {
                orb.destroy();
                }
                catch(Exception ex)
                {
                ex.printStackTrace();
                }
            }

            System.exit(0);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
