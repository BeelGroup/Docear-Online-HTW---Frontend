require [],  () ->  

  # usage of logger:
  #document.log 'devmode is ON', 'warn'
  #document.log $('#mindmap-container'), 'console'
  #document.log 'devmode is ON'
  document.logging = $("body").attr 'data-logging-strategy'
  if document.logging is 'popup'
    require ['dev/log4js-mini'], () -> 
      document.logger = new Log(Log.DEBUG, Log.popupLogger)
      document.log = (messageOrObject, mode = 'debug')->
        if mode is 'debug'
          document.logger.debug messageOrObject
        else if mode is 'warn'
          document.logger.warn messageOrObject
        else if mode is 'console'
          console.log messageOrObject

        # scroll to bottom
        y = $(document.logger._window.document).height()
        document.logger._window.scroll(0,y);

      # onload message
      document.log 'devmode is ON', 'warn'
  # dont't log in livemode    
  else 
    document.log = ->