package jp.go.aist.hrp.joystick.views;

import jp.go.aist.hrp.joystick.Activator;
import jp.go.aist.hrp.joystick.rtc.JoystickComp;
import jp.go.aist.rtm.RTC.*;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.SWT;



/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */


public class joystickView extends ViewPart {
	private Canvas canvas;
	private static Point pj = new Point(0,0);
	private static Point PC = pj;
	private boolean mouseDrag = false;
	private Region region;
	
	public joystickView() {
        super();    	
    }
    

    /**
	 *  @brief Joystick コンポーネントの立ち上げ
	 */
	public void execJoystick() {
        System.out.println("[Joystick] COMPONENT START");
        
        String confPath = Activator.getConfigFilePath();
        
        if(confPath=="") {
        	System.out.println("Failed to locate the path to configuration file : rtc.conf");
        	System.out.println("Joystick Aborted");
        } else {
	        String[] args = {"-f", confPath};        
	        
	//      String pluginPath = Activator.getPath();
	//        
	//      // Initialize manager
	//    	String[] args = {"-f", pluginPath+"/rtc.conf"};
	        final Manager manager = Manager.init(args);
	    	
	        // Set module initialization procedure
	        // This procedure will be invoked in activateManager() function.
	        JoystickComp joystickComp = new JoystickComp();
	        manager.setModuleInitProc(joystickComp); 
	
	        if(manager.getPOAManager().get_state().value()!=1) {
				// Activate manager and register to naming service
				manager.activateManager();
				
				// If you want to run the manager in non-blocking mode, do like this
				manager.runManager(true);
	        }
        }
    }    
    
    public static Point getJoystickPosition() {
    	Point pos = new Point(pj.x-PC.x,pj.y-PC.y);
    	pos.y = -pos.y;
    	return pos;
    }

    public void createPartControl(Composite parent) {
    	canvas = new Canvas(parent, SWT.BORDER | SWT.NO_BACKGROUND);
    	final int pitch=50;
    	execJoystick();
    	
       // Create a paint handler for the canvas
        canvas.addPaintListener(new PaintListener() {
          public void paintControl(PaintEvent e) {
            Point p0 = canvas.getLocation();
        	Point p1 = canvas.getSize();
        	Point P0 = new Point(p0.x,p0.y);
        	Point P1 = new Point(p0.x+p1.x,p0.y+p1.y);
        	Point pc = new Point((P0.x+P1.x)/2,(P0.y+P1.y)/2);
        	PC = pc;
        	
        	Color col1 = new Color(e.display,221,221,221);
        	Color col2 = new Color(e.display,238,238,238);
        	Color white= new Color(e.display,255,255,255);
        	Color color; 
        	
        	Image image = new Image(canvas.getDisplay(), canvas.getBounds());
        	GC gcImage = new GC(image);
        	
            gcImage.setBackground(e.gc.getBackground());
            gcImage.fillRectangle(image.getBounds());
            
            gcImage.setForeground(white);
            
            for(int i=10;i>0;i--) {
            	p0.x = pc.x - pitch * i;
            	p0.y = pc.y - pitch * i;
            	p1.x = pc.x + pitch * i;
            	p1.y = pc.y + pitch * i;
            	
            	if(i%2 == 0) {
            		color = col1;
            	} else {
            		color = col2;
            	}
            	gcImage.setBackground(color);
            	gcImage.fillOval(p0.x,p0.y,p1.x-p0.x,p1.y-p0.y);                
            	gcImage.drawOval(p0.x,p0.y,p1.x-p0.x,p1.y-p0.y);
            }
            gcImage.setForeground(e.display.getSystemColor(SWT.COLOR_BLACK));
            gcImage.drawLine(pc.x, P0.y, pc.x, P1.y);
            gcImage.drawLine(P0.x, pc.y, P1.x, pc.y);
              
            gcImage.drawString("x", P1.x-20, pc.y+10);
            gcImage.drawString("y", pc.x+10, P0.y);
            
            if(!mouseDrag) {
            	pj = new Point(pc.x,pc.y);
            }
            
            Point pos = getJoystickPosition();
            double _r = Math.round(Math.hypot(pos.x, pos.y)*100)/100.0;
            double _th =  Math.round(Math.toDegrees(Math.atan2(pos.y,pos.x))*100)/100.0;
            gcImage.drawString("x: "+pos.x, (pc.x+P1.x)/2-20, P1.y-40);
            gcImage.drawString("y: "+pos.y, (pc.x+P1.x)/2+55, P1.y-40);
            gcImage.drawString("r: "+_r, (pc.x+P1.x)/2-20, P1.y-20);
            gcImage.drawString("th: "+_th, (pc.x+P1.x)/2+50, P1.y-20);
            
            gcImage.setBackground(col1);
            gcImage.drawLine(pc.x, pc.y, pj.x, pj.y);
            gcImage.fillOval(pj.x-10, pj.y-10, 20, 20);                
            gcImage.drawOval(pj.x-10, pj.y-10, 20, 20);
            
            e.gc.drawImage(image, 0, 0);

            image.dispose();
            gcImage.dispose();
            
            region = new Region();
            region.add(new Rectangle(pj.x-10, pj.y-10, 20, 20));
          }
        });
        
        canvas.addMouseListener(new MouseAdapter() {
        	public void mouseDown(MouseEvent e) {
        		Point ep = new Point(e.x, e.y);
        		if(region.contains(ep)) {
        			mouseDrag = true;
        		}
        	}
        	
        	public void mouseUp(MouseEvent e) {
        		mouseDrag = false;
        		canvas.redraw();
        	}
        });
        
        canvas.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				if(mouseDrag) {
					pj.x = e.x;
					pj.y = e.y;
					canvas.redraw();
				}
			} 
        });
    }

    public void setFocus() { 
    	
    }

} 