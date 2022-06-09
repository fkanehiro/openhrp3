from javax.swing import *

def act(evt):
  print("punch!!!")

f = JFrame()
c = f.getContentPane()
c.add(JButton("Punch", actionPerformed = act))
f.setSize(200,200)
f.setVisible(1)