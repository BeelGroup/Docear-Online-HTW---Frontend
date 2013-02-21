package services.backend.mindmap;

import play.Configuration;
import play.Play;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

//TODO backend team, what is the task of this class?
//TODO Play Framework is stateless don't keep state there
//TODO maybe bad code because of Singleton
@Deprecated//will be implemented stateless and with Akka Actors
public class ServerMindmapMap {
	private final Map<String, Set<String>> serverMapIdMap;
	private final Map<String, String> mapIdServerMap;
	private final int maxMapsPerServer;
	private final int initialPort;
    private static ServerMindmapMap INSTANCE;

	private ServerMindmapMap(int maxMapsPerServer, int initialPort) {
		serverMapIdMap = Collections.synchronizedMap(new HashMap<String, Set<String>>());
		mapIdServerMap = Collections.synchronizedMap(new HashMap<String, String>());
		this.maxMapsPerServer = maxMapsPerServer;
		this.initialPort = initialPort;
	}

    public static ServerMindmapMap getInstance() {
        if (INSTANCE == null) {
            INSTANCE = createInstance();
        }
        return INSTANCE;
    }

    private static ServerMindmapMap createInstance() {
        final Configuration conf = Play.application().configuration();
        int mapsPerInstance = conf.getInt("backend.mapsPerInstance");
        boolean useSingleDocearInstance = conf.getBoolean("backend.useSingleInstance");
        final Integer port = conf.getInt("backend.firstPort");
        final ServerMindmapMap mindmapServerMap = new ServerMindmapMap(mapsPerInstance, port);

        if(useSingleDocearInstance) {
                final String hostURL = conf.getString("backend.singleInstance.host");
                //final String path = conf.getString("backend.v10.pathprefix");
                
                mindmapServerMap.put(hostURL, "5");
                mindmapServerMap.remove("5");
        }
        return mindmapServerMap;
    }

    public void put(String serverAddress, String mapId) {
		//add to port map
		if(!serverMapIdMap.containsKey(serverAddress)) {
			serverMapIdMap.put(serverAddress, new HashSet<String>());
		}
		serverMapIdMap.get(serverAddress).add(mapId);

		//add to id map
		mapIdServerMap.put(mapId, serverAddress);
	}

	/**
	 * 
	 * @param mapId map to remove
	 * @return hosting server or null of not hosted
	 */
	public String remove(String mapId) {
		if(mapIdServerMap.containsKey(mapId)) {
			//get hosting server
			String hostingServerPort = mapIdServerMap.get(mapId);
			Set<String> mapsOnHostingServer = serverMapIdMap.get(hostingServerPort);
			//remove from maps
			mapsOnHostingServer.remove(mapId);
			mapIdServerMap.remove(mapId);
			return hostingServerPort;
		} else {
			return null;
		}
	}
	
	/**
	 * removes a server from the list
	 * @return set with mapIds that have lost their server
	 */
	public Set<String> remove(URL server) {
		if(serverMapIdMap.containsKey(server)) {
			Set<String> mapIds = serverMapIdMap.remove(server);
			
			for(String id: mapIds) {
				mapIdServerMap.remove(id);
			}
			
			return mapIds;
		}
		
		return new HashSet<String>();
	}
	
	/**
	 * 
	 * @return server port or null if there is no free capacity
	 */
	public String getServerWithFreeCapacity() {
		for(Entry<String,Set<String>> entry : serverMapIdMap.entrySet()) {
			if(entry.getValue().size() < maxMapsPerServer) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	public boolean hasOpenMaps(URL serverUrl) {
		return serverMapIdMap.containsKey(serverUrl) ? serverMapIdMap.get(serverUrl).size() > 0 : false;
	}
	
	/**
	 * 
	 * @param mapId
	 * @return server port or null if not hosted
	 */
	public String getServerURLForMap(String mapId) {
		if(containsServerURLForMap(mapId)) {
			return mapIdServerMap.get(mapId);
		} else {
			return null;
		}
	}

    /**
     *
     * @param mapId
     * @return boolean if contains mapId
     */
    public boolean containsServerURLForMap(String mapId) {
        return mapIdServerMap.containsKey(mapId);
    }
}