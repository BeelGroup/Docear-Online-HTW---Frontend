require ['views/MapView','routers/DocearRouter', 'views/RootNodeView', 'views/CanvasView', 'views/MinimapView', 'views/ZoomPanelView', 'models/RootNode'],  (MapView, DocearRouter, RootNodeView, CanvasView, MinimapView, ZoomPanelView, RootNodeModel) -> 

  if $('#mindmap-container').size() > 0

    $('#mindmap-container').fadeIn()
    
    mapView = new MapView('mindmap-viewport')
    mapView.renderAndAppendTo($('#mindmap-container'))
    router = new  DocearRouter(mapView)

    if typeof mapView.mapId == 'undefined'
    	mapView.loadMap("welcome")
