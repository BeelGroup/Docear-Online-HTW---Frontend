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
      # call unlock after document.unlockDelay Sec to give model time to submit changes
      @nodeModel.get('persistenceHandler').unlock(@nodeModel, document.unlockDelay)
      
      @$node.children('.inner-node').animate({
        opacity: 1.0
      }, 0)
      $(@$el).fadeOut(document.fadeDuration, ->
        $(this).remove()
        $('.editor-toolbar a').unbind().addClass('disabled')
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
      
      $mmCanvas = @$node.closest('.mindmap-canvas')
      
      $(obj).find('.edit-overlay:first').animate({
        opacity: 0.0
        top: $mmCanvas.offset().top
        left: $mmCanvas.offset().left
        width: $mmCanvas.outerWidth()
        height: $mmCanvas.outerHeight()
      }, 0)
      
      $editContainer = $(obj).find('.node-editor:first')
      
      editorId = 'editor-'+Date.now()
      $editContainer.attr('id', editorId)

      $toolbar = $('.editor-toolbar')
      $toolbar.find('a.btn').removeClass('disabled')
      $toolbar.attr('data-target', '#'+editorId)
      
      $toolbarIndoc = $(obj).find('.editor-toolbar-indoc')
      
      offset = @$node.offset()
      
      $($editContainer).wysiwyg()
      $editContainer.html(@$node.children('.inner-node').children('.content').html())
      $editContainer.offset(offset)
      
      @selectText(editorId)
      
      toolbarX = offset.left
      toolbarY = offset.top+($editContainer.outerHeight())
      $toolbarIndoc.offset({left: toolbarX, top: toolbarY})
      $toolbarIndoc.draggable({ handle: ".handle" });
      if $.browser.msie and $.browser.version < 9
        $toolbarIndoc.remove()
      
      @scaleLikeRoot($editContainer)
      
      $viewPort = @$node.closest('.mindmap-viewport') 
      @$node.children('.inner-node').animate({
        opacity: 0.0
      }, 0)
      @

    render:->
      @updateLock()
      @$el.html @template {}
      @
      
    updateLock:->
      if not @destroyed
        @nodeModel.get('persistenceHandler').lock(@nodeModel)
        nodeEditView = @
        setTimeout(->
          nodeEditView.updateLock()
        , document.lockRefresh)

    scaleLikeRoot:($elem)-> 
      scaleAmount = @nodeView.rootView.scaleAmount
      if scaleAmount != 1
        lastScaleAmount = @nodeView.rootView.lastScaleAmount    
        currentScale = @nodeView.rootView.currentScale
        possibilities = document.body.style
        fallback = false
  
        deltaTop = ($($elem).outerHeight() - ($($elem).outerHeight() / (1/scaleAmount))) /2
        deltaLeft = ($($elem).outerWidth() - ($($elem).outerWidth() / (1/scaleAmount))) /2
  
        # IE
        if $.browser.msie 
          if $.browser.version > 8
            if scaleAmount > 1
              $($elem).css
                '-ms-transform': "scale(#{scaleAmount})" 
            
            $($elem).animate {
              'top' : "-="+deltaTop
              'left' : "-="+deltaLeft
            }, 0
  
          else if $.browser.version <= 8 
            fallback = true
  
        # Safari, Firefox and Chrome with CSS3 support 
        else if($.inArray('WebkitTransform', possibilities) or 
        $.inArray('MozTransform', inpossibilities) or 
        $.inArray('OTransform', possibilities)) 
          $($elem).animate {
            'scale' : scaleAmount
            'top' : "-="+deltaTop
            'left' : "-="+deltaLeft
          }, 0, ->
            if scaleAmount < 1
              $($elem).animate {
                'scale' : 1
              }, 500
        else
          fallback = true
  
        # ultra fallback
        if fallback
          scaleDiff = 0
          if lastScaleAmount != scaleAmount
            if scaleAmount > lastScaleAmount then scaleDiff = 25 else scaleDiff = -25
            $($elem).effect 'scale', {percent: 100 + scaleDiff, origin: ['middle','center']}, 1, => @refreshDom()

  module.exports = NodeEdit