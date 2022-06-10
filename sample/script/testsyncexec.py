import syncExec
import java.lang.Runnable as Runnable

class MyRunnable(Runnable):
	def run(self):
		print("a")
		return None

syncExec.Exec(MyRunnable())

