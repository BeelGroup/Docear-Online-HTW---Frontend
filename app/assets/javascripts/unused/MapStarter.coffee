define ['routers/DocearRouter', 'views/RootNodeView', 'views/NodeView', 'views/HtmlView', 'models/Node', 'models/RootNode'],  (DocearRouter, RootNodeView, NodeView,HtmlView,NodeModel,RootNodeModel) ->  
   
  module = ->

  class MapStarter extends Backbone.View


    el: $("body")
    events:
      """click  a[href^="#"]""": "gotToRoute" #catch all links starting with a hash

    initialize: ->
      @router = new  DocearRouter()
      Backbone.history.start()

    gotToRoute: (event)->
      hashRoute = event.target.attributes.href.value
      @router.navigate hashRoute, {trigger: true, replace: true} #TODO until "Uncaught Error: Backbone.history has already been started" replace:true must stay



    constructor:(@json)->
      @importNodes()


    positionNodes:->
      rootView = new RootNodeView @rootNode
      $rootHtml = $(rootView.render().el).html()
      $("#mindmap").append($rootHtml)
      rootView.renderChilds()
      
      ##mindMapPositioner = new Positioner($('#mindmap'), @rootNode)


    ## ############################################## 
    ## replace this with json parser            ##!!!
    ## ##############################################
    importNodes:->
      @rootNode = new RootNodeModel("ID_0", false, "root", 'mindmap')

      right1 = new NodeModel('ID_1',false,  'right 1')

      right1_1 = new NodeModel('ID_5',false,  'right 1.1')
      right1_2 = new NodeModel('ID_6',false,  'right 1.2')      
      right1_3 = new NodeModel('ID_2',false,  'right 1.3')        
      right1_4 = new NodeModel('ID_55',false,  'right 1.4')
      right1_5 = new NodeModel('ID_56',false,  'right 1.5')       
      right1_6 = new NodeModel('ID_57',false,  'right 1.6')

      right1_3_1 = new NodeModel('ID_3',false,  'right 1.3.1')
      right1_3_2 = new NodeModel('ID_4',false,  'right 1.3.2')
      
      right1_3.set 'children', [right1_3_1, right1_3_2], silent: true
      right1.set 'children', [right1_1, right1_2, right1_3, right1_4, right1_5, right1_6], silent: true
              
      right2 = new NodeModel('ID_7',false,  'right 2')
      right3 = new NodeModel('ID_8',false, "right 3<br><img src='/assets/images/docear/logo/main-logo.png' style='height: 68px; width: 350px' />", true)
      
      left1 = new NodeModel('ID_9',false,  'right 1')
      left1_1 = new NodeModel('ID_10',false,  'right 1.1')
      left1_1_1 = new NodeModel('ID_11',false,  'right 1.1.1')
      left1_1_2 = new NodeModel('ID_12',false,  'right 1.1.2')
      left1_1.set 'children', [left1_1_1, left1_1_2], silent: true
              
      left1_2 = new NodeModel('ID_13',false,  'right 1.2')
      left1_2_1 = new NodeModel('ID_14',false,  'right 1.2.1')
      left1_2_2 = new NodeModel('ID_15',false,  'right 1.2.2')
      left1_2_3 = new NodeModel('ID_16',false,  'right 1.2.3')
      left1_2.set 'children', [left1_2_1, left1_2_2, left1_2_3], silent: true
      
      @rootNode.set 'rightChildren', [right1, right2, right3]
      @rootNode.set 'leftChildren', [left1]



  module.exports = MapStarter