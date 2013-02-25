require ['views/MapView', 'routers/DocearRouter'],  (MapView, DocearRouter) ->  

  if $('#mindmap-container').size() > 0
    initializeJsPlumb()
    mapView = new MapView('mindmap-container')
    mapView.renderAndAppendTo($('.container'))
    router = new  DocearRouter(mapView)