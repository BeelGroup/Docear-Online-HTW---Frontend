define ->
  module = () ->

  class NodeEdit extends Backbone.View

    tagName: 'div',
    className: 'node-edit-container hide-with-overlay' 
    template: Handlebars.templates['NodeEdit']

    events:
      "click .overlay"     : "hideEditView"
      "click .cancel"     : "hideEditView"
      "click .save"     : "saveChanges"
 
    constructor:(@nodeModel)->
      @$node = $('#'+@nodeModel.get('id'))
      super()    

    hideEditView: (event)->
      $(@$el).fadeOut(document.fadeDuration, ->
        $(this).remove()
      )
      
    saveChanges: (event)->
      @nodeModel.set 'nodeText', $(@$el.html).find('textarea.node-content').val()
      @hideEditView()
      
    renderAndAppendTo:($element)->
      $element.append(@render().el)
      
      obj = $(@render().el)
      $(obj).find('.overlay:first').animate({
        opacity: 0.4
      }, document.fadeDuration)
      
      editContainer = $(obj).find('.edit-container:first')
      editContainer.animate({
        opacity: 1.0,
        left: '-='+(editContainer.outerWidth() - @$node.outerWidth()) / 2,
        top: '-='+(editContainer.outerHeight() - @$node.outerHeight()) / 2
      }, document.fadeDuration)
      @
      

    render:->
      @$el.html @template {}
      ec = $(@$el.html).find('.edit-container:first')
      ec.offset(@$node.offset())
      $(ec).find('.node-id:first').val(@$node.attr('id'))
      $textarea = $(ec).find('textarea.node-content')
      $textarea.val(@$node.children('.inner-node').children('.content').html())
      $textarea.select()
      @

  module.exports = NodeEdit