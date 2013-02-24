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


    scale:(amount)->
        #CSS3:
        possibilities = document.body.style

        if($.inArray('WebkitTransform', possibilities) or 
           $.inArray('MozTransform', inpossibilities) or 
           $.inArray('OTransform', possibilities) or 
           $.inArray('transform', possibilities))

          @getElement().css
            '-moz-transform'    : "scale(#{amount})"  #/* Firefox */
            '-webkit-transform' : "scale(#{amount})"  #/* Safari and Chrome */
            '-ms-transform'     : "scale(#{amount})"  #/* IE 9 */
            '-o-transform'      : "scale(#{amount})"  #/* Opera */
            #node.css 'zoom', "#{@zoomAmount}%"
            #node.effect("scale", {percent:150, origin:['middle','center']}, 500)   
        
        else
          console.log 'No CSS3'


    render: ->
      @$el.html @template @getRenderData()
      # render the subviews
      for viewId, view of @subViews
        html = view.render().el
        $(html).appendTo(@el)
      # extend the ready rendered htlm element
      @afterRender()
      @


    alignControls: (model, recursive = false)->
      nodes = [model]
      while node = nodes.shift()
        $node = $('#'+node.id)
        if recursive
          nodes = $.merge(nodes, node.get('children').slice()  )
        $innerNode = $($node).children('.inner-node')
        $fold = $($innerNode).children('.fold')
        $($fold).css('top', ($($innerNode).outerHeight()/2 - $($fold).outerHeight()/2)+"px")
        
      

  module.exports = AbstractNodeView