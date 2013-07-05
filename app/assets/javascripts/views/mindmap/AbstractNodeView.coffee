define ['logger', 'models/mindmap/Node', 'views/SyncedView', 'views/mindmap/NodeEditView'], (logger, nodeModel, SyncedView, NodeEditView) ->
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
      @model.bind "change:isHtml",@changeNodeText , @
      @model.bind "change:lastAddedChild",@changeChildren , @
      @model.on 'deleteNode', @destroy, @

      @addEvents()


    addEvents:()->
      @$el.click (event)=> 
        @handleClick(event)
        event.stopPropagation()
        false
        

    handleClick:(event)->
      type = @selectionType $(event.target)
      if type is 'select'
        @selectNode(event)
      else if not(document.wasDragged or $(event.target).parent().hasClass('controls')) and type is 'deselect'
        @selectNone(event)

      if $(event.target).hasClass 'action-fold-all'
        @model.set 'folded', not @model.get 'folded' 

    selectNode:(event)->
      if $.inArray('EDIT_NODE_TEXT', document.features) > -1 and ( @model.get 'selected' || $(@$el).hasClass('selected') ) and !@model.get('rootNodeModel').get('isReadonly')
        #$("##{(@model.get 'rootNodeModel').get 'id'}").trigger 'newSelectedNode', @
        @controls.actionEdit(event)
      @model.set 'selected', true

    selectionType:($target)->
      $parent = $target
      # range 1...20 is needed, if node-content is HTML and its dom is nested up to 20 levels deep
      for i in [1...20]
        if $parent.hasClass('inner-node')
          return 'select'
        else if $parent.hasClass('controls')
          return 'deselect'
        else if $parent.hasClass('action-fold') 
          return 'noChange'
        $parent = $parent.parent()
      'deselect'  


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


    changeLockStatus: ->
      $lockContainer = @$el.children('.inner-node').children('.lock')
      if @model.get 'locked' 
        if (@model.get('lockedBy') != null)
          $lockContainer.children('.lock-username').text(@model.get('lockedBy'))
          $lockContainer.fadeIn('fast')
      else
        $lockContainer.children('.lock-username').text('')
        $lockContainer.fadeOut('fast')
    

    changeSelectStatus: ->
      if @model.get 'selected'
        @$el.addClass('selected')
      else
        @$el.removeClass('selected')
      

    initialFoldStatus:()-> 
      shouldBeVisible = !@model.get('folded')
      @privateUpdateFoldStatus(shouldBeVisible, true)

     
    updateFoldStatus:()->
      @$el.attr('folded', @model.get 'folded')
      if @renderOnExpand

        # visible for layouting
        @$el.find('.children:first').toggle()
        (@model.get 'rootNodeModel').trigger 'refreshDomConnectionsAndBoundaries'

        # childs will layouted (not rendered) the first and only time here.
        @renderOnExpand = false
        @switchFoldButtons()

      else
        shouldBeVisible = !@model.get('folded')
        domVisible = @$el.children('.children').is ':visible'
        if shouldBeVisible isnt domVisible
          @privateUpdateFoldStatus()
          (@model.get 'rootNodeModel').trigger 'updateMinimap'

      

    privateUpdateFoldStatus:()->
        $children = @$el.children('.children')
        $myself = @$el.children('.inner-node')

        @switchFoldButtons()

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
        
        # toggle visibilety of childs
        $children.fadeToggle(document.fadeDuration)


    switchFoldButtons:->
      $nodesToFold = @$el.children('.inner-node').children('.action-fold.node-control')
      # toggle +/- button
      $nodesToFold.toggleClass 'invisible'


    changeNodeText: ->
      $node = @$el
      if $node.hasClass('root')
        $childrenContainer = $node.children('.rightChildren')
      else
        $childrenContainer = $node.children('.children:first')
      
      preWidth = $node.outerWidth()
      preHeight = $node.outerHeight()
      childrenHeight = $childrenContainer.outerHeight()
      parentIsHeigher = preHeight > childrenHeight

      $contentContainer = $node.children('.inner-node').children('.content')
      if @model.get 'isHtml'
        document.log "Changing text as HTML"
        $contentContainer.addClass('isHtml')
        $contentContainer.html(@model.get 'nodeText')
      else
        document.log "Changing text as PLAIN"
        $contentContainer.removeClass('isHtml')
        $contentContainer.text(@model.get 'nodeText')
      @alignButtons()
      
      postHeight = $node.outerHeight()
      diffWidth = 0
      if $($node).hasClass('right') or $($node).hasClass('root')
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
      
      if $($node).hasClass('right') or $($node).hasClass('root')
        $childrenContainer.animate {
          left: '+='+diffWidth
        },  document.fadeDuration
      
      if $($node).hasClass('root')
        $childrenContainer = $node.children('.children')
      $childrenContainer.animate {
        top: '+='+(postHeight-preHeight)/2
      },  document.fadeDuration

      if $node.parent().children('.node').size() > 0
        $node.animate {
          top: '-='+(postHeight-preHeight)/2
        }, document.fadeDuration, =>
          if !!@connection
            @connection.repaintConnection()

      
     
    foldModel: ->
      @$el.toggleClass('selected')
      @model.set 'folded', not @model.get 'folded'

    subView: (view, autoRender = false) ->
      # if model is set, use its id OR a unique random id
      viewId = view.model?.id or String(Math.random() * new Date().getTime())
      # add view to subviews
      @subViews[viewId] = view
      view


    getRenderData: ->
    # if the model is already set, parse it to json
      data = {}
      if @model?
        data = @model.toJSON()
        data['simpleTooltip'] = ($.inArray('SIMPLE_TOOLTIP', document.features) > -1)
        data['lockable'] = ($.inArray('LOCK_NODE', document.features) > -1)
      data


    alignButtons:->
      $fold = @$el.children('.inner-node').children('.action-fold')

      height = (@$el.outerHeight()/2 - $fold.outerHeight()/2)
      $fold.css('top', height+"px")
      
      if !!@controls
        @controls.alignToNode()
        

    scale:(amount)->
      @getElement().zoomTo({targetsize:amount*(@$el.outerWidth()/@$el.parent().width()), duration:600, root: @getElement().parent()});
      
    
    resizeTree: ($node, nodeModel, height)->
      parent = nodeModel.get 'parent'
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
        setTimeout(->
          parent.updateConnection()
          for child in parent.get('children')
            child.updateConnection()
        , document.fadeDuration)
      
      if !parent.isRoot()
        @resizeTree $parent, parent, height

    
  module.exports = AbstractNodeView