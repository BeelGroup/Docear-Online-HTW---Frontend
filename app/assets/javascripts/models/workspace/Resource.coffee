define ['logger', 'collections/workspace/Resources'], (logger, Resources)->
  module = () ->

  class Resource extends Backbone.Model 

    constructor: (@project, path, @isRoot = false, parent = null)->
      super()
      @updated = false
      @set 'path', path
      @set 'id', path
      @set 'filename', path.substring(path.lastIndexOf('/')+1);
      @set 'parent', parent
      
    initialize : ()->
      @resources = new Resources()
      
    deleteResourceByPath: (path)->
      @resources.remove(path)
      
    fillFromData: (data)->
      @set 'hash', data.hash
      @set 'bytes', data.bytes
      @set 'revision', data.revision
      @set 'dir', data.dir
      @set 'deleted', data.deleted      
      @set 'path', data.path
      @set 'id', data.path
      @set 'filename', data.path.substring(data.path.lastIndexOf('/')+1);
      
      if data.contents isnt undefined
        for resourceData in data.contents
          if @resources.get(resourceData.path) == undefined
            resource = new Resource(@project, resourceData.path, false, @)
            resource.fillFromData resourceData
            
            if @isRoot
              resource.update(@resources)
            else
              @resources.add(resource)


    addResouce:(newChild)->
      @resources.add(newChild)


    update: (resources)=>
      if not @updated
        me = @
        params = {
          url: jsRoutes.controllers.ProjectController.metadata(@project.id, encodeURI(@get('path'))).url
          type: 'GET'
          cache: false
          success: (data)->
            me.updated = true
            me.fillFromData(data)
            if resources != undefined
              resources.add(me)
            document.log "files data for "+(me.get 'filename')+ " received"
          dataType: 'json' 
        }
        $.ajax(params)     
  module.exports = Resource