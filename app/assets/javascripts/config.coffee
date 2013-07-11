document.zoomStep = 20
document.maxZoom = 200
document.minZoom = 20

document.nodeCount = 0

document.navigation = {}
document.navigation.key = {}
document.navigation.key.allowed = [37, 38, 39, 40, 32]
document.navigation.key.left = "left"
document.navigation.key.right = "right"
document.navigation.key.up = "up"
document.navigation.key.down = "down"
document.navigation.key.fold = "space"
document.navigation.key.centerMap = "esc"
document.navigation.key.strg = "ctrl"
document.navigation.key.del = "del"

document.navigation.key.edit = {}
document.navigation.key.edit.exit = "enter"

document.navigation.key.literals = ['a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','ä','ö','ü','ß',
									'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','Ä','Ö','Ü']

document.navigation.key.addSibling = "enter"
document.navigation.key.addChild = "ins"

document.illegalFilenameCharacter = "\\/:*?\"<>|`\\n\\r\\t\\0\\f"

document.fadeDuration = 150
document.scrollStep = 20
document.scrollDuration = 50

document.strgPressed = false

document.graph = {}
document.graph.defaultColor = '#487698'
document.graph.defaultWidth = 2

document.currentZoom = 1

document.initialLoad = false
document.loadChunkSize = 50
document.initialLoadChunkSize = 2000
document.sleepTimeOnPartialLoading = 40

document.lockRefresh = 10000
document.unlockDelay = 400

document.simpleTooltip = true
