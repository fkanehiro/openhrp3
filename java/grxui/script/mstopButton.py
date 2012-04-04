#
# emStop Alpha 2
#
import javax.swing.AbstractAction as AbstractAction
import javax.swing.Timer          as Timer
import javax.swing.JFrame         as JFrame
import javax.swing.JPanel         as JPanel
import javax.swing.JOptionPane    as JOptionPane
import javax.swing                as swing

import java.awt.BorderLayout      as BorderLayout
import java.awt                   as awt
import java.awt.Color             as Color
import java.awt.Dimension	  as Dimension

import java.lang.Runnable         as Runnable
import java.lang.System           as System

import java.util.ResourceBundle   as ResourceBundle
import java.util.Locale           as Locale

import org.omg.CORBA              as CORBA
import org.omg.CosNaming          as CosNaming

import time
import sys
import string
import os

# Auditor Stuff
#import com.generalrobotix.hrp.auditor.ConfigBundle    as ConfigBundle
import com.generalrobotix.ui.util.GrxConfigBundle    as GrxConfigBundle

# sys.path.append('emerStopPluginstubskel.jar')
import jp.go.aist.hrp.simulator.PluginHelper as PluginHelper
import jp.go.aist.hrp.simulator.PluginManagerHelper as PluginManagerHelper
import jp.go.aist.hrp.simulator.emerStopPluginHelper as emerStopPluginHelper
import jp.go.aist.hrp.simulator.emerStopPluginPackage.EmerStatus as EmerStatus


ERROR_INPUT_COMMAND	= EmerStatus.ERROR_INPUT_COMMAND
ERROR_INITIALIZE	= EmerStatus.ERROR_INITIALIZE
ERROR_NOT_START		= EmerStatus.ERROR_NOT_START

EMER_STOP_READY		= EmerStatus.EMER_STOP_READY
EMER_STOP_BUSY		= EmerStatus.EMER_STOP_BUSY
EMER_INIT_READY		= EmerStatus.EMER_INIT_READY
EMER_INIT_BUSY		= EmerStatus.EMER_INIT_BUSY
EMER_RESTART_READY	= EmerStatus.EMER_RESTART_READY
EMER_RESTART_BUSY	= EmerStatus.EMER_RESTART_BUSY

global MB
MB = None

ConfigBundle = None

