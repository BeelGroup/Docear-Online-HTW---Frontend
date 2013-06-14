define ['logger'], (logger) ->
  module = () ->

  class ResourceView extends Backbone.View

    constructor:(@model, @projectView, @parentView)->
      super()
      @model.resources.bind "add", @add , @   
      @_rendered = false

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
      path = @model.get 'path'
      # root folde was already rendered with projekt template
      if path isnt "/"
        # extract parentpath in order to find parent dom node via id
        lastSlashIndex = path.lastIndexOf '/'

        if lastSlashIndex isnt 0
          parentPath = path.substr(0, lastSlashIndex)
        else
          parentPath = "/"

        console.log path
        console.log parentPath

        cleanParentPath = parentPath.replace new RegExp("/", "g"), "\\/" 
        console.log cleanParentPath
        $parent = $("#"+@projectView.getId()).find "#" + cleanParentPath

        classes = 'resource '
        if @model.get 'dir'
          classes += 'folder '

        thisState = 'leaf'
        if @model.get 'dir'
          thisState = 'closed'

        newNode = { attr: {class: classes, id: path}, state: thisState, data: @model.get('filename') }
        obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)

      @_rendered = true
 
      for resourceView in @resourceViews
        resourceView.render()
      @


  module.exports = ResourceView