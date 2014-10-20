import time
import com.generalrobotix.ui.item.GrxWorldStateItem as GrxWorldStateItem
import syncExec
import java.lang.Runnable as Runnable

class MyRunnable(Runnable):
	def run(self):
		item.setPosition(c)
		return None

item  = uimanager.getSelectedItem(GrxWorldStateItem, None)
n = item.getLogSize()
if n > 0:
	print "now auto playing..."

	c = 0
	while 1:
		syncExec.Exec(MyRunnable())
    		c = c + 100
		if c > n:
			c = 0
    		time.sleep(1)
