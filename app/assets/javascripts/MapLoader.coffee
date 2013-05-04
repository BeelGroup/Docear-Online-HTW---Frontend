define ['NodeFactory', 'models/RootNode', 'models/Node', 'handlers/PersistenceHandler'],  (NodeFactory, RootNode, Node, PersistenceHandler) ->  
  module = ->

  class MapLoader

  	constructor:(@data, @mapId)->
      @nodeFactory = new NodeFactory()
      @rootNodeWasPassed = false

    load:->
      if @rootNodeWasPassed then @loadNext() else @firstLoad()

    loadNext:->
      null

    firstLoad:->
      @rootNode = @nodeFactory.createRootNodeByData(@data, null, @mapId)
      @rootNodeWasPassed = true
      @rootNode

  module.exports = MapLoader      