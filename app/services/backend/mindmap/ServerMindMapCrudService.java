package services.backend.mindmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.WS;
import util.backend.ZipUtils;
import play.libs.F.Promise;

@Profile("backendProd")
@Component
@Deprecated//will be implemented stateless and with Akka Actors
public class ServerMindMapCrudService extends MindMapCrudServiceBase implements MindMapCrudService {
	private static Map<String, String> serverIdToMapIdMap = new HashMap<String, String>();
	
    @Override
    public Promise<JsonNode>  mindMapAsJson(String id) throws DocearServiceException, IOException {
    	String mindmapId = serverIdToMapIdMap.get(id);
        URL serverUrl = null;
        if(mindmapId == null) { //if not hosted, send to a server
        	Logger.debug("No map for server id " + id + ". Sending to server...");
            serverUrl = sendMapToDocearInstance(id);
            mindmapId = serverIdToMapIdMap.get(id);
        } else {
        	serverUrl = ServerMindmapMap.getInstance().getServerURLForMap(mindmapId);
        	Logger.debug("ServerId: " + id + "; MapId: " + mindmapId + "; url: " +serverUrl.toString());
        }
        String wsUrl = serverUrl.toString();
        WS.Response response = WS.url(wsUrl + "/map/" + mindmapId + "/json").get().get();
        if(response.getStatus() != 200) {
            throw new IOException("couldn't obtain mind map from server. Response code: " + response.getStatus());
        }
        return Promise.pure(response.asJson());
    }

    @Override
    public Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException {
    	if(user == null) {
    		throw new NullPointerException("user cannot be null");
    	}
    	
        String docearServerAPIURL = "https://api.docear.org/user";
        WS.Response response =  WS.url(docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/")
                .setHeader("accessToken", user.getAccessToken()).get().get();

        BufferedReader br = new BufferedReader (new StringReader(response.getBody().toString()));

        List<UserMindmapInfo> infos = new LinkedList<UserMindmapInfo>();
        for ( String line; (line = br.readLine()) != null; ){
            String[] strings = line.split("\\|#\\|");
            Logger.debug(line);
            UserMindmapInfo info = new UserMindmapInfo(strings[0], strings[1], strings[2], strings[3], strings[4]);
            infos.add(info);
        }
        
        return Promise.pure(Arrays.asList(infos.toArray(new UserMindmapInfo[0])));
    }

    private static URL sendMapToDocearInstance(String mapId) throws NoUserLoggedInException {
        //find server with capacity
        URL serverUrl = ServerMindmapMap.getInstance().getServerWithFreeCapacity();

        
        InputStream fileStream = null;
        String mmId = null;
        if(mapId.length() == 1) { //test map
        	mmId = mapId;
            fileStream = Play.application().resourceAsStream("mindmaps/"+mapId+".mm");
        } else { //map from user account
            try {
                User user = controllers.User.getCurrentUser();
                if(user == null)
                    throw new NoUserLoggedInException();
                
            	File mmFile = getMindMapFileFromDocearServer(user, mapId);
            	if(mmFile == null)
            		return null;
            	
            	//TODO just a hack, because we are currently using different ids for retrieval then supposed
            	mmId = getMapIdFromFile(mmFile);
                fileStream = new FileInputStream(mmFile);
            } catch (FileNotFoundException e) {
                Logger.error("can't find mindmap file", e);
            } catch (IOException e) {
                Logger.error("can't open mindmap file", e);
            }
        }

        serverIdToMapIdMap.put(mapId, mmId);

        //send file to server and put in map
        String wsUrl = serverUrl.toString();
        WS.url(wsUrl+"/map")
                .setHeader("Content-Type", "application/octet-stream")
                .setHeader("Content-Deposition", "attachement; filename=\""+mapId+".mm\"")
                .put(fileStream).get();
        ServerMindmapMap.getInstance().put(serverUrl, mmId);
        
        return serverUrl;
    }

    @Deprecated
    public static String createWebserviceUrl(int port) {
        return "http://localhost:"+port+"/rest/v1";
    }

    private static boolean closeDocearInstance(URL serverUrl) {
        String wsUrl = serverUrl.toString();
        F.Promise<WS.Response> promise = WS.url(wsUrl+"/close").get();
        if(promise.get().getStatus() == 200) {
            ServerMindmapMap.getInstance().remove(serverUrl);
            return true;
        }
        return false;
    }

    private static File getMindMapFileFromDocearServer(final User user, final String mmIdOnServer) throws IOException {
        String docearServerAPIURL = "https://api.docear.org/user";

        WS.Response response =  WS.url(docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/" + mmIdOnServer)
                .setHeader("accessToken", user.getAccessToken())
                .get().get();
        
        if(response.getStatus() == 200) {
        	return ZipUtils.extractMindmap(response.getBodyAsStream());
        } else {
        	return null;
        }
        	
    }
    
    private static String getMapIdFromFile(File mindmapFile) {
		try {
			DocumentBuilderFactory dbFactory =  DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(mindmapFile);

			doc.getDocumentElement().normalize();

			return doc.getDocumentElement().getAttribute("dcr_id");

		} catch (Exception e) {}


		return null;
	}
}
