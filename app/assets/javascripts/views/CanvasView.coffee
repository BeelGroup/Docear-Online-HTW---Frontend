define ->
  module = () ->

  class Canvas extends Backbone.View

    id: document.canvasID
    tagName: 'div'
    className: 'mindmap-canvas'

    constructor:(@zoomAmount = 100)->
      super()

    moreEvents:()=>
      $("##{document.canvasID}").mousewheel (event, delta, deltaX, deltaY)=>
        $viewport = $("##{document.viewportID}")          
        x = event.pageX - $viewport.offset().left - $viewport.width()/2
        y = event.pageY - $viewport.offset().top - $viewport.height()/2
        shift = 'x': x, 'y': y
        if deltaY > 0 then @zoomIn(event, shift) else @zoomOut(event, shift)
        event.preventDefault() 

      $(document).keydown (event)=>
        if @rootView
          @rootView.userKeyInput event


    getElement:()->
      $("##{@id}")


    afterAppend:()->
      @$el.draggable({
        cancel: "a.ui-icon, .node",
        containment: document.viewportID,
        cursor: "move",
        handle: document.canvasID 
      });


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


    zoomCenter:()=>
      @zoomAmount = 100
      @zoom()
      @rootView.centerInContainer()
      @center()
      @$el.trigger 'center'


    center:->
      xPos = document.canvasWidth/2 - $("##{document.viewportID}").width()/2
      yPos = document.canvasHeight/2 - $("##{document.viewportID}").height()/2
      @$el.css 
       'left'  : "#{-xPos}px",
       'top'   : "#{-yPos}px"

    setRootView:(@rootView)->


    renderAndAppendTo:($element)->
      $element.append(@render().el)

      @$el.css 
        'width' : "#{document.canvasWidth}px"
        'height': "#{document.canvasHeight}px"

      @center()
      @moreEvents()
      @afterAppend()



  module.exports = Canvas