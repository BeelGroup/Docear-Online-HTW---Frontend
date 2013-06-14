define ['logger', 'collections/workspace/Resources'], (logger, Resources)->
  module = () ->

  class Resource extends Backbone.Model 

    constructor: (@project, data, @isRoot)->
      super()
      if not isRoot
        @fillFromData data
      else
        @set 'path', data
        @set 'id', data
        @set 'filename', data.substring(data.lastIndexOf('/')+1);

      
    initialize : ()->
      @resources = new Resources()
      
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
            resource = new Resource(@project, resourceData)
            @resources.add(resource)
            #if !($.inArray('WORKSPACE_LAZY_LOADING', document.features) > -1)
            resource.update()
      
      
    update: ()->
      me = @
      params = {
        url: jsRoutes.controllers.ProjectController.metadata(@project.id, encodeURI(@get('path'))).url
        type: 'GET'
        cache: false
        success: (data)->
          me.fillFromData(data)
          document.log "files data received"
        dataType: 'json' 
      }
      $.ajax(params)
  module.exports = Resource