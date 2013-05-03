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

  	constructor:(@data)->
      @nodeFactory = new NodeFactory()
      @rootNodeWasPassed = false
      @stillDataToLoad = true

      # debugging
      @testcalls = -1

    injectRootView:(@rootView)->

    continueLoading:->
      if @stillDataToLoad
        window.setTimeout @loadStep, 400

        
    loadStep:=>
      @loadMore()
      # should work ... but doesnt
      @rootView.refreshDom()
      @rootView.connectChildren()
      document.log @rootView, 'console'
      @continueLoading()
      
     
    # load root data with some childs
    firstLoad:->
      @rootNode = @nodeFactory.createRootNodeByData(@data, null)
      @rootNodeWasPassed = true
      @rootNode


    # load / render / append next nodes
    loadMore:->
      for node in @rootNode.getNodeList() 
        if typeof (renderUs = node.get 'childsToLoad') isnt 'undefined'
          for renderMe in renderUs  
            @appendNodesToParent renderMe, node
          node.set 'childsToLoad', 'undefined'


    appendNodesToParent:(id, parentNode)->
      #debugging
      if id is 'ID_1616796999' or id is 'ID_1891755637' or id is 'ID_452528799' or id is 'ID_1279466317'
        data = @getDataByID id
        nodesToAppend = @nodeFactory.getRecursiveChildren data, parentNode, @rootNode
        # add children to list of childerns
        parentNode.set 'children', (parentNode.get 'children').concat nodesToAppend
        # append to dom
        @rootView.recursiveRender $('#'+parentNode.get 'id').find('.children:first'), nodesToAppend
        document.log nodesToAppend, 'console'


    getDataByID:(id)->
      @testcalls++
      # DEBUGGING
      # ID_1616796999.children
      if @testcalls is 0
        data0 = [{"id":"ID_1616796999","nodeText":"You can add images to a node","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0}]
      # ID_1891755637.children
      else if @testcalls is 1
        data1 = [{"id":"ID_1891755637","nodeText":"They can be in any standard format","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_871612900","nodeText":"JPEG","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0},{"id":"ID_1546013773","nodeText":"GIF","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0},{"id":"ID_1792512538","nodeText":"PNG","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0}],"hGap":0,"shiftY":0}]
      else if @testcalls is 2
      # ID_452528799.children
        data2 = [{"id":"ID_452528799","nodeText":"Just copy and paste or drag&drop an image to the mind map","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0}]
      # ID_1279466317.children
      else if @testcalls is 3
        @stillDataToLoad = false
        data3 = [{"id":"ID_1279466317","nodeText":"Example","isHtml":false,"folded":false,"icons":[],"children":[{"id":"ID_1178635212","nodeText":"Docear Logo","isHtml":false,"folded":false,"icons":[],"children":[],"hGap":0,"shiftY":0}],"hGap":0,"shiftY":0}]


      ###
       jsRoutes.controllers.MindMap.mapAsJson(mapId).url
        /map/mapId/json?-1 

      ###






  module.exports = MapLoader      