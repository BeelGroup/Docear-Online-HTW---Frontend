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
      @$el.html @template
      $(@$el).children('form').attr('action', jsRoutes.controllers.ProjectController.putFile(@projectId, @path).url)
      @
    
    appendAndRender:($elem)->
      $dialog = $(@render().el)
      $elem.append($dialog)
      $dialog.dialog({
          autoOpen: true,
          width: 500,
          height: 300,
          modal: true
        })
        
      $('#fileupload').fileupload({
          url: jsRoutes.controllers.ProjectController.putFile(@projectId, @path, false, -1).url
          type: "PUT"
          dataType: 'json'
          done: (e, data)->
            console.log(data)
      })
      ###
      
      uploadButton = $('<button/>').addClass('btn').text('Upload').on('click', ()->
        $this = $(this)
        data = $this.data()
        $this.off('click').text('Abort').on('click', ()=>
          $this.remove()
          data.abort()
          data.submit().always(()=>
            $this.remove();
          )
        )
      )
      
      $('#fileupload').fileupload({
        url: url,
        type: "PUT",
        dataType: 'json',
        autoUpload: false,
        multipart: false
        #acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
        maxFileSize: 5000000, # 5 MB
        disableImageResize: true,
        previewMaxWidth: 100,
        previewMaxHeight: 100,
        previewCrop: true
      }).on('fileuploadadd', (e, data)->
        console.log "fileuploadadd"
        data.context = $('#upload-files')
        $.each(data.files, (index, file)->
          node = $('<p/>').append($('<span/>').text(file.name))
          node.append('<br>').append(uploadButton.clone(true).data(data))
          node.appendTo(data.context)
        )
      ).on('fileuploadprocessalways', (e, data)->
        console.log "fileuploadprocessalways"
        index = data.index
        file = data.files[index]
        node = $(data.context.children()[index])
        if file.preview
          node.prepend('<br>').prepend(file.preview)
        
        if file.error
          node.append('<br>').append(file.error)
          
        if (index + 1) is data.files.length
          data.context.find('button').text('Upload').prop('disabled', !!data.files.error)

      ).on('fileuploadprogressall', (e, data)->
        console.log "fileuploadprogressall"
        progress = parseInt(data.loaded / data.total * 100, 10)
        $('#progress .bar').css(
          'width',
          progress + '%'
        )
      ).on('fileuploaddone', (e, data)->
        console.log "fileuploaddone"
        $.each(data.result.files, (index, file)->
          link = $('<a>').attr('target', '_blank').prop('href', file.url)
          $(data.context.children()[index]).wrap(link)
        )
      ).on('fileuploadfail', (e, data)->
        console.log "fileuploadfail"
        $.each(data.result.files, (index, file)->
          error = $('<span/>').text(file.error)
          $(data.context.children()[index]).append('<br>').append(error)
        )
      )
      
      ###
  module.exports = Upload
