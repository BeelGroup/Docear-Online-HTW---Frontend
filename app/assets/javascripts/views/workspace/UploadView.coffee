define ['logger'], (logger) ->
  module = () ->

  class Upload extends Backbone.View
  
    tagName  : 'div'
    className: 'upload'
    id: 'upload-dialog'
    template : Handlebars.templates['Upload']

    constructor:(@projectId, @path)->
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
      
      $form.find('.file-to-upload').change (evt)=>
        files = evt.target.files; 
        fileInfos = []
        
        $fileList = $form.find('.selected-filenames');
        
        for f in files
          #tempFunc is necessary to make sure file reader nows "filename"
          tempFunc = (f)=>
            filename = escape(f.name)
            filepath = @path+filename
            fileInfos.push({
              "name": escape(f.name)
              "type": f.type
              "size": f.size
              "modified": f.lastModifiedDate.toLocaleDateString()
            })
            $fileListItem = $("<li class=\"uploaded-file #{filepath}\"> #{escape(f.name)} (#{Math.round(f.size/1000)} kB) <span class=\"status\">...loading</span></li>")
            $fileList.append($fileListItem)
            
            me = @
            reader = new FileReader()
            reader.onload = (event)=>
              $.ajax({
                url: jsRoutes.controllers.ProjectController.putFile(@projectId, @path+filename, false, -1).url
                type: 'PUT'
                processData: false
                enctype: 'application/text'
                contentType: 'application/text'
                data: event.target.result
                dataType: 'json'
                complete: (data)->
                  $fileListItem.find(".status").text("Done").addClass('alert-success')
              })
            reader.readAsText(f);
          tempFunc(f)

      
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
