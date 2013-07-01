define ['logger'], (logger) ->
  module = () ->

  class ResourceView extends Backbone.View

    constructor:(@model, @projectView, @parentView)->
      super()
      @model.resources.bind "add", @add , @   
      @model.resources.bind "remove", @remove , @
      @_rendered = false
      
    addBindingsTo:(obj)->
      #obj.find(".jstree-icon:first").on "click", @updateChilds
      # not very efficient, but currently the best solution
      $('#workspace-tree').bind("open_node.jstree", (event, data)=>
          # if opened node id equals my id
          if @path == $(data.args[0][0]).attr('id')
            @updateChilds()
      )

    updateChilds:=>
      for resourceView in @resourceViews
        resourceView.model.update()

    initialize:()->
      @resourceViews = []
      @model.resources.each (resource)=>
        @resourceViews.push(new ResourceView(resource, @projectView, @$el))
      
    getCleanedPath:(dirtyPath)->
      cleanPath = dirtyPath.replace new RegExp("/", "g"), "\\/"
      cleanPath = cleanPath.replace new RegExp("\\.", "g"), "\\."

    add:(model)->
      resourceView = new ResourceView(model, @projectView, @$el)
      @resourceViews.push(resourceView)
      
      cleanPath = @getCleanedPath(model.get('path'))
      $objToDelete = $("#"+cleanPath)

      if @_rendered
        # if class "delete-me-on-update" is set, the folder was created on this client
        if $objToDelete.hasClass("delete-me-on-update")
          $('#workspace-tree').jstree("delete_node", $objToDelete)
          $('#workspace-tree').jstree('open_node', resourceView.render())
        else
          resourceView.render()

    remove:(resourceToDelete)->
      document.log "Trying to remove node "+resourceToDelete.id

      cleanPath = @getCleanedPath resourceToDelete.id
      $objToDelete = $("#"+cleanPath)
      $('#workspace-tree').jstree("delete_node", $objToDelete)
    
    render:()->
      @path = @model.get 'path'
      iconClass = ''
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
          iconClass = 'icon-file'

        if @path.indexOf(".mm") isnt -1
          classes += 'mindmap-file '
          iconClass = 'icon-pencil'

        thisState = 'leaf'
        if @model.get 'dir'
          thisState = 'closed'

        newNode = { attr: {class: classes, id: @path}, state: thisState, data: @model.get('filename') }
        obj = $('#workspace-tree').jstree("create_node", $parent, 'inside', newNode, false, false)
        @addBindingsTo(obj, @path.replace new RegExp("/", "g"))

        # add icon class to resource
        obj.find('a .jstree-icon').addClass(iconClass)
        # $('#workspace-tree').find('li.file > a .jstree-icon').addClass('icon-star')
        #console.log obj.find('.jstree-icon:last').addClass('icon-user icon-white')

      @_rendered = true
 
      for resourceView in @resourceViews
        resourceView.render()

      obj


  module.exports = ResourceView