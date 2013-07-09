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
      "keydown" : "checkBoundariesOfInputContainer"
 
    constructor:(@nodeModel, @nodeView)->
      @$node = $('#'+@nodeModel.get('id'))
      super()    

    checkBoundariesOfInputContainer:(cancelAnimations = true)->
      $editorWindow = @$el.find(".node-editor:first")
      $toolbar = @$el.find(".editor-toolbar:first")

      buffer = 20

      parentSize = 
        width: @$el.width()
        height: @$el.height()

      $editorWindow.position()

      currentWidth = if $toolbar.outerWidth() > $editorWindow.outerWidth() then $toolbar.outerWidth() else $editorWindow.outerWidth()
      maxRightOuterBound = $editorWindow.position().left + currentWidth
      maxLowerOuterBound = $("#mindmap-viewport").position().top + $("#mindmap-viewport").outerHeight()
      maxUpperOuterBound = $("#mindmap-viewport").position().top

      if ($editorWindow.position().left - buffer) < 0   
        @diffX = $editorWindow.position().left - buffer
      else  
        checkDiffX = maxRightOuterBound - @$el.width() + buffer
        @diffX = if checkDiffX > 0 then checkDiffX else 0

      if ($editorWindow.position().top - buffer - $toolbar.outerHeight())  < maxUpperOuterBound   
        @diffY = $editorWindow.position().top + (- maxUpperOuterBound - buffer - $toolbar.outerHeight())
      else  
        checkDiffY = ($editorWindow.outerHeight() + $editorWindow.position().top + buffer) - maxLowerOuterBound
        @diffY = if checkDiffY > 0 then checkDiffY else 0

      if cancelAnimations
        @$el.children().stop()

      @$el.children().animate({
          'left' : "-="+ @diffX
          'top' : "-="+ @diffY
        }, 400)



    destroy:->
      # http://stackoverflow.com/questions/6569704/destroy-or-remove-a-view-in-backbone-js
      this.undelegateEvents()
      this.$el.removeData().unbind()
      this.remove()
      Backbone.View.prototype.remove.call(this)
      @destroyed = true
      
      
    
    hide: (event)->

      $('.editor-toolbar').fadeOut document.fadeDuration

      @$el.children().animate({
        'left' : "+="+ @diffX
        'top' : "+="+ @diffY
      }, document.fadeDuration*2, =>

        @$node.children('.inner-node').css
          opacity: 1.0

        $(@$el).css
          opacity: 0.0

        $(@).remove()
        $('.editor-toolbar a').unbind().addClass('disabled')
        
        @destroy()        
      )

      
    hideAndSave: (event)->
      @saveChanges event
      @hide event
      
    saveChanges: (event)->
      $(@$el).find('.node-editor').cleanHtml()
      
      newNodeText = $(@$el).find('.node-editor:first').html()
      if newNodeText isnt @nodeModel.get('nodeText')
        @nodeModel.set 'isHtml', true
        @nodeModel.set 'nodeText', $(@$el).find('.node-editor:first').html()
        
        @nodeModel.save(unlock = true)
      else
        @nodeModel.get('persistenceHandler').unlock(@nodeModel)
        
      
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
      
      #toolbarX = offset.left
      #toolbarY = offset.top+($editContainer.outerHeight())
      #$toolbarIndoc.offset({left: toolbarX, top: toolbarY})
      $toolbarIndoc.draggable({ handle: ".handle" });
      if $.browser.msie and $.browser.version < 9
        $toolbarIndoc.remove()

      @positionToolbarOnTop()
      @scaleLikeRoot($editContainer)
      
      $viewPort = @$node.closest('.mindmap-viewport') 
      @$node.children('.inner-node').animate({
        opacity: 0.0
      }, 0)

      @

    positionToolbarOnTop:->
      $editorWindow = @$el.find(".node-editor:first")
      $toolbar = @$el.find(".editor-toolbar:first")

      buffer = 20

      pos = $editorWindow.position()
      pos.top = pos.top - buffer -  $toolbar.outerHeight()

      $toolbar.css pos

    render:->
      @updateLock()
      @$el.html @template {simpleTooltip: ($.inArray('SIMPLE_TOOLTIP', document.features) > -1)}
      @
      
    updateLock:->
      if $(@$el).hasClass('close-and-destroy')
        @hide()
      else if not @destroyed
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
          me = @
          $($elem).animate(
            'scale' : scaleAmount
            'top' : "-="+deltaTop
            'left' : "-="+deltaLeft
          , 0).animate 
                'scale' : if document.currentZoom < 1 then 1 else document.currentZoom
              , 500, "swing", =>
                @checkBoundariesOfInputContainer()
        else
          fallback = true
  
        # ultra fallback
        if fallback
          scaleDiff = 0
          if lastScaleAmount != scaleAmount
            if scaleAmount > lastScaleAmount then scaleDiff = 25 else scaleDiff = -25
            $($elem).effect 'scale', {percent: 100 + scaleDiff, origin: ['middle','center']}, 1, => @refreshDom()

      else
        @checkBoundariesOfInputContainer()

  module.exports = NodeEdit