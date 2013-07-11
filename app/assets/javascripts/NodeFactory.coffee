define ['logger', 'models/mindmap/RootNode', 'models/mindmap/Node', 'handlers/PersistenceHandler', 'handlers/MindMapUpdateHandler'],  (logger, RootNode, Node, PersistenceHandler, MindMapUpdateHandler) ->  
  module = ->

  class NodeFactory

    persistenceHandlers = []
  

    createRootNodeByData:(data, containerID, @mapId, @projectId)->
      rootNode = new RootNode()
      rootNode.set 'containerID', containerID
      rootNode.set 'leftChildren', []
      rootNode.set 'rightChildren', []
      
      # data.id should be filename
      rootNode.set 'mapId', @mapId
      rootNode.set 'mapName', data.id
      rootNode.set 'folded', false
      rootNode.set 'revision', data.revision
      
      isReadonly = data.isReadonly
      if !($.inArray('ENABLE_READONLY_MAP', document.features) > -1)
        isReadonly = false
      rootNode.set 'isReadonly', isReadonly

      if persistenceHandlers[@mapId] == undefined
        persistenceHandlers[@mapId] = new PersistenceHandler(@mapId, @projectId, isReadonly)

      if !isReadonly
        mindMapUpdateHandlers = new MindMapUpdateHandler(@mapId, rootNode, @projectId)
        rootNode.set 'mindMapUpdateHandler', mindMapUpdateHandlers
      
      @setDefaults(rootNode, rootNode, data.root)
      rootNode.activateListeners()

      if data.root.leftChildren isnt undefined
        leftNodes = @createChildrenRecursive(data.root.leftChildren, rootNode, rootNode)
        rootNode.set 'leftChildren', leftNodes
      
      if data.root.rightChildren isnt undefined
        rightNodes = @createChildrenRecursive(data.root.rightChildren, rootNode, rootNode)
        rootNode.set 'rightChildren', rightNodes

      rootNode


    createNodeByData:(data, parent, rootNode)->
      node = new Node()
      node.set 'children', []
      node.set 'parent', parent
      node.set 'folded', data.folded
      if data.childrenIds isnt undefined 
        node.set 'childsToLoad', data.childrenIds
        rootNode.addParentToParentToLoadList(node)
        for id in data.childrenIds
          rootNode.addNodetoUnfinishedList(id, node)

      rootNode.addNodeToList node

      @setDefaults(node, rootNode, data)
      node.activateListeners()

      node


    setDefaults:(node, rootNode, data)->
      node.set 'id', data.id

      if not data.isHtml
        nodeTexts = data.nodeText.split '\n'
        # if there are linebreaks
        if nodeTexts.length > 1
          container = $("<div></div>")
          # create DIVs for each line and append a whitespace on their end if required
          for currentLine in nodeTexts
            if currentLine.slice(-1) isnt '-'
              container.append $("<div>#{currentLine.concat '&nbsp;'}</div>")
            else
              container.append $("<div>#{currentLine}</div>")
          # in this case force isHtml to true and set text 
          data.isHtml = true
          data.nodeText = container.html()

      node.set 'nodeText', data.nodeText
      node.set 'isHtml', data.isHtml
      
      node.set 'xPos', 0
      node.set 'yPos', 0
      node.set 'hGap', 0
      node.set 'shiftY', 0
      
      if data.locked isnt undefined and data.locked != ""
        node.lock data.locked
      else
        node.unlock()

      node.set 'rootNodeModel', rootNode
      node.set 'selected', false
      
      node.set 'previouslySelected', false
      node.set 'foldable', ($.inArray('FOLD_NODE', document.features) > -1)
      node.set 'lastAddedChild', 'undefined'
      node.set 'connectionUpdated', 0

      node.set 'persistenceHandler', persistenceHandlers[rootNode.get('mapId')]
      node.set 'attributesToPersist', ['folded', 'nodeText', 'isHtml']
      node.set 'autoPersist', false

      node.set 'foldedShow', false

      node.setEdgestyle(data.edgeStyle)


    createChildrenRecursive:(childrenData, parent, root)->
      children = []
      if childrenData != undefined
        for child in childrenData
          newChild = @createNodeByData(child, parent, root)
          if child.children != undefined
            newChild.set 'children', @createChildrenRecursive(child.children, newChild, root)
          children.push newChild
      children


    createNodeByText:(text)->  
      # dont forget to add the new node to the list
      # -> rootNode.addNodeToList node


  module.exports = NodeFactory