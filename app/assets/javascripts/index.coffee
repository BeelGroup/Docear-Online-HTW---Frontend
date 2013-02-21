require ['MapController', 'routers/DocearRouter'],  (MapController, DocearRouter) ->  

  
  initializeJsPlumb()
  mapController = new MapController()
  mapController.loadMap(5) # load map 5 as default
  router = new  DocearRouter(mapController)