#######################################################################
#
# mstopAction
#
class mstopAction(AbstractAction):
    
    def __init__(self, _btn, _rnc, _f, _fg, _bg):
	self.Button = _btn
	self.rootNC = _rnc
        self.frame = _f
        self.fgColor = _fg
	self.bgColor = _bg
	self.motionSys = None
	self.emStop = None
	self.Status = CORBA.ShortHolder()
	#self.isImStopped = 0
	self.isLogStopped = 0
	self.isLogSaved   = 0
	self.IobClient = None
	self.emerID = 'km'

        
    def getMStopPlugin(self,name):
	try:
	    _n = CosNaming.NameComponent(name, '')
            _km = emerStopPluginHelper.narrow(self.rootNC.resolve([_n]))
            return _km
        
	except:
            self.Button.setText(MB.getObject('emerStop.label.emernoconnection'))
            print 'can not find mstop plugin'
	    return None

    def getObject(self,name):
        try:
	    return self.rootNC.resolve([CosNaming.NameComponent(name,'')])
	except:
	    return None

    def getPlugin(self,name):
	try:
	    return PluginHelper.narrow(self.getObject(name))
	except:
	    return None

    def getPluginManager(self,name):
	try:
            ms = PluginManagerHelper.narrow(self.getObject(name))
	    return ms
        except:
	    return None
        
    def flipColor(self,color1,color2=None):
        if color2 == None:
            color2 = self.bgColor
        if self.Button.getBackground() == color2:
            self.Button.setBackground(color1)
        else:
            self.Button.setBackground(color2)
            
    def actionPerformed(self, event):
        self.frame.getSTPlugin("st")
	if self.emStop != None:
	    try:
		self.Status = self.emStop.getStatus()
		if   self.Status == EMER_STOP_READY:
                    _txt = MB.getObject('emerStop.label.emerstopready')
                    self.Button.setText(_txt)
		    self.Button.setBackground(Color.red)
                    self.Button.setEnabled(1)
                    # enable ST
                    self.frame.getSTPlugin("st")
                    
		elif self.Status == EMER_STOP_BUSY:
                    self.Button.setEnabled(0)
                    _txt = MB.getObject('emerStop.label.emerstopbusy')
                    self.Button.setText(_txt)
		    self.flipColor(Color.red)
                    
		elif self.Status == EMER_INIT_READY:
                    _txt = MB.getObject('emerStop.label.emerinitready')
                    self.Button.setText(_txt)
		    self.Button.setBackground(Color.orange)
                    self.Button.setEnabled(1)
                    
                elif self.Status == EMER_INIT_BUSY:
                    self.Button.setEnabled(0)
                    _txt = MB.getObject('emerStop.label.emerinitbusy')
                    self.Button.setText(_txt)
                    self.flipColor(Color.orange)
                    
                elif self.Status == EMER_RESTART_READY:
                    _txt = MB.getObject('emerStop.label.emerrestartready')
                    self.Button.setText(_txt)
                    self.Button.setBackground(Color.green)
                    self.Button.setEnabled(1)
		    #self.Button.doClick()
                    
                elif self.Status == EMER_RESTART_BUSY:
                    self.Button.setEnabled(0)
                    _txt = MB.getObject('emerStop.label.emerrestartbusy')
                    self.Button.setText(_txt)
		    self.flipColor(Color.green)
                    
                elif self.Status == ERROR_NOT_START:
                    t = 'not started'
                    self.Button.setEnabled(0)
                    _txt = MB.getObject('emerStop.label.emernotstarted')
                    self.Button.setText(_txt)
      		    self.flipColor(Color.blue)
                elif self.Status == ERROR_INITIALIZE:
                    _t = MB.getObject('emerStop.message.errorinitialize')
                    swing.JOptionPane.showMessageDialog(self.frame, _t,
                        'MStop ERROR',JOptionPane.ERROR_MESSAGE)

                elif self.Status == ERROR_INPUT_COMMAND:
                    t = MB.getObject('emerStop.message.errorinputcommand')
                    swing.JOptionPane.showMessageDialog(self.frame, t,
                        'MStop ERROR',JOptionPane.ERROR_MESSAGE)

                if self.Status == EMER_INIT_READY:
                    try:
                        if self.isLogStopped == 0: 
                            self.getPlugin("log").stop()
                            self.isLogStopped = 1
                        #if self.isLogSaved == 0:
                        #    self.IobClient = IOBClient("iob")
                        #    if self.IobClient.isFloating(20):
                        #        self.getPlugin("log").sendMsg(":save '/home/guest/demolog/demo_'+time.strftime('20%y%m%d%H%M')")
                        #        self.isLogSaved = 1
                    except:
                        print 'log is not active'
                        self.isLogStopped = 1
                else:
                    self.isLogStopped = 0
                    self.isLogSaved = 0

                #if (self.Status == EMER_STOP_BUSY) or (self.Status == EMER_INIT_BUSY) or (self.Status == EMER_RESTART_BUSY):
                #    try:
                #        if self.isImStopped == 0: 
		#	    _im = self.getPlugin('im')
		#	    print '_im = ', _im
		#	    _im.stop()
                #            self.isImStopped = 1
		#    except:
		#	print 'im not found'
                #else:
                #    self.isImStopped = 0

            except:
                _txt = MB.getObject('emerStop.label.emernoconnection')
                self.Button.setText(_txt)
		self.Button.setBackground(self.bgColor)
                self.Button.setEnabled(0)
                self.emStop = None

        else:
	    self.emStop = self.getMStopPlugin("km")
        self.updateMotionSys()

    def updateMotionSys(self,update=0,id='motionsys'):
	if self.motionSys == None or update == 1:
            self.motionSys = self.getPluginManager(id)	
	    try:
	        self.echo('[kmstopTest] connected to '+id)
		return 1
            except:
		return 0 
	return 1
   
    def send(self,com):
	try:
	    self.motionSys.sendMsg(':interpret '+com)
	except:
	    if self.updateMotionSys(1) == 1:
	        self.motionSys.sendMsg(':interpret '+com)

    def echo(self,mes):
        dateLabel = time.strftime('20%y/%m/%d %H:%M > ')
	try:
	    self.motionSys.sendMsg(':interpret echo '+dateLabel+mes)
	except:
            if self.updateMotionSys(1) == 1:
                self.motionSys.sendMsg(':interpret echo '+dateLabel+mes)
    


