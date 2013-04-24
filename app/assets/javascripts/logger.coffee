require [],  () ->  


  # usage of logger:
  #document.log 'devmode is ON', 'warn'
  #document.log $('#mindmap-container'), 'console'
  #document.log 'devmode is ON'
  if document.body.className.match 'js-logging'
    require ['dev/log4js-mini'], () -> 
      document.logger = new Log(Log.DEBUG, Log.popupLogger)
      document.log = (messageOrObject, mode = 'debug')->
        if document.devmode is on
          if mode is 'debug'
            document.logger.debug messageOrObject
          else if mode is 'warn'
            document.logger.warn messageOrObject
          else if mode is 'console'
            console.log messageOrObject
      document.log 'devmode is ON', 'warn'
      
  else 
    document.log = ->