define ->
  module = () ->

  class NodeEdit extends Backbone.View

    tagName: 'div',
    className: 'node-edit-container hide-with-overlay' 
    template: Handlebars.templates['NodeEdit']
    # needed to stop refreshing lock
    destroyed: false

    events:
      "click .edit-overlay"  : "hideAndSave"
      "click .save"     : "saveChanges"
      "click .cancel"     : "hide"
 
    constructor:(@nodeModel, @nodeView)->
      @$node = $('#'+@nodeModel.get('id'))
      super()    

    destroy:->
      # http://stackoverflow.com/questions/6569704/destroy-or-remove-a-view-in-backbone-js
      this.undelegateEvents()
      this.$el.removeData().unbind()
      this.remove()
      Backbone.View.prototype.remove.call(this)
      @destroyed = true
      
      
    
    hide: (event)->
      @nodeModel.get('persistenceHandler').unlock(@nodeModel)
      
      $(@$el).fadeOut(document.fadeDuration, ->
        $(this).remove()
        $('.editor-toolbar a.btn').addClass('disabled')
      )
      @destroy()
      
    hideAndSave: (event)->
      @saveChanges event
      @hide event
      
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
        selection.removeAllRanges();
        selection.addRange(range);
      
        
    renderAndAppendTo:($element)->
      obj = $(@render().el)
      $element.append(obj)
      
      $(obj).find('.edit-overlay:first').animate({
        opacity: 0.0
      }, document.fadeDuration)
      
      $editContainer = $(obj).find('.node-editor:first')
      
      editorId = 'editor-'+Date.now()
      $editContainer.attr('id', editorId)
      $($editContainer).wysiwyg()

      $toolbar = $(obj).find('.editor-toolbar')
      $('.editor-toolbar a.btn').removeClass('disabled')
      
      $toolbarIndoc = $(obj).find('.editor-toolbar-indoc')
      $toolbar.attr('data-target', '#'+editorId)
        
      offset = @$node.offset()
      
      $editContainer.html(@$node.children('.inner-node').children('.content').html())
      $editContainer.offset(offset)
      
      @selectText(editorId)
      
      if $.browser.msie and $.browser.version < 9
        $toolbarIndoc.remove()
      else
        toolbarX = offset.left
        toolbarY = offset.top+($editContainer.outerHeight())
        $toolbar.offset({left: toolbarX, top: toolbarY})
        $toolbarIndoc.draggable({ handle: ".handle" });
      @
      

    render:->
      @updateLock()
      @$el.html @template {}
      ec = $(@$el.html).find('.edit-container:first')
      ec.offset(@$node.offset())
      $(ec).find('.node-id:first').val(@$node.attr('id'))
      @
      
    updateLock:->
      if not @destroyed
        @nodeModel.get('persistenceHandler').lock(@nodeModel)
        nodeEditView = @
        setTimeout(->
          nodeEditView.updateLock()
        , document.lockRefresh)

  module.exports = NodeEdit