#######################################################################

class emStopTest(JFrame):

    def __init__(self, _rnc, _t, _s):
        global ConfigBundle
	self.rootNC = _rnc
        self.enableST = _s

  	width  = ConfigBundle.getInt("suspendpanel.width" ,400)
  	height = ConfigBundle.getInt("suspendpanel.height",400)
  	posX = ConfigBundle.getInt("suspendpanel.pos.x",0)
  	posY = ConfigBundle.getInt("suspendpanel.pos.y",0)
        bwidth  = ConfigBundle.getInt("suspendpanel.button.width",250)
        bheight = ConfigBundle.getInt("suspendpanel.button.height",250)

	self.frame = swing.JFrame(title='MStop Test', windowClosing=self.onClose)
	if self.enableST:
	    height = height * 2
  	self.setSize(width,height)
  	self.setLocation(posX,posY)
	self.contentPane.setLayout(BorderLayout())
        layoutPanel = JPanel()
	self.contentPane.add(layoutPanel,BorderLayout.CENTER)

        _txt = MB.getObject('emerStop.label.emernoconnection')
	self.kmButton = swing.JButton(_txt,actionPerformed=self.onPressedMStop)
	self.kmButton.setEnabled(0)
        self.kmButton.setPreferredSize(Dimension(bwidth,bheight))
	layoutPanel.add(self.kmButton)

        if self.enableST:
            _txt = MB.getObject('stStop.label.emernoconnection')
            self.stButton = swing.JButton(_txt,actionPerformed=self.onPressedSTStop)
            self.stButton.setEnabled(0)
            self.stButton.setPreferredSize(Dimension(bwidth,bheight))
            layoutPanel.add(self.stButton)
        else:
            self.stButton = None

        self.ST = None # reference to plugin(CORBA)

        self.TimerAction = mstopAction(self.kmButton, self.rootNC, self,
                                       self.foreground, self.background)
        self.updateTimer = Timer(_t, self.TimerAction)
        self.updateTimer.setInitialDelay(0)
        self.updateTimer.setCoalesce(1)
        self.updateTimer.start()

        self.setVisible(1)


    #
    # MStop Stuff
    #
    def onPressedMStop(self, event):
        com = 'send km ' 
	if self.TimerAction.Status == EMER_STOP_READY:
	    com = com+':emerON'
	elif self.TimerAction.Status == EMER_INIT_READY:
	    com = com+':go-initial-bodypos'
        elif self.TimerAction.Status == EMER_RESTART_READY:
	    com = com+':restart'
	else:
            if MB != None:
                _txt = MB.getObject('emerStop.message.errorbuttonpress')
                print _txt%(self.TimerAction.Status)
	    return
        self.TimerAction.send(com)
        self.TimerAction.echo(com)

    #
    # ST stuff
    #
    def getSTPlugin(self,name):
        if self.stButton and not self.stButton.isEnabled():
            try:
                _n = CosNaming.NameComponent(name, "")
                self.ST = PluginHelper.narrow(self.rootNC.resolve([_n]))
                _txt = MB.getObject('stStop.label.emerstopready')
                self.stButton.setText(_txt)
                self.stButton.setEnabled(1) # ST Button enabled here!
            except:
                _txt = MB.getObject('stStop.label.emernoconnection')
                self.stButton.setText(_txt)
                self.ST = None
                print 'can not find st plugin'
    #
    # ST Stop
    #
    def onPressedSTStop(self, event):
        
        # these are reenabled either by mstopAction.actionPerformed
        # or when canceled below
        self.stButton.setEnabled(0)
        self.kmButton.setEnabled(0)

        wmesg  = MB.getObject('stStop.message.emerstopconfirm')
        wtitle = MB.getObject('stStop.title.emerstopconfirm')
        #ret = JOptionPane.showConfirmDialog(self.frame,
        #                                    wmesg,
        #                                    wtitle,
        #                                    JOptionPane.YES_NO_OPTION,
        #                                    JOptionPane.WARNING_MESSAGE)
        ret = JOptionPane.YES_OPTION
        if ret == JOptionPane.YES_OPTION:
            #self.ST.sendMsg(':emergency')
            #print 'send :emergency'
            self.ST.sendMsg(':suspend')
            print 'send :suspend'

        else:
            self.stButton.setEnabled(1)
            self.kmButton.setEnabled(1)

	
    def onClose(self, event):
        System.exit(0)


