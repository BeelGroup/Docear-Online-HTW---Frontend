require ['views/MapView', 'routers/DocearRouter'],  (MapView, DocearRouter) ->  

  if $('#mindmap-container').size() > 0
    $('#mindmap-container').fadeIn()
    mapView = new MapView('mindmap-viewport')
    mapView.renderAndAppendTo($('#mindmap-container'), true)
    router = new  DocearRouter(mapView)
    mapView.loadMap("welcome")
