require ['views/MapView', 'routers/DocearRouter'],  (MapView, DocearRouter) ->  

  
  initializeJsPlumb()
  mapView = new MapView('mindmap-container')
  mapView.renderAndAppendTo($('.container'))
  router = new  DocearRouter(mapView)