import time

print "now auto playing..."
logger = uimanager.getView("Logger View")
c = 0
while 1:
  if not logger.isPlaying(): 
    logger.play()
    c = c + 1
    print str(c) + " time"
  time.sleep(5)
