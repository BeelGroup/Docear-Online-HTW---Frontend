define ['logger'], (logger) ->
  module = () ->

  class Upload extends Backbone.View
  
    tagName  : 'div'
    className: 'upload'
    id: 'upload-dialog'
    template : Handlebars.templates['Upload']

    constructor:(@projectId, @path, @workspace)->
      super()

    element:-> @$el

    render:()->
      values = {
          'action': jsRoutes.controllers.ProjectController.putFile(@projectId, @path).url
          'method': 'PUT'
          'path': @path
          'projectId': @projectId
          'target': @projectId+"_"+Math.random()
      }
      
      @$el.html @template values

          
      $form = $(@$el).children('form.file-upload')
      
      $form.find('.file-upload-button').click (evt)=>
        projectModel = @workspace.get @projectId
        
        $fileList = $form.find('.selected-filenames')
        for f in @files
          #tempFunc is necessary to make sure file reader nows "filename"
          tempFunc = (f)=>
            filename = escape(f.name)
            filepath = @path+filename
            
            me = @
            resource = projectModel.getResourceByPath(filepath)
            revision = -1
            if !!resource
              revision = resource.get('revision')
            reader = new FileReader()
            reader.onload = (event)=>
              $.ajax({
                url: jsRoutes.controllers.ProjectController.putFile(@projectId, @path+filename, false, revision).url
                type: 'PUT'
                processData: false
                enctype: 'application/text'
                contentType: 'application/text'
                data: event.target.result
                dataType: 'json'
                success: (data)->
                  $("li.uploaded-file input[value*='#{data.path}']").parent().find(".status").text("Done").addClass('alert-success')
              })
            reader.readAsText(f);
          tempFunc(f)
      
      $form.find('.file-to-upload').change (evt)=>
        if evt.target.files.length > 0
          @files = evt.target.files
          fileInfos = []
          
          $fileList = $form.find('.selected-filenames')
          $fileList.empty()
          
          projectModel = @workspace.get @projectId
          for f in @files
            filename = escape(f.name)
            filepath = @path+filename
            
            fileInfos.push({
              "name": escape(f.name)
              "type": f.type
              "size": f.size
              "modified": f.lastModifiedDate.toLocaleDateString()
            })
            
            resource = projectModel.getResourceByPath(filepath)
            status = "new"
            if !!resource
              status = "exists (overwrite)"
              
            values = {
              'filepath': filepath
              'name': f.name
              'size': Math.round(f.size/1000)
              'status': status
            }
  
            $fileListItem = Handlebars.templates['UploadItem'] values
            $fileList.append($fileListItem)

      
      $form.submit =>
        if (window.File && window.FileReader && window.FileList && window.Blob)
          #ok
        else
          alert('The File APIs are not fully supported in this browser.');
        
        false
      @
    
    appendAndRender:($elem)->
      $dialog = $(@render().el)
      $elem.append($dialog)
      $dialog.dialog({
          autoOpen: true,
          width: 500,
          height: 300,
          modal: true
          title: "File upload"
        })
        
  module.exports = Upload