#######################################################################


def main():
    global MB, ConfigBundle

    #Locale.setDefault(Locale('en', 'US'))
    #MB = ResourceBundle.getBundle('resources.MessageBundle', Locale.US)
    MB = ResourceBundle.getBundle('MessageBundle', Locale.US)
    #MB = ResourceBundle.getBundle('messagebundle', Locale.US) # works
    #MB = ResourceBundle.getBundle('messagebundle')
    #Locale.setDefault(Locale('ja', 'JP'))
    #MB = ResourceBundle.getBundle('resources.Mes sageBundle', Locale.JAPAN)

    _p   = System.getProperties()

    #ConfigBundle = GrxConfigBundle('../grxuirc.xml')
    ConfigBundle = GrxConfigBundle('resources/robot.properties')
    robotType = ConfigBundle.getStr('robot.type')
    print 'robotType = ', robotType
    nsHost = ConfigBundle.getStr('robot.nshost','localhost')
    nsPort = ConfigBundle.getInt('robot.nsport',2809)
    #_a   = string.split(System.getProperty('NS_OPT'))
    _a = ["-ORBInitRef", "NameService=corbaloc:iiop:"+nsHost+":"+str(nsPort)+"/NameService"]
    print _a

    _orb = CORBA.ORB.init(_a, _p)

    try:
	_ns  = _orb.resolve_initial_references('NameService')
	_r   = CosNaming.NamingContextHelper.narrow(_ns)

    except:
        _r   = None
	#print MB.getObject('emerStop.message.nserror')
	print ConfigBundle.getStr('emerStop.message.nserror')

    import getopt
    try:
        #opts, args = getopt.getopt(os.sys.argv[1:], "t:s:")
        opts, args = getopt.getopt(sys.argv[1:], "t:s:")

    except getopt.error, msg:
        print msg

    # setup
    _st = 1
    _t = 750 # default timer loop time

    for o, a in opts:
        if o == '-s': # enable ST button (boolean, true for ON)
	    if a[0] == 'f' or a[0] == 'F' or a[0] == '0':
		_st = 0
		
        elif o == '-t':
            try:
                _t = int(a)
            except:
                print MB.getObject('emerStop.message.tsinvalid'), a, \
                      MB.getObject('emerStop.message.tsdefault')
                _t = 750

    m = emStopTest(_r, _t, _st)

    
if __name__ == "__main__":
    main()

