define ['views/NodeEditView'], (NodeEditView) ->
  module = () ->

  class NodeControls extends Backbone.View

    tagName: 'div',
    className: 'controls' 
    template: Handlebars.templates['innerNodeControls']

    events:
      "click .action-edit"     : "actionEdit"
      "click .action-new-node" : "actionNewNode"
      "click .action-share"    : "actionShare"
      "click .action-move"    : "actionMove"
 
    constructor:(@nodeModel, @$node)->
      super()    

    actionEdit: (event)->
      node = @nodeView.model
      node.set 'selected', true
      
      $node = @nodeView.$el

      $mindmapCanvas = $($node).closest('body')
      
      nodeEditView = new NodeEditView(node, @nodeView)
      nodeEditView.renderAndAppendTo($mindmapCanvas)
      
    actionNewNode: (event)->
      console.log "newNode @ "+@nodeView.model.get 'id'
      @nodeView.model.createAndAddChild()
    
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
          movable: ($.inArray('MOVE_NODE', document.features) > -1)
          isRoot: (@nodeModel.constructor.name == 'RootNode')
      }
      @$el.html @template attrs
      @

  module.exports = NodeControls