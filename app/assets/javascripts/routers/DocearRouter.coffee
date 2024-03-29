define ['logger'],(logger) ->
  module = ->
  
  class DocearRouter extends Backbone.Router

    ## splat parameter (*) (regex)
    ## reg ex example: '/^(.*?)$/' : 'doIt'
    routes:
      'project/:projectId/map/*path': 'loadMap'
      'closeMap/*path': 'closeMap'
      '*path': 'notFound'

    constructor:(@mapView)->
      super()
      Backbone.history.stop()
      Backbone.history.start()

    loadMap: (projectId, mapId)->
      document.log "Load map #{mapId} from project #{projectId} (DocearRouter.loadMap())" 
      @mapView.loadMap projectId, mapId
    
    closeMap: (mapId)->
      @mapView.closeMap mapId

    notFound:(params)->
      document.log 'Route not found: #{params}', 'warn'

  module.exports = DocearRouter