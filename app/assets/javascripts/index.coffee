require ['views/MapView', 'routers/DocearRouter'],  (MapView, DocearRouter) ->  

  if $('#mindmap-container').size() > 0
    initializeJsPlumb()
    mapView = new MapView('mindmap-viewport')
    mapView.renderAndAppendTo($('#mindmap-container'))
    router = new  DocearRouter(mapView)
    mapView.loadMap(1)