require ['views/MapView', 'routers/DocearRouter'],  (MapView, DocearRouter) ->  

  if $('#mindmap-container').size() > 0
    mapView = new MapView('mindmap-viewport')
    mapView.renderAndAppendTo($('#mindmap-container'))
    router = new  DocearRouter(mapView)