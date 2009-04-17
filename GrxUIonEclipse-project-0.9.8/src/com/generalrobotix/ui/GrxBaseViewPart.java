package com.generalrobotix.ui;

import java.lang.reflect.Constructor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import com.generalrobotix.ui.grxui.Activator;

public class GrxBaseViewPart extends ViewPart {

	protected GrxBaseView v=null;

	//public GrxBaseViewPart(){System.out.println(this);}

	public GrxBaseView getGrxBaseView() {
		return v;
	}

	public void createPartControl(Composite parent) {
		createView( GrxBaseView.class, "Base View", this, parent );
	}

	public void createView(Class <?extends GrxBaseView> cls, String name, GrxBaseViewPart vp, Composite p ){
		Constructor<? extends GrxBaseView> c = null;
		Activator act = Activator.getDefault();
		try {
			c = cls.getConstructor(new Class[] { String.class, GrxPluginManager.class, GrxBaseViewPart.class, Composite.class });
			v = (GrxBaseView) c.newInstance(new Object[] { name, act.manager_, vp, p });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setFocus() {}
	
	public void dispose(){
		v.shutdown();
	}
} 
