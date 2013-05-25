define ['logger', 'NodeFactory', 'models/RootNode', 'models/Node', 'handlers/PersistenceHandler'],  (logger, NodeFactory, RootNode, Node, PersistenceHandler) ->  
  module = ->

  ###
  Usage:
    // create a loader object and call firstLoad for initial load 
    mapLoader = new MapLoader(data);
    rootView = new RootNodeView mapLoader.firstLoad()

    // continue loading in 50ms steps (now you can browse the map while loading)
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
      @showUs = Array()
      @renderAllNNodes = 200
      @renderTimes = 1


    render:->
      for element in @showUs
        #document.log 'partial rendering'
        element.show()
      @showUs = []
      @rootView.model.trigger 'refreshDomConnectionsAndBoundaries'

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
        @loadStep @rootNode.getNextParentToLoad()


    loadStep:(parentToLoadNode)=>
      #document.log 'Amount of nodes: '+document.nodeCount+' Current parent node id: '+parentToLoadNode.get 'id'
      # render all n nodes
      if (document.nodeCount - (document.nodeCount % @renderAllNNodes)) is @renderAllNNodes*@renderTimes
        @renderTimes++
        @render()

      if @continue
        if parentToLoadNode isnt undefined and parentToLoadNode isnt 'undefined'
          @getDataAndRenderNodesByID parentToLoadNode
        if @rootNode.getParentsToLoadSize() > 0 and @continue = true
          window.setTimeout @loadStep,document.sleepTimeOnPartialLoading, @rootNode.getNextParentToLoad()
        else
          document.log 'finished loading'
          @render()

    appendNodesToParent:(dataOfParentNode, parentNode)=>
      if dataOfParentNode isnt null

        # create child node models
        childs = @nodeFactory.createChildrenRecursive dataOfParentNode.children, parentNode, @rootNode

        # add children to list of childerns 
        parentNode.set 'children', (parentNode.get 'children').concat childs

        # show the + sign, when childs of the current parent are loaded
        if (parentNode.get 'folded') and !(parentNode.get 'foldedShow')
          $('#'+parentNode.get 'id').find('.inner-node .expand-icon').show()
          parentNode.set 'foldedShow', true

        # append the new childs to the dom
        $childElement = $('#'+parentNode.get 'id').find('.children:first')
        @rootView.recursiveRender $childElement, childs

        # hide the new childs ... they will be shown again and also layouted in the render method of this class
        if (parentNode.get 'folded') isnt on
          $childElement.hide()
          @showUs.push $childElement

    getDataAndRenderNodesByID:(parentToLoadNode)=>
      href = jsRoutes.controllers.MindMap.getNode(@mapId, parentToLoadNode.get 'id', document.loadChunkSize).url
      
      # WORKAROUND
      correctURL = href.replace '-1', 'nodeCount='+document.loadChunkSize

      request = $.ajax(
        invokedata:
          maploader: @
          parentToLoadNode: parentToLoadNode
        url: correctURL
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

  module.exports = MapLoader      