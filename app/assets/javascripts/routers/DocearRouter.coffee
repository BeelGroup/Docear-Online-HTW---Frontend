define [],() ->
  module = ->
  
  class DocearRouter extends Backbone.Router


    ## splat parameter (*) (regex)
    ## reg ex example: '/^(.*?)$/' : 'doIt'
    routes:
      'map/:mapId': 'loadMap'
      'doSomathingWithMap/:id/*action':  'resource'
      #'*path': 'notFound'


    doIt:(id, action) -> console.log "do #{action} with map #{id}"  

    constructor:(@mapController)->
      super()
      Backbone.history.stop()
      Backbone.history.start()

    loadMap: (mapId)->
      @mapController.renderMap mapId

    #notFound:()->
    #  console.log 'Route not found'



  module.exports = DocearRouter