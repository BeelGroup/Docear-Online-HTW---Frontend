define ['logger'],(logger) ->
  module = ->
  
  class DocearRouter extends Backbone.Router

    ## splat parameter (*) (regex)
    ## reg ex example: '/^(.*?)$/' : 'doIt'
    routes:
      'map/:mapId': 'loadMap'
      'doSomathingWithMap/:id/*action':  'resource'
      '*path': 'notFound'


    constructor:(@mapController)->
      super()
      Backbone.history.stop()
      Backbone.history.start()

    loadMap: (mapId)->
      @mapController.renderMap mapId

    notFound:()->
      document.log 'Route not found', 'warn'



  module.exports = DocearRouter