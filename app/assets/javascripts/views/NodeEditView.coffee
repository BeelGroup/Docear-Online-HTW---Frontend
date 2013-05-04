define ->
  module = () ->

  class NodeEdit extends Backbone.View

    tagName: 'div',
    className: 'node-edit-container hide-with-overlay' 
    template: Handlebars.templates['NodeEdit']

    events:
      "click .overlay"  : "hideEditView"
      "click .cancel"   : "hideEditView"
      "click .save"     : "saveChanges"
 
    constructor:(@nodeModel, @nodeView)->
      @$node = $('#'+@nodeModel.get('id'))
      super()    

    hideEditView: (event)->
      @saveChanges event
      
      $(@$el).fadeOut(document.fadeDuration, ->
        $(this).remove()
      )
      
    saveChanges: (event)->
      @nodeModel.set 'isHTML', true
      @nodeModel.set 'nodeText', $(@$el).find('.node-editor:first').html()
      
    renderAndAppendTo:($element)->
      $element.append(@render().el)
      
      obj = $(@render().el)
      $(obj).find('.overlay:first').animate({
        opacity: 0.4
      }, document.fadeDuration)
      
      $editContainer = $(obj).find('.node-editor:first')
      
      editorId = 'editor-'+Date.now()
      $editContainer.attr('id', editorId)
      $($editContainer).wysiwyg()

      $toolbar = $(obj).find('.editor-toolbar:first')
      $toolbar.attr('data-target', '#'+editorId)
      
      offset = @$node.offset()
      
      $editContainer.html(@$node.children('.inner-node').children('.content').html())
      $editContainer.offset(offset)
      
      $toolbar.offset(offset)
      $toolbar.draggable({ handle: ".handle" });
      $toolbar.animate({
        left: '+='+($editContainer.outerWidth() + 20) #a little distance away from node
        top: '-='+(($toolbar.outerHeight() - $editContainer.outerHeight()) / 2)
      })
      
      @
      

    render:->
      @$el.html @template {}
      ec = $(@$el.html).find('.edit-container:first')
      ec.offset(@$node.offset())
      $(ec).find('.node-id:first').val(@$node.attr('id'))
      @

  module.exports = NodeEdit