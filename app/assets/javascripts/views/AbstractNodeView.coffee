define ['models/Node', 'views/SyncedView', 'views/NodeEditView'], (nodeModel, SyncedView, NodeEditView) ->
  module = ->
  
  class AbstractNodeView extends SyncedView

    subViews: {}
    horizontalSpacer: 20
    verticalSpacer: 10
    connection: null


    fieldMap:
      '#nodeText': "nodeText"
      '.node' :
        field: 'Pos'
        toModel: 'PosToModel'
        toForm: 'PosToForm' 

      
    constructor: () ->
      super()
      @model.bind "change:locked",@changeLockStatus , @   
      @model.bind "change:selected",@changeSelectStatus , @   
      @model.bind "change:folded",@updateFoldStatus , @
      @model.bind "change:nodeText",@changeNodeText , @
      @addEvents()


    addEvents:()->
      @$el.click (event)=> 
        @handleClick(event)
        event.stopPropagation()
        false
        

    handleClick:(event)->
      if @isInnerNode $(event.target)
        @selectNode(event) 
      else if not document.wasDragged  
        @selectNone(event)

      if $(event.target).hasClass 'action-fold-all'
        @model.set 'folded', not @model.get 'folded' 

    selectNode:(event)->
      if @model.get 'selected' || $(@$el).hasClass('selected')
        @controls.actionEdit(event)
      else
        @model.set 'selected', true

    isInnerNode:($target)->
      if $target.parents().hasClass('inner-node') or $target.hasClass('inner-node') 
        true 
      else 
        false


    selectNone:()->
      @model.get('rootNodeModel').selectNone()
    

    PosToModel: ->
      # TODO: Event will not be called on change
      @model.set 'xPos', @$el.css 'left'
      @model.set 'yPos', @$el.css 'top'


    PosToForm: ->
      x = (@model.get 'Pos').x
      y = (@model.get 'Pos').y
      node = @$el #$('#'+@model.get 'id')

      node.css
        'left'    : x + 'px'
        'top'     : y + 'px'


    getElement:()->
      @$el #$('#'+@model.get 'id')


    lockModel: ->
      # will be replaced by username
      @model.lock 'me'
      document.log 'locked'


    changeLockStatus: ->
      if @model.get 'locked' 
        if (@model.get('lockedBy') != 'me')
          @$('.changeable').attr('disabled', 'disabled')
      else
        @$('.changeable').removeAttr('disabled')
    

    changeSelectStatus: ->
      @$el.toggleClass('selected')
      

    initialFoldStatus:()-> 
      shouldBeVisible = !@model.get('folded')
      @updateFS(shouldBeVisible, true)
     
    updateFoldStatus:()->
      shouldBeVisible = !@model.get('folded')
      domVisible = @$el.children('.children').is ':visible'
      @updateFS(shouldBeVisible, domVisible)

    updateFS:(shouldBeVisible, domVisible)->
      if shouldBeVisible isnt domVisible
        
        $children = @$el.children('.children')
        $myself = @$el.children('.inner-node')

        $nodesToFold = @$el.children('.inner-node').children('.action-fold')
        $nodesToFold.toggleClass 'invisible'

        childrenHeight = $children.outerHeight()
        nodeHeight = $myself.outerHeight()
        
        if $children.children('.node').size() > 0
          diff = childrenHeight - nodeHeight
          if(@model.get 'folded')
            if childrenHeight > nodeHeight
              @resizeTree @$el, @model, diff
          else
            if childrenHeight > nodeHeight
              @resizeTree @$el, @model, -diff
        
        $children.fadeToggle(document.fadeDuration)


    changeNodeText: ->
      $node = @$el
      $childrenContainer = $node.children('.children:first')
      
      preWidth = $node.outerWidth()
      preHeight = $node.outerHeight()
      childrenHeight = $childrenContainer.outerHeight()
      parentIsHeigher = preHeight > childrenHeight

      $contentContainer = $node.children('.inner-node').children('.content')
      if @model.get 'isHTML'
        $contentContainer.addClass('isHTML')
        $contentContainer.html(@model.get 'nodeText')
      else
        $contentContainer.text(@model.get 'nodeText')
      
      
      postHeight = $node.outerHeight()
      diffWidth = 0
      if $($node).hasClass('right')
        diffWidth = ($node.outerWidth() - preWidth)
      
      diff = 0
      if postHeight > childrenHeight
        if preHeight > childrenHeight
          diff = postHeight - preHeight
        else
          diff = postHeight - childrenHeight
      else if postHeight < childrenHeight
        if preHeight > childrenHeight
          diff = childrenHeight - preHeight
      if diff != 0
        @resizeTree $node, @model, -diff
      
      $childrenContainer.animate {
        left: '+='+diffWidth
        top: '+='+(postHeight-preHeight)/2
      },  document.fadeDuration

      $node.animate {
        top: '-='+(postHeight-preHeight)/2
      }, document.fadeDuration

      model = @model
      
      # timeout "document.fadeDuration" might not be enough, since other animations maybe need a few millis more
      setTimeout(->
        model.getRoot().updateAllConnections()
      , document.fadeDuration)
      
      
     
    foldModel: ->
      @$el.toggleClass('selected')
      @model.set 'folded', not @model.get 'folded'
      

    # [Debugging] model modification
    modificateModel: -> 
      @model.set 'nodeText', Math.random()   
      @model.set 'xPos', (@model.get 'xPos') + 20   
      @model.set 'yPos', (@model.get 'yPos') + 20
      if(@model.get 'locked')   
        @model.unlock()
      else
        @model.lock 'Mr. P'
     

    subView: (view, autoRender = false) ->
      # if model is set, use its id OR a unique random id
      viewId = view.model?.id or String(Math.random() * new Date().getTime())
      # add view to subviews
      @subViews[viewId] = view
      view


    getRenderData: ->
    # if the model is already set, parse it to json
      if @model?
        @model.toJSON()
    # otherwise pass an empty JSON
      else
        {}


    alignButtons:->
      $fold = @$el.children('.inner-node').children('.action-fold')

      height = (@$el.outerHeight()/2 - $fold.outerHeight()/2)
      $fold.css('top', height+"px")
        

    scale:(amount)->
      @getElement().zoomTo({targetsize:amount*(@$el.outerWidth()/@$el.parent().width()), duration:600, root: @getElement().parent()});
      
    
    resizeTree: ($node, nodeModel, height)->
      $parent = $node.parent().closest('.node')
      $parentsChildren = $node.closest('.children')
      if($($parentsChildren).children('.node').size() > 1)
        $parentsChildren.animate({
          top: '+='+(height/2)
        },
        duration: document.fadeDuration)
        $parentsChildren.css('height', $parentsChildren.outerHeight()-height)
      
        $node.animate({
          top: '-='+(height/2)
        }, 
        duration: document.fadeDuration)
      
        $nextBrother = $($node).next('.node')
        while $nextBrother.size() > 0
          $($nextBrother).animate({
            top: '-='+(height)
          }, document.fadeDuration)
          $nextBrother = $($nextBrother).next('.node')
          
        # to make it visible inside the timeout
        parent = nodeModel.get 'parent'
        setTimeout(->
          for child in parent.get('children')
            child.updateConnection()
        , document.fadeDuration)
        
        @resizeTree $parent, parent, height

    
  module.exports = AbstractNodeView