define ['models/Node', 'views/SyncedView', 'views/NodeEditView', 'views/NodeControlsView'], (nodeModel, SyncedView, NodeEditView, NodeControlsView) ->
  module = ->
  
  class AbstractNodeView extends SyncedView

    tagName: 'div'
    className: 'node' 
    subViews: {}
    horizontalSpacer: 20
    verticalSpacer: 10

    fieldMap:
      '#nodeText': "nodeText"
      '.node' :
        field: 'Pos'
        toModel: 'PosToModel'
        toForm: 'PosToForm'

    # define events -> here u can pass informations to the model
    events: 
      'click': (event)-> @selectNode(event)
      'click .changeable': 'lockModel'
      'click .action-show': 'printModel'
      'click .action-change': 'modificateModel'
      #'click .action-fold': -> @foldModel()
      
    constructor: (@model) ->
      super()
      @model.bind "change:locked",@changeLockStatus , @   
      @model.bind "change:selected",@changeSelectStatus , @   
      @model.bind "change:folded",@changeFoldedStatus , @
      @model.bind "change:nodeText",@changeNodeText , @
      
    selectNode:()->
      console.log 'test'

    PosToModel: ->
      # TODO: Event will not be called on change
      @model.set 'xPos', @$el.css 'left'
      @model.set 'yPos', @$el.css 'top'

    PosToForm: ->
      x = (@model.get 'Pos').x
      y = (@model.get 'Pos').y
      node = $('#'+@model.get 'id')

      node.css
        'left'    : x + 'px'
        'top'     : y + 'px'

    getElement:()->
      $('#'+@model.get 'id')

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
      $('#'+@model.id).toggleClass('selected')
      
    changeFoldedStatus: ->
      $node = $("#"+@model.id)
      $children = $($node).children('.children')
      isVisible = $children.is(':visible')
      
      $fold = $node.children('.inner-node').children('.action-fold')
      $fold.toggleClass 'icon-minus-sign'
      $fold.toggleClass 'icon-plus-sign'
      
      childrenHeight = $children.outerHeight()
      nodeHeight = $node.outerHeight()
      
      if $children.children('.node').size() > 0
        diff = childrenHeight - nodeHeight
        if isVisible
          if childrenHeight > nodeHeight
            @resizeTree $node, diff
          $children.fadeOut(document.fadeDuration)
        else
          if childrenHeight > nodeHeight
            @resizeTree $node, -diff
          $children.fadeIn(document.fadeDuration)
        
    changeNodeText: ->
      $node = $("#"+@model.id)
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
        @resizeTree $node, -diff
      
      $childrenContainer.animate {
        left: '+='+diffWidth
        top: '+='+(postHeight-preHeight)/2
      },  document.fadeDuration

      $node.animate {
        top: '-='+(postHeight-preHeight)/2
      }, document.fadeDuration
    

    # [Debugging] 
    printModel: ->      
      ##console.log @model.toJSON()

      
    foldModel: ->
      $('#'+@model.id).toggleClass('selected')
      isVisible = $('#'+@model.id).children('.children').is(':visible')
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


    #TODO: add translate

    scale:(amount)->
      
      #possibilities = document.body.style

      #if($.inArray('WebkitTransform', possibilities) or 
      #   $.inArray('MozTransform', inpossibilities) or 
      #   $.inArray('OTransform', possibilities) or 
      #   $.inArray('transform', possibilities))

      #  @getElement().css
      #    '-moz-transform'    : "scale(#{amount})"  #/* Firefox */
      #    '-webkit-transform' : "scale(#{amount})"  #/* Safari and Chrome */
      #    '-ms-transform'     : "scale(#{amount})"  #/* IE 9 */
      #    '-o-transform'      : "scale(#{amount})"  #/* Opera */  

      

      @getElement().zoomTo({targetsize:amount*(@$el.outerWidth()/@$el.parent().width()), duration:600, root: @getElement().parent()});
      


    render: ->
      @$el.html @template @getRenderData()
      # render the subviews
      @$el.attr('id', @model.get 'id')
      @$el.attr('folded', @model.get 'folded')
      for viewId, view of @subViews
        html = view.render().el
        $(html).appendTo(@el)
      # extend the ready rendered htlm element
      @afterRender()
      @

      
    resizeTree: ($node, height, currentZoom = -1)->
      if currentZoom == -1
        currentZoom = getCurrentZoomAmount($node.closest('.node.root'))
      
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
          
        setTimeout(->
          $.each($parentsChildren.children('.node'), (index, $node)->
            updateTree $node, currentZoom
          )
        , document.fadeDuration)
        
        @resizeTree $parent, height, currentZoom
        
        
    
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
          
        $($fold).click (event)->
          currentNodeId = $(this).closest('.node').attr('id')
          #isVisible = $('#'+currentNodeId).children('.children').is(':visible')
          #model.findById(currentNodeId).set 'folded', isVisible
          model.findById(currentNodeId).fold()

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
          
        $($innerNode).click (event)->
          $currentNode = $(this).closest('.node')
          currentNodeId = $($currentNode).attr('id')
          currentNode = model.findById(currentNodeId)
          currentNode.set 'selected', true
          
      
      

  module.exports = AbstractNodeView