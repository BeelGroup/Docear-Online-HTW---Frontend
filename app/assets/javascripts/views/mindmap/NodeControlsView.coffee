define ['views/mindmap/NodeEditView'], (NodeEditView) ->
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
      "click .action-trash" : "actionDelete"
 
    constructor:(@nodeModel, @nodeView, @$node)->
      super()    

    actionEdit: (event)->
      node = @nodeView.model
      $node = @nodeView.$el
      $node.css('')
      # give signal that node is already locked by a user
      if node.get 'locked'
        $node.switchClass('', 'locked-warning', document.fadeDuration, ->
          $node.switchClass('locked-warning', '', document.fadeDuration)
        )
      else
        node.set 'selected', true
        
        $node = @nodeView.$el
  
        if $( "#ribbons li.tab.active a.ribbon-edit" ).size() <= 0 
          $( "#ribbons li.tab a.ribbon-edit" ).click()
        
        $mindmapCanvas = $($node).closest('#mindmap-container')
        nodeEditView = new NodeEditView(node, @nodeView)
        nodeEditView.renderAndAppendTo($mindmapCanvas)

    actionDelete:(event)->
      @nodeView.model.get('persistenceHandler').deleteNode @nodeView.model

      
    actionNewNode: (event)->
      model = @nodeView.model
      if model.get('folded')
        model.set 'folded', false
      if model.isRoot()
        if $(event.currentTarget).hasClass('left')
          if @nodeView.$el.children('.leftChildren:visible').length is 0
            @nodeView.changeFoldedStatus('left')
          
          model.createAndAddChild('Left')
          document.log "newNode added to root - Left"
        else
          if @nodeView.$el.children('.rightChildren:visible').length is 0
            @nodeView.changeFoldedStatus('right')
          
          model.createAndAddChild('Right')
          document.log "newNode added to root - Right"
      else
        document.log "newNode added to @ "+@nodeView.model.get 'id'
        model.createAndAddChild()
    
    actionShare: (event)->
      document.log "share @ "+@nodeView.model.get 'id'
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
          addable: ($.inArray('ADD_NODE', document.features) > -1)
          foldable: ($.inArray('FOLD_NODE', document.features) > -1)
          lockable: ($.inArray('LOCK_NODE', document.features) > -1)
          movable: ($.inArray('MOVE_NODE', document.features) > -1)
          deletable: ($.inArray('DELETE_NODE', document.features) > -1)
          isRoot: (@nodeModel.typeName == 'rootModel')
      }
      @$el.html @template attrs
      @

  module.exports = NodeControls