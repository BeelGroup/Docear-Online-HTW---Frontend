import sys
import time
import logging
import requests
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from watchdog.events import LoggingEventHandler

class custom_handler(FileSystemEventHandler):
  def __init__(self):
    FileSystemEventHandler.__init__(self)
    logging.basicConfig(level=logging.INFO,
                        format='%(asctime)s - %(message)s',
                        datefmt='%Y-%m-%d %H:%M:%S')

  def on_any_event(self, event):
    LoggingEventHandler().dispatch(event)
    path = event.src_path
    if not path.endswith("__"):
      print "Path: ", path
      url = 'http://localhost:5000/'
      files = {'file': open(path, 'rb')}

      r = requests.post(url, files=files, stream=True)
      print r


if __name__ == "__main__":
  event_handler = custom_handler()
  observer = Observer()
  observer.schedule(event_handler, path=sys.argv[1], recursive=True)
  observer.start()
  try:
    while True:
      time.sleep(1)
  except KeyboardInterrupt:
    observer.stop()
  observer.join()