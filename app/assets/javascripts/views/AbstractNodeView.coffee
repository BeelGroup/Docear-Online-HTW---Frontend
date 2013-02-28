define ['models/Node', 'views/SyncedView', 'views/HtmlView'], (nodeModel, SyncedView, HtmlView) ->
  module = ->
  
  class AbstractNodeView extends SyncedView

    tagName: 'div',
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


    # a.k.a. constructor
    constructor: (@model) ->
      super()
      id: @model.get 'id'
      @model.bind "change:locked",@changeLockStatus , @   
      @model.bind "change:selected",@changeSelectStatus , @   
      @model.bind "change:folded",@changeFoldedStatus , @   

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


    # define events -> here u can pass informations to the model
    events: =>
      'click .changeable': 'lockModel'
      'click .action-show': 'printModel'
      'click .action-change': 'modificateModel'
      'click .action-save': (-> @model.save(@model.saveOptions))
    
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
      
      childrenHeight = $children.outerHeight()
      nodeHeight = $node.outerHeight()
      
      if $children.children('.node').size() > 0
        diff = childrenHeight - nodeHeight
        $node.parent().closest('.children').children('svg, ._jsPlumb_endpoint').fadeOut(200)
        if isVisible
          if childrenHeight > nodeHeight
            @resizeTree $node, diff
          $children.fadeOut(document.fadeDuration, ->
            $node.parent().closest('.children').children('._jsPlumb_endpoint').show()
            jsPlumb.repaintEverything()
          )
        else
          @resizeTree $node, -diff
  
          $children.fadeIn(document.fadeDuration, ->
            $node.parent().closest('.children').children('._jsPlumb_endpoint').show()
            jsPlumb.repaintEverything()
          )
        
      
    

    # [Debugging] 
    printModel: ->      
      ##console.log @model.toJSON()

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
      for viewId, view of @subViews
        html = view.render().el
        $(html).appendTo(@el)
      # extend the ready rendered htlm element
      @afterRender()
      @

      
    resizeTree: ($node, height)->
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
        }, document.fadeDuration)
      
        $nextBrother = $($node).next('.node')
        while $nextBrother.size() > 0
          $($nextBrother).animate({
            top: '-='+(height)
          }, document.fadeDuration)
          $nextBrother = $($nextBrother).next('.node')
        @resizeTree $parent, height

    
    alignControls: (model, recursive = false)->
      nodes = [model]
      while node = nodes.shift()
        $node = $('#'+node.id)
        if recursive
          nodes = $.merge(nodes, node.get('children').slice()  )
        $innerNode = $($node).children('.inner-node')
        $fold = $($innerNode).children('.fold')
        $($fold).css('top', ($($innerNode).outerHeight()/2 - $($fold).outerHeight()/2)+"px")
        
        $($fold).click (event)->
          $node = $(this).closest('.node')
          nodeId = $node.attr('id')
          isVisible = $($node).children('.children').is(':visible')
          model.findById(nodeId).set 'folded', isVisible
        
        $($innerNode).click (event)->
          $selectedNode = $('.node.selected')
          selectedNodeId = $($selectedNode).attr('id')
          
          $currentNode = $(this).closest('.node')
          currentNodeId = $($currentNode).attr('id')
          
          if $selectedNode.size() > 0
            selectedNode = model.findById(selectedNodeId);
            selectedNode.set 'selected', false
            
            if $($selectedNode).closest('.children').children('#'+currentNodeId).size() > 0
              selectedNode.set 'previouslySelected', false

          currentNode = model.findById(currentNodeId)
          currentNode.set 'previouslySelected', true
          currentNode.set 'selected', true
          
          
      
      

  module.exports = AbstractNodeView