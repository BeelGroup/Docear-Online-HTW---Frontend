define ['logger', 'NodeFactory', 'models/RootNode', 'models/Node', 'handlers/PersistenceHandler'],  (logger, NodeFactory, RootNode, Node, PersistenceHandler) ->  
  module = ->

  ###
  Usage:
    // create a loader object and call firstLoad for initial load 
    mapLoader = new MapLoader(data);
    rootView = new RootNodeView mapLoader.firstLoad()

    // continue loading in 400ms steps (now you can browse the map during loading)
    mapLoader.continueLoading()

    // be happy
  ###
  class MapLoader

    constructor:(@data, @mapId)->
      @nodeFactory = new NodeFactory()
      @rootNodeWasPassed = false
      @stillDataToLoad = true
      @rendering = off
      @removeFromList = Array()
      @continue = true

    injectRootView:(@rootView)->

    stop:->
      @continue = false


    # load root data with some childs
    firstLoad:->
      @rootNode = @nodeFactory.createRootNodeByData(@data, null, @mapId)
      @rootNodeWasPassed = true
      @continue = true
      @rootNode


    continueLoading:->
      if @rootNode.getParentsToLoadSize() > 0 and @continue = true
        window.setTimeout @loadMore, 40
      else
        @rootView.model.trigger 'refreshDomConnectionsAndBoundaries' 
        document.log 'finished loading'


    # load / render / append next nodes
    loadMore:=>
      @loadStep @rootNode.getNextParentToLoad()
      
      # only do that, when unfolded!
      @rootView.model.trigger 'refreshDomConnectionsAndBoundaries'
      document.log 'load step'


    loadStep:(parentToLoadNode)=>
      if @continue
        if parentToLoadNode isnt undefined and parentToLoadNode isnt 'undefined'
          @getDataAndRenderNodesByID parentToLoadNode
        if @rootNode.getParentsToLoadSize() > 0 and @continue = true
          window.setTimeout @loadStep,100, @rootNode.getNextParentToLoad()
        else
          @continueLoading()


    appendNodesToParent:(dataOfParentNode, parentNode)=>
      if dataOfParentNode isnt null
        childs = @nodeFactory.createChildrenRecursive dataOfParentNode.children, parentNode, @rootNode
        # add children to list of childerns
        parentNode.set 'children', (parentNode.get 'children').concat childs
        #newNode.set 'children', childs

        if (parentNode.get 'folded') and !(parentNode.get 'foldedShow')
          $('#'+parentNode.get 'id').find('.inner-node .action-fold').show()
          parentNode.set 'foldedShow', true

        # append to dom
        @rootView.recursiveRender $('#'+parentNode.get 'id').find('.children:first'), childs


    getDataAndRenderNodesByID:(parentToLoadNode)=>
      href = jsRoutes.controllers.MindMap.getNode(@mapId, parentToLoadNode.get 'id', document.loadChunkSize).url
      request = $.ajax(
        invokedata:
          maploader: @
          parentToLoadNode: parentToLoadNode
        url: href
        type: 'GET',
        async: true,
        dataType: "json"
        cache: false,
        timeout: 30000,
        fail: ->
          document.log "error on loading childs. Textstatus:"+textStatus
        ,
        success: (data)-> @invokedata.maploader.appendNodesToParent(data, parentToLoadNode)
      )

      if request.status is 200
        request.responseText
      else 
        null

    ### 
      appendNodesToParent called by getDataById (when ajax request was successfull)
      appentNodeToParent calls continueLOading. when nodes were added
    ###
  module.exports = MapLoader      