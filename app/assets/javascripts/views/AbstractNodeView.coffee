define ['models/Node', 'views/SyncedView', 'views/NodeEditView', 'views/NodeControlsView', 'views/ConnectionView'], (nodeModel, SyncedView, NodeEditView, NodeControlsView, ConnectionView) ->
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
      @model.bind "change:folded",@changeFoldedStatus , @
      @model.bind "change:nodeText",@changeNodeText , @
      @addEvents()


    addEvents:()->
      @$el.click (event)=> 
        @handleClick(event)
        event.stopPropagation()
        false
        

    handleClick:(event)->

      if @isInnerNode $(event.target)
        @selectNode() 
      else if not document.wasDragged  
        @selectNone()

      if $(event.target).hasClass 'action-fold-all'
        console.log 'fold DOM target: '
        console.log event.target
        console.log 'fold id: '+@model.get 'id'
        console.log 'childs are visible: '+@$el.children('.children').is(':visible')
        @model.set 'folded', @$el.children('.children').is(':visible')

    selectNode:()->
      @model.set 'selected', true

    isInnerNode:($target)->
      if $target.parents().hasClass('inner-node') or $target.hasClass('inner-node') 
        true 
      else 
        false


    selectNone:()->
      console.log 'select none'
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
      console.log 'locked'


    changeLockStatus: ->
      if @model.get 'locked' 
        if (@model.get('lockedBy') != 'me')
          @$('.changeable').attr('disabled', 'disabled')
      else
        @$('.changeable').removeAttr('disabled')
    

    changeSelectStatus: ->
      @$el.toggleClass('selected')
      

    changeFoldedStatus: ->
      $node = @$el
      $children = $($node).children('.children')
      isVisible = $children.is(':visible')
      
      $fold = $node.children('.inner-node').children('.action-fold')
      $fold.toggleClass 'invisible'
      
      childrenHeight = $children.outerHeight()
      nodeHeight = $node.outerHeight()
      
      if $children.children('.node').size() > 0
        diff = childrenHeight - nodeHeight
        if isVisible
          if childrenHeight > nodeHeight
            @resizeTree $node, @model, diff
          $children.fadeOut(document.fadeDuration)
        else
          if childrenHeight > nodeHeight
            @resizeTree $node, @model, -diff
          $children.fadeIn(document.fadeDuration)
        

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
      diffWidth = $node.outerWidth() - preWidth
      if $($node).hasClass('left')
        diffWidth = -diffWidth
      
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
    
     
    foldModel: ->
      @$el.toggleClass('selected')
      isVisible = @$el.children('.children').is(':visible')
      @model.set 'folded', isVisible
      

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

    afterRender: ->
      @$el.append(@model.get 'purehtml')
      controls = new NodeControlsView(@model, @$el)
      controls.renderAndAppendToNode()
      
      if @model.get('parent') != undefined and @model.get('parent') != null
        @connection = new ConnectionView(@model.get('parent'), @model)
        @connection.renderAndAppendToNode(@$el)
        

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


    alignControls: (model, recursive = false)->
      nodes = [model]
      while node = nodes.shift()
        $node = $('#'+node.id)
        if recursive
          nodes = $.merge(nodes, node.get('children').slice()  )
        $innerNode = $($node).children('.inner-node')
        $fold = $($innerNode).children('.action-fold')
        $controls = $($innerNode).children('.controls')
        $($fold).css('top', ($($innerNode).outerHeight()/2 - $($fold).outerHeight()/2)+"px")
        $($controls).css('left', ($($innerNode).outerWidth()-$($controls).outerWidth())/2+"px")
        
        if $($node).children('.children').children('.node').size() == 0
          $($fold).hide()
          

        $newNode = $($controls).children('.action-new-node')
        $($newNode).click (event)->
          currentNodeId = $(this).closest('.node').attr('id')
          # dummy
          model.findById(currentNodeId).createAndAddChild()
        
        $edit = $($controls).children('.action-edit')
        $($edit).click (event)->
          $node = $(this).closest('.node')
          node = model.findById($node.attr('id'))
          $mindmapCanvas = $(this).closest('.mindmap-canvas')
          nodeEditView = new NodeEditView(node)
          nodeEditView.renderAndAppendTo($mindmapCanvas)
          

          
      
      

  module.exports = AbstractNodeView