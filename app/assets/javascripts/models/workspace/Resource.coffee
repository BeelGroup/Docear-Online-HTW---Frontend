define ['logger', 'collections/workspace/Resources'], (logger, Resources)->
  module = () ->

  class Resource extends Backbone.Model 

    constructor: (@project, @path)->
      super()
      @set 'path', @path
      @set 'id', @path
      @set 'filename', @path.substring(@path.lastIndexOf('/')+1);
      
    initialize : ()->
      @resources = new Resources()
      
    fillFromData: (data)->
      @set 'hash', data.hash
      @set 'bytes', data.bytes
      @set 'revision', data.revision
      @set 'dir', data.dir
      @set 'deleted', data.deleted
      
      for resourceData in data.contents
        if @resources.get(resourceData.path) == undefined
          @resources.add(new Resource(@project, resourceData.path))
      
      
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