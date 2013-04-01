require ['views/MapView','routers/DocearRouter', 'views/RootNodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/RootNode'],  (MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel) -> 

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
