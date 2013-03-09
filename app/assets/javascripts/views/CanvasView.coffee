define ->
  module = () ->

  class Canvas extends Backbone.View

    tagName: 'div'
    className: 'mindmap-canvas'



    constructor:(@id, @width = 8000, @height = 8000, @zoomAmount = 100)->
      super()

    moreEvents:()=>
      @$el.mousewheel (event, delta, deltaX, deltaY)=>
        $viewport = @$el.parent()          
        x = event.pageX - $viewport.offset().left - $viewport.width()/2
        y = event.pageY - $viewport.offset().top - $viewport.height()/2
        shift = 'x': x, 'y': y
        if deltaY > 0 then @zoomIn(event, shift) else @zoomOut(event, shift)
        event.preventDefault() 

      $(document).keydown (event)=>
        if !($(event.target).is('input, textarea')) and typeof @rootView != "undefined"
          @rootView.userKeyInput event




    getElement:()->
      $("##{@id}")


    afterAppend:()->
      @$el.draggable
        cancel: "a.ui-icon, .inner-node, :input"
        containment: @$el.parent().attr('id')
        cursor: "move"
        handle: @id


    move:(x,y)->
      @$el.css 
       'left'  : "#{(@$el.css 'left')+x}px"
       'top'   : "#{(@$el.css 'top')+y}px"


    zoomIn:(event)=>
      if(@zoomAmount+document.zoomStep <= document.maxZoom)
        @zoomAmount += document.zoomStep
        @zoom(event)
       

    zoomOut:(event, shift)=>
      if(@zoomAmount-document.zoomStep >= document.minZoom)
        @zoomAmount -= document.zoomStep
        @zoom(event)


    zoom:(event)=>
      if(typeof @rootView != "undefined")
        console.log "zoom:#{@zoomAmount}%"
        @rootView.scale @zoomAmount/100
        @$el.trigger 'zoom', @zoomAmount/100


    zoomCenter:()=>
      if(typeof @rootView != "undefined")
        @zoomAmount = 100
        @zoom()
        @center()
        #@rootView.centerInContainer()
        
        @$el.trigger 'center'


    center:->
      # compute center of canvas - center of viewport (== total center)
      xPos = @width  / 2 - @$el.parent().width()  / 2
      yPos = @height / 2 - @$el.parent().height() / 2
      @$el.animate 
       'left'  : "#{-xPos}px"
       'top'   : "#{-yPos}px"


    setRootView:(@rootView)->
      @rootView.getElement().on 'newSelectedNode', (event, selectedNode)-> console.log "new selection: #{selectedNode.get 'id'}"
      @zoomAmount = 100

    renderAndAppendTo:($element)->
      $element.append(@render().el)

      @$el.css 
        'width' : "#{@width}px"
        'height': "#{@height}px"

      @center()
      @moreEvents()
      @afterAppend()



  module.exports = Canvas