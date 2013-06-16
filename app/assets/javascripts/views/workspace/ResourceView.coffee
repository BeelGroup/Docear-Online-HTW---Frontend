define ['logger'], (logger) ->
  module = () ->

  class ResourceView extends Backbone.View

    constructor:(@model, @projectView, @parentView)->
      super()
      @model.resources.bind "add", @add , @   
      @_rendered = false
      
    addBindingsTo:(obj)->
      #obj.find(".jstree-icon:first").on "click", @updateChilds

      # not very efficient, but currently the best solution
      $('#workspace-tree').bind("open_node.jstree", (event, data)=>
          # if opened node id equals my id
          if @path == $(data.args[0][0]).attr('id')
            @updateChilds()
      )

    test:->
      console.log 'test'

    updateChilds:=>
      for resourceView in @resourceViews
        resourceView.model.update()

    initialize : ()->
      @resourceViews = []
      @model.resources.each (resource)=>
        @resourceViews.push(new ResourceView(resource, @projectView, @$el))
      
    add: (model)->
      resourceView = new ResourceView(model, @projectView, @$el)
      @resourceViews.push(resourceView)
      
      if @_rendered
        resourceView.render()
    
    render:()->
      @path = @model.get 'path'
      # root folde was already rendered with projekt template
      if @path isnt "/"
        # extract parentpath in order to find parent dom node via id
        lastSlashIndex = @path.lastIndexOf '/'

        if lastSlashIndex isnt 0
          parentPath = @path.substr(0, lastSlashIndex)
        else
          parentPath = "/"

        cleanParentPath = parentPath.replace new RegExp("/", "g"), "\\/" 
        $parent = $("#"+@projectView.getId()).find "#" + cleanParentPath

        classes = 'resource '
        if @model.get 'dir'
          classes += 'folder '
        else
          classes += 'file '

        thisState = 'leaf'
        if @model.get 'dir'
          thisState = 'closed'

        newNode = { attr: {class: classes, id: @path}, state: thisState, data: @model.get('filename') }
        obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)
        @addBindingsTo(obj, @path.replace new RegExp("/", "g"))

      @_rendered = true
 
      for resourceView in @resourceViews
        resourceView.render()

      @


  module.exports = ResourceView