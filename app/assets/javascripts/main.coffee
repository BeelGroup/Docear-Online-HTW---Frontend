require ['views/MapView','routers/DocearRouter', 'views/RootNodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/RootNode'],  (MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel) -> 

  if document.devmode is on
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

      # usage:
      document.log 'devmode is ON', 'warn'
      #document.log $('#mindmap-container'), 'console'
      #document.log 'devmode is ON'



  if $('#mindmap-container').size() > 0

    $('#mindmap-container').fadeIn()
    
    mapView = new MapView('mindmap-viewport')
    if $.browser.msie and $.browser.version < 9
      console.log 'uncool!'
      mapView.renderAndAppendTo($('#mindmap-container'), false)
    else
      mapView.renderAndAppendTo($('#mindmap-container'))
    router = new  DocearRouter(mapView)

    if typeof mapView.mapId == 'undefined'
    	mapView.loadMap("welcome")
