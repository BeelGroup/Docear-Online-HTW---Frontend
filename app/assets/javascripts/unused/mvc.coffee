require ['MapController', 'routers/DocearRouter'],  (MapController, DocearRouter) ->  

  
  initializeJsPlumb()
  mapController = new MapController('mindmap-container')
  mapController.renderAndAppendTo($('.container'))
  router = new  DocearRouter(mapController)
