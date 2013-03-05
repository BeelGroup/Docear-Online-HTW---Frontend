define ->
  module = () ->

  class NodeControls extends Backbone.View

    tagName: 'div',
    className: 'controls' 
    template: Handlebars.templates['innerNodeControls']

    events:
      "click .action-edit"     : "actionEdit"
      "click .action-new-node"     : "actionNewNode"
      "click .action-share"     : "actionShare"
 
    constructor:(@nodeModel, @$node)->
      super()    

    actionEdit: (event)->
      console.log "edit"
      
    actionNewNode: (event)->
      console.log "newNode"
    
    actionShare: (event)->
      console.log "share"
    
    renderAndAppendToNode:()->
      @$node.find('.node:first').children('.inner-node').append(@render().el)
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