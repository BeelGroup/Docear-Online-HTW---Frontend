define ['NodeFactory', 'models/RootNode', 'models/Node', 'handlers/PersistenceHandler'],  (NodeFactory, RootNode, Node, PersistenceHandler) ->  
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

    injectRootView:(@rootView)->

    # load root data with some childs
    firstLoad:->
      @rootNode = @nodeFactory.createRootNodeByData(@data, null, @mapId)
      @rootNodeWasPassed = true
      @rootNode


    continueLoading:->
      if Object.keys(@rootNode.getUnfinishedNodes()).length > 0
        window.setTimeout @loadMore, 400
      else
        @rootNode.trigger 'finishedLoading' 

          
    # load / render / append next nodes
    loadMore:=>
      items = @rootNode.getUnfinishedNodes()
      ids = Object.keys(items);
      for id in ids
        if id isnt undefined and id isnt 'undefined'
          @getDataByID id, items[id]

      @rootView.refreshDom()
      @rootView.connectChildren()


      @continueLoading()


    appendNodesToParent:(data, parentNode)=>
      #console.log data
      if data isnt null
        newNode = @nodeFactory.createNodeByData data, parentNode, @rootNode 
        childs = @nodeFactory.createChildrenRecursive data.children, newNode, @rootNode
        # add children to list of childerns
        parentNode.set 'children', (parentNode.get 'children').concat newNode
        newNode.set 'children', childs
        # append to dom
        @rootView.recursiveRender $('#'+parentNode.get 'id').find('.children:first'), {newNode}
        # nodes were loaded, remove index from "still to load list"
        delete @rootNode.getUnfinishedNodes()[data.id]
        #console.log Object.keys(@rootNode.getUnfinishedNodes()).length 



    getDataByID:(nodeId, myparent)->
      href = jsRoutes.controllers.MindMap.getNode(@mapId, nodeId, 1).url
      console.log href
      request = $.ajax(
        invokedata: {
          maploader: @
          myparent: myparent
        }
        url: href
        type: 'GET',
        async: true,
        dataType: "json"
        cache: false,
        timeout: 30000,
        fail: ->
          document.log "error on loading childs. Textstatus:"+textStatus
        ,
        success: (data)-> @invokedata.maploader.appendNodesToParent(data, @invokedata.myparent)
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