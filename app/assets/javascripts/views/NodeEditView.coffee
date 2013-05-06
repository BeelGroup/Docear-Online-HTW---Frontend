define ->
  module = () ->

  class NodeEdit extends Backbone.View

    tagName: 'div',
    className: 'node-edit-container hide-with-overlay' 
    template: Handlebars.templates['NodeEdit']

    events:
      "click .edit-overlay"  : "hideEditView"
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
      $(@$el).find('.node-editor').cleanHtml()
      
      newNodeText = $(@$el).find('.node-editor:first').html()
      if newNodeText isnt @nodeModel.get('nodeText')
        @nodeModel.set 'isHTML', true
        @nodeModel.set 'nodeText', $(@$el).find('.node-editor:first').html()
      
    # found @ http://stackoverflow.com/questions/985272/jquery-selecting-text-in-an-element-akin-to-highlighting-with-your-mouse/987376#987376
    selectText: (nodeId)->
      doc = document
      text = doc.getElementById(nodeId)
      range = null
      selection = null
      if (doc.body.createTextRange) #ms
        range = doc.body.createTextRange();
        range.moveToElementText(text);
        range.select();
      else if (window.getSelection) #all others
        selection = window.getSelection();    
        range = doc.createRange();
        range.selectNodeContents(text);
        console.log range
        selection.removeAllRanges();
        selection.addRange(range);
      
        
    renderAndAppendTo:($element)->
      obj = $(@render().el)
      $element.append(obj)
      
      $(obj).find('.edit-overlay:first').animate({
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
      
      @selectText(editorId)
      
      if $.browser.msie and $.browser.version < 9
        $toolbar.remove()
      else
        toolbarX = offset.left+($editContainer.outerWidth() + 20)
        toolbarY = offset.top-(($toolbar.outerHeight() - $editContainer.outerHeight()) / 2)
        $toolbar.offset({left: toolbarX, top: toolbarY})
        $toolbar.draggable({ handle: ".handle" });
      @
      

    render:->
      @$el.html @template {}
      ec = $(@$el.html).find('.edit-container:first')
      ec.offset(@$node.offset())
      $(ec).find('.node-id:first').val(@$node.attr('id'))
      @

  module.exports = NodeEdit