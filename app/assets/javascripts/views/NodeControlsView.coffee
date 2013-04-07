define ->
  module = () ->

  class NodeControls extends Backbone.View

    tagName: 'div',
    className: 'controls' 
    template: Handlebars.templates['innerNodeControls']

    events:
      "click .action-edit"     : "actionEdit"
      "click .action-new-node" : "actionNewNode"
      "click .action-share"    : "actionShare"
 
    constructor:(@nodeModel, @$node)->
      super()    

    actionEdit: (event)->
      console.log "edit @ "+@nodeView.model.get 'id'
      # call functions via @nodeView
      ###
      $edit = $($controls).children('.action-edit')
      $($edit).click (event)->
        $node = $(this).closest('.node')
        node = model.findById($node.attr('id'))
        $mindmapCanvas = $(this).closest('.mindmap-canvas')
        nodeEditView = new NodeEditView(node)
        nodeEditView.renderAndAppendTo($mindmapCanvas)
      ###
      
    actionNewNode: (event)->
      console.log "newNode @ "+@nodeView.model.get 'id'
      # call functions via @nodeView
      ###
        $newNode = $($controls).children('.action-new-node')
        $($newNode).click (event)->
          currentNodeId = $(this).closest('.node').attr('id')
          # dummy
          model.findById(currentNodeId).createAndAddChild()
      ###
    
    actionShare: (event)->
      console.log "share @ "+@nodeView.model.get 'id'
      # call functions via @nodeView
    
    renderAndAppendToNode:(@nodeView)->
      $element = @nodeView.$el.children('.inner-node')
      $element.append(@render().el)
      width = (@nodeView.getElement().outerWidth()/2 - @$el.outerWidth())#@$el.outerWidth()/2)
      @$el.css('left', width+"px")
      @
      

    render:->
      attrs = {
          editable: ($.inArray('NODE_CONTROLS', document.features) > -1)
          editableText: ($.inArray('EDIT_NODE_TEXT', document.features) > -1)
          foldable: ($.inArray('FOLD_NODE', document.features) > -1)
          isRoot: (@nodeModel.constructor.name == 'RootNode')
      }
      @$el.html @template attrs
      @

  module.exports = NodeControls