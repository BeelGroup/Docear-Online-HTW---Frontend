define ['logger', 'models/RootNode', 'models/Node', 'handlers/PersistenceHandler'],  (logger, RootNode, Node, PersistenceHandler) ->  
  module = ->

  class NodeFactory

    ###
      todo:
        remember nodes with childs, which still need to be rendered
           - List of nodes to be rendered
             - each with their parent objects (for adding)
        getter for this childs
        should be used in a loader class
    ###

    createRootNodeByData:(data, containerID)->
      rootNode = new RootNode()
      rootNode.set 'containerID', containerID
      rootNode.set 'leftChildren', []
      rootNode.set 'rightChildren', []
      rootNode.set 'mapId', data.id
      rootNode.set 'folded', false

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
          # in this case force isHTML to true and set text 
          data.isHtml = true
          data.nodeText = container.html()

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

      node.set 'persistenceHandler', (new PersistenceHandler())
      node.set 'attributesToPersist', ['folded', 'nodeText', 'isHTML', 'locked']

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