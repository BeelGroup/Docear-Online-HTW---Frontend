define ['models/RootNode', 'models/Node', 'handlers/PersistenceHandler'],  (RootNode, Node, PersistenceHandler) ->  
  module = ->

  class NodeFactory

    persistenceHandlers = []
  
    ###
      todo:
        remember nodes with childs, which still need to be rendered
           - List of nodes to be rendered
             - each with their parent objects (for adding)
        getter for this childs
        should be used in a loader class
    ###

    createRootNodeByData:(data, containerID, mapId)->
    
      rootNode = new RootNode()
      rootNode.set 'containerID', containerID
      rootNode.set 'leftChildren', []
      rootNode.set 'rightChildren', []
      rootNode.set 'mapId', data.id
      rootNode.set 'folded', false

      if persistenceHandlers[data.id] == undefined
        persistenceHandlers[data.id] = new PersistenceHandler(mapId)
      
      @setDefaults(rootNode, rootNode, data.root)
      rootNode.activateListeners()

      if data.root.leftChildren != undefined
        leftNodes = @getRecursiveChildren(data.root.leftChildren, rootNode, rootNode)
        rootNode.set 'leftChildren', leftNodes
      
      if data.root.rightChildren != undefined
        rightNodes = @getRecursiveChildren(data.root.rightChildren, rootNode, rootNode)
        rootNode.set 'rightChildren', rightNodes

      rootNode


    createNodeByData:(data, rootNode, parent)->

      node = new Node()
      node.set 'children', []
      node.set 'parent', parent
      node.set 'folded', data.folded

      @setDefaults(node, rootNode, data)
      node.activateListeners()

      node


    setDefaults:(node, rootNode, data)->

      node.set 'id', data.id
      node.set 'nodeText', data.nodeText
      node.set 'isHTML', data.isHtml
      node.set 'xPos', 0
      node.set 'yPos', 0
      node.set 'hGap', 0
      node.set 'shiftY', 0
      node.set 'locked', false

      node.set 'rootNodeModel', rootNode
      node.set 'selected', false
      
      node.set 'previouslySelected', false
      node.set 'foldable', ($.inArray('FOLD_NODE', document.features) > -1)
      node.set 'lastAddedChild', 'undefined'
      node.set 'connectionUpdated', 0

      node.set 'persistenceHandler', persistenceHandlers[rootNode.get('mapId')]
      node.set 'attributesToPersist', ['folded', 'nodeText', 'isHTML', 'locked']

      node.setEdgestyle(data.edgeStyle)


    getRecursiveChildren:(childrenData, parent, root)->

      children = []
      if childrenData.id != undefined && childrenData.id != null
        newChild = @createNodeByData(childrenData, root, parent)
        children.push newChild
      else if childrenData != undefined
        for child in childrenData
          newChild = @createNodeByData(child, root, parent)
          if child.children != undefined
            newChild.set 'children', @getRecursiveChildren(child.children, newChild, root)
          children.push newChild
      children


    createNodeByText:(text)->  



  module.exports = NodeFactory