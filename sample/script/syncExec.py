import org.eclipse.swt.widgets.Display as Display

def Exec(r):
	display = Display.getDefault()	
	display.syncExec(r)
	return None
