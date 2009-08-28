import org.eclipse.swt.widgets.Display as Display

def exec(r):
	display = Display.getDefault()	
	display.syncExec(r)
	return None
