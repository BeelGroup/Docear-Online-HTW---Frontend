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
      
    add: (resource)->
      resourceView = new ResourceView(resource, @projectView, @$el)
      @resourceViews.push(resourceView)
      
      if @_rendered
        resourceView.render()
    

    render:()->
      path = @model.get 'path'
      # root folde was already rendered with projekt template
      if path isnt "/"
        # extract parentpath in order to find parent dom node via id
        lastSlashIndex = path.lastIndexOf '/'
        parentPath = path.substr(0, lastSlashIndex+1)
        $parent = $("#"+@projectView.getId()).find("#\\"+parentPath)

        classes = 'resource '
        if @model.get 'dir'
          classes += 'folder '

        thisState = 'closed'
        if @model.get 'dir'
          thisState = 'open'

        newNode = { attr: {class: classes, id: @model.get('id')}, state: thisState, data: @model.get('filename') }
        obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)

      @_rendered = true
 
      for resourceView in @resourceViews
        resourceView.render()
      @


  module.exports = ResourceView