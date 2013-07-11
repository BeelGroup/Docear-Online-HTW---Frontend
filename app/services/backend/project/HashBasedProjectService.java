package services.backend.project;

import static services.backend.project.filestore.PathFactory.path;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import models.backend.exceptions.sendResult.NotFoundException;
import models.backend.exceptions.sendResult.SendResultException;
import models.project.exceptions.InvalidFileNameException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.docear.messages.models.MapIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Logger;
import play.libs.Akka;
import play.libs.F.Promise;
import services.backend.mindmap.MindMapCrudService;
import services.backend.project.filestore.FileStore;
import services.backend.project.filestore.PathFactory;
import services.backend.project.filestore.PathFactory.PathFactoryHashedFile;
import services.backend.project.persistance.Changes;
import services.backend.project.persistance.EntityCursor;
import services.backend.project.persistance.FileIndexStore;
import services.backend.project.persistance.FileMetaData;
import services.backend.project.persistance.Project;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService implements ProjectService {
	
	private static final char[] ILLEGAL_CHARACTERS = { '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

    private final Map<String, List<UpdateCallable>> projectListenersMap = new HashMap<String, List<UpdateCallable>>();
    private final Map<String, List<UpdateCallable>> userListenerMap = new HashMap<String, List<UpdateCallable>>();
    @Autowired
    private FileStore fileStore;
    @Autowired
    private FileIndexStore fileIndexStore;
    @Autowired
    private MindMapCrudService mindMapCrudService;

    private static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Invalid Crypto algorithm! ", e);
        }
    }

    public String sha512(InputStream inputStream) throws IOException {
        return DigestUtils.sha512Hex(inputStream);
    }

    @Override
    public Project createProject(String username, String name) throws IOException {
        final Project project = fileIndexStore.createProject(name, username);
        callListenerForChangeForUser(username);
        return project;
    }

    @Override
    public void addUserToProject(String projectId, String usernameToAdd) throws IOException {
        List<Project> projects = getProjectsFromUser(usernameToAdd);
        for (Project project : projects) {
            if (project.getId().equals(projectId)) {
                throw new SendResultException("User is already in project.", 403);
            }
        }
        fileIndexStore.addUserToProject(projectId, usernameToAdd);
        callListenerForChangeForUser(usernameToAdd);
        callListenersForChangeInProject(projectId);
    }

    @Override
    public boolean removeUserFromProject(String projectId, String usernameToRemove, final boolean keepLastUser) throws IOException {
    	Logger.debug("HashBasedProjectService.removeUserFromProject => projectId: "+projectId+"; usernameToRemove: "+usernameToRemove);
        final boolean removed = fileIndexStore.removeUserFromProject(projectId, usernameToRemove, keepLastUser);
        if (removed) {
            callListenerForChangeForUser(usernameToRemove);
            callListenersForChangeInProject(projectId);
        }
        return removed;
    }

    @Override
    public Project getProjectById(String projectId) throws IOException {
        return fileIndexStore.findProjectById(projectId);
    }

    @Override
    public List<Project> getProjectsFromUser(String username) throws IOException {
    	EntityCursor<Project> projects = null;
    	List<Project> projectsList = null;
    	try {
    		projects = fileIndexStore.findProjectsFromUser(username);
    		if(projects != null)
    			projectsList = convertEntityCursorToList(projects);
    		else
    			projectsList = new ArrayList<Project>();
    	} finally {
    		if(projects != null)
    			projects.close();
    	}
        return projectsList;
    }

    @Override
    public InputStream getFile(String projectId, String path, boolean zipped) throws IOException, InvalidFileNameException {
        Logger.debug("HashBasedProjectService.getFile => projectId: " + projectId + "; path: " + path);
        path = normalizePath(path);

        try {
            // look for file in fileIndexStore
            FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
            if (metadata == null) {
                throw new NotFoundException("File not found!");
            }

            //check if file is an open mindmap
            final MapIdentifier mapIdentifier = new MapIdentifier(projectId, path);
            if(mindMapCrudService.isMindMapOpened(mapIdentifier)) {
            	mindMapCrudService.saveMindMapInProjectService(mapIdentifier);
            }
            metadata = fileIndexStore.getMetaData(projectId, path);
            final String fileHash = metadata.getHash();
            Logger.debug("HashBasedProjectService.getFile => fileHash: " + fileHash);

            if (zipped)
                return fileStore.open(path().hash(fileHash).zipped());
            else
                return fileStore.open(path().hash(fileHash).raw());

        } catch (FileNotFoundException e) {
            throw new NotFoundException("File not found!", e);
        }
    }

    @Override
    public FileMetaData metadata(String projectId, String path) throws IOException {
        path = normalizePath(path);
        Logger.debug("HashBasedProjectService => projectId: " + projectId + "; path: " + path);
        final FileMetaData metadata = fileIndexStore.getMetaData(projectId, path);
        if (metadata == null) {
            throw new NotFoundException("File not found!");
        }
        return metadata;
    }

    @Override
    public EntityCursor<FileMetaData> getMetaDataOfDirectChildren(String id, String path, int max) throws IOException {
        return fileIndexStore.getMetaDataOfDirectChildren(id, path, max);
    }

    @Override
    public FileMetaData createFolder(String projectId, String path) throws IOException {
        path = normalizePath(path);
        upsertFoldersInPath(projectId, path);
        final FileMetaData metadata = FileMetaData.folder(path, false);
        fileIndexStore.upsertFile(projectId, metadata);
        final FileMetaData newMetaData = fileIndexStore.getMetaData(projectId, path);
        callListenersForChangeInProject(projectId);
        return newMetaData;
    }

    private void upsertFoldersInPath(String projectId, String path) throws IOException {
        path = normalizePath(path);

        // check that root exists
        if (fileIndexStore.getMetaData(projectId, "/") == null) {
            fileIndexStore.upsertFile(projectId, FileMetaData.folder("/", false));
        }
        final String[] folders = path.split("/");
        // dont check before first slash, dont check last part (new resoource)
        String currentPath = "";
        for (int i = 1; i < folders.length - 1; i++) {
            currentPath += "/" + folders[i];
            Logger.debug("upsertFoldersInPath => currentPath: " + currentPath);
            final FileMetaData metadata = fileIndexStore.getMetaData(projectId, currentPath);
            if (metadata == null || !metadata.isDir() || metadata.isDeleted()) {
                Logger.debug("upsertFoldersInPath => Inserting Folder: " + currentPath);
                fileIndexStore.upsertFile(projectId, FileMetaData.folder(currentPath, false));
            }
        }

    }

    
    @Override
    public FileMetaData putFile(String projectId, String path, byte[] fileBytes, boolean isZip, Long parentRevision, boolean forceOverride) throws IOException, InvalidFileNameException {
        Logger.debug("putFile => projectId: " + projectId + "; path: " + path + "; forceOverride: " + forceOverride + "; bytes: " + fileBytes.length);
        
        //check that path has no invalid signs
        
        if(StringUtils.containsAny(path, ILLEGAL_CHARACTERS)) {
        	throw new InvalidFileNameException("The filename contains invalid characters.");
        }
        String actualPath = normalizePath(path);
        upsertFoldersInPath(projectId, actualPath);

        
        
        //check that path is not rootpath
        if (actualPath.equals("/"))
            throw new SendResultException("Cannot override root!", 400);

        // check if file is present, not deleted and not forced to be overriden
        final FileMetaData currentServerMetaData = fileIndexStore.getMetaData(projectId, actualPath);
        if (currentServerMetaData != null && !currentServerMetaData.isDeleted() && !forceOverride) {
            final Long currentServerRevision = currentServerMetaData.getRevision();
            // when file is present it is important that parentRev has been
            if (parentRevision == null)
                throw new SendResultException("parentRevision is mandatory, because file is present", 400);
            if (currentServerRevision > parentRevision) {
                // Conflict! change path to a conflicted version path
                Logger.debug("Conflict");
                int indexOfLastDot = actualPath.lastIndexOf('.');
                final int indexOfLastSignBeforeExtension = indexOfLastDot != -1 ? indexOfLastDot : actualPath.length();

                String conflictedPath;
                boolean resourcePresent;
                int currentConflictedNumber = 0;
                do {
                    currentConflictedNumber++;
                    conflictedPath = new StringBuffer(actualPath).insert(indexOfLastSignBeforeExtension, "(Conflicted Version " + currentConflictedNumber + ")").toString();
                    final FileMetaData meta = fileIndexStore.getMetaData(projectId, conflictedPath);
                    resourcePresent = meta == null ? false : meta.isDeleted() == true ? false : true;
                } while (resourcePresent);

                actualPath = conflictedPath;
            } else if (parentRevision > currentServerRevision) {
                throw new SendResultException("Given Revision is too big!", 400);
            }
        }

        //check if file for hash is already present
        final PutFileResult result = isZip ? putFileInStoreWithZippedFileBytes(fileBytes) : putFileInStoreWithFileBytes(fileBytes);
        final String fileHash = result.getFileHash();
        final long bytes = result.getBytes();

        // update file in index
        Logger.debug("actualPath: " + actualPath);
        final FileMetaData metadata = FileMetaData.file(actualPath, fileHash, bytes, false);
        Logger.debug(metadata.toString());
        fileIndexStore.upsertFile(projectId, metadata);
        final FileMetaData newMetaData = fileIndexStore.getMetaData(projectId, actualPath);
        callListenersForChangeInProject(projectId);
        return newMetaData;
    }

    @Override
    public void moveFile(String projectId, String oldPath, String newPath) throws IOException {
        oldPath = normalizePath(oldPath);
        newPath = normalizePath(newPath);

        // check that old resource is present
        final FileMetaData oldMeta = fileIndexStore.getMetaData(projectId, oldPath);
        if (oldMeta == null || oldMeta.isDeleted()) {
            throw new NotFoundException("file not found");
        }

        upsertFoldersInPath(projectId, newPath);

        if (oldMeta.isDir()) {
            // directory handling
            // check if resource is already a folder
            final FileMetaData currentNewMeta = fileIndexStore.getMetaData(projectId, newPath);
            final FileMetaData newMeta = FileMetaData.folder(newPath, false);
            if (currentNewMeta == null || currentNewMeta.isDeleted() || !currentNewMeta.isDir()) {
                // when not present or no folder, make it a folder
                fileIndexStore.upsertFile(projectId, newMeta);
            }

            moveFileRecursion(projectId, oldMeta, newMeta);
            fileIndexStore.upsertFile(projectId, FileMetaData.folder(oldPath, true));
        } else {
            // file handling
            final FileMetaData newMeta = FileMetaData.file(newPath, oldMeta.getHash(), oldMeta.getBytes(), false);
            fileIndexStore.upsertFile(projectId, newMeta);
            fileIndexStore.upsertFile(projectId, FileMetaData.file(oldPath, "", 0, true));
        }

        callListenersForChangeInProject(projectId);
    }

    private void moveFileRecursion(String projectId, FileMetaData folderMetadata, FileMetaData newFolderMetadata) throws IOException {
        final EntityCursor<FileMetaData> oldChildrenMetadatas = fileIndexStore.getMetaDataOfDirectChildren(projectId, folderMetadata.getPath(), Integer.MAX_VALUE);
        Logger.debug("HashBasedProjectService.moveFileRecursion => folderMeta: " + folderMetadata + "; newFolderMeta: " + newFolderMetadata);

        try {
            final String oldBasePath = folderMetadata.getPath();
            final String newBasePath = newFolderMetadata.getPath();
            for (final FileMetaData oldMetadata : oldChildrenMetadatas) {
                // ignore deleted files
                if (oldMetadata.isDeleted())
                    continue;

                final String newPath = oldMetadata.getPath().replace(oldBasePath, newBasePath);
                // create and upsert on new position
                final FileMetaData newMetadata = oldMetadata.isDir() ? FileMetaData.folder(newPath, false) : FileMetaData.file(newPath, oldMetadata.getHash(), oldMetadata.getBytes(), false);
                fileIndexStore.upsertFile(projectId, newMetadata);

                // trigger recursion for folders
                if (newMetadata.isDir()) {
                    // delete on old position
                    fileIndexStore.upsertFile(projectId, FileMetaData.folder(oldMetadata.getPath(), true));
                    moveFileRecursion(projectId, oldMetadata, newMetadata);
                } else {
                    // delete on old position
                    fileIndexStore.upsertFile(projectId, FileMetaData.file(oldMetadata.getPath(), "", 0, true));
                }
            }
        } finally {
            oldChildrenMetadatas.close();
        }
    }

    /**
     * @param zippedFileBytes
     * @return fileHash
     * @throws IOException
     */
    private PutFileResult putFileInStoreWithZippedFileBytes(byte[] zippedFileBytes) throws IOException {
        OutputStream out = null;
        ZipInputStream zipStream = null;
        DigestInputStream digestIn = null;
        String fileHash = null;
        long fileByteCount = -1;

        try {
            final String zippedTmpPath = path().tmp();
            final String tmpPath = path().tmp();

            // copy zipfile to tmp dir
            out = fileStore.create(zippedTmpPath);
            IOUtils.write(zippedFileBytes, out);
            IOUtils.closeQuietly(out);

            // get unzipped file
            zipStream = new ZipInputStream(new ByteArrayInputStream(zippedFileBytes));
            zipStream.getNextEntry();
            out = fileStore.create(tmpPath);
            // write to temp location
            fileByteCount = IOUtils.copy(zipStream, out);
            IOUtils.closeQuietly(out);
            // get hash
            digestIn = new DigestInputStream(zipStream, createMessageDigest());
            fileHash = sha512(digestIn);
            
            if(isFileAlreadyPresent(fileHash)) {
            	Logger.debug("putFile => file already present, do not save.");
            	fileStore.delete(zippedTmpPath);
            	fileStore.delete(tmpPath);
            	return new PutFileResult(fileHash, fileByteCount);
            }
            
            Logger.debug("putFile => saving file");
            final String unzippedPath = path().hash(fileHash).raw();
            final String zippedPath = path().hash(fileHash).zipped();

            fileStore.move(tmpPath, unzippedPath);
            fileStore.move(zippedTmpPath, zippedPath);

        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(zipStream);
        }
        return new PutFileResult(fileHash, fileByteCount);
    }

    private PutFileResult putFileInStoreWithFileBytes(byte[] fileBytes) throws IOException {
        OutputStream out = null;
        ZipOutputStream zipStream = null;
        ByteArrayInputStream byteArryIn = null;
        DigestInputStream digestIn = null;
        String fileHash = null;
        long fileByteCount = -1;

        try {
            final String tmpPath = path().tmp();

            // copy file to tmp dir
            byteArryIn = new ByteArrayInputStream(fileBytes);
            out = fileStore.create(tmpPath);
            fileByteCount = IOUtils.copy(byteArryIn, out);
            IOUtils.closeQuietly(out);

            // get hash
            digestIn = new DigestInputStream(new ByteArrayInputStream(fileBytes), createMessageDigest());
            fileHash = sha512(digestIn);
            
            if(isFileAlreadyPresent(fileHash)) {
            	Logger.debug("putFile => file already present, do not save.");
            	fileStore.delete(tmpPath);
            	return new PutFileResult(fileHash, fileByteCount);
            }

            Logger.debug("putFile => saving file");
            final String unzippedPath = path().hash(fileHash).raw();
            final String zippedPath = path().hash(fileHash).zipped();

            // write zipped file
            out = fileStore.create(zippedPath);
            zipStream = new ZipOutputStream(out);
            zipStream.putNextEntry(new ZipEntry("file"));
            IOUtils.write(fileBytes, zipStream);
            zipStream.closeEntry();
            IOUtils.closeQuietly(zipStream);
            IOUtils.closeQuietly(out);

            fileStore.move(tmpPath, unzippedPath);
        } finally {
            IOUtils.closeQuietly(zipStream);
            IOUtils.closeQuietly(byteArryIn);
            IOUtils.closeQuietly(out);
        }
        return new PutFileResult(fileHash, fileByteCount);
    }
    
    private boolean isFileAlreadyPresent(String fileHash) {
    	final PathFactoryHashedFile path = PathFactory.path().hash(fileHash);
    	
    	InputStream in = null;
    	boolean present = true;
    	try {
			fileStore.open(path.raw());
		} catch (FileNotFoundException e) {
			present = false;
		} catch (IOException e) {
			present = false;
		} finally {
			IOUtils.closeQuietly(in);
		}
    	
    	return present;
    }

    @Override
    public Promise<JsonNode> listenIfUpdateOccurs(String username, Map<String, Long> projectRevisionMap, boolean longPolling) throws IOException {
    	final Map<String, List<String>> projectUserMap = new HashMap<String, List<String>>();
        for (String projectId : projectRevisionMap.keySet()) {
            final Project project = fileIndexStore.findProjectById(projectId);
            if (project != null) {
                projectUserMap.put(projectId, project.getAuthorizedUsers());
            }
        }

        final UpdateCallable callable = new UpdateCallable(fileIndexStore, projectRevisionMap, projectUserMap, username);

        final Promise<JsonNode> promise = Akka.future(callable);

        if (longPolling) {
            // put in listener maps
            for (Map.Entry<String, Long> entry : projectRevisionMap.entrySet()) {
                final String projectId = entry.getKey();

                // check if projectId has entry in listener map
                if (!projectListenersMap.containsKey(projectId)) {
                    projectListenersMap.put(projectId, Collections.synchronizedList(new ArrayList<UpdateCallable>()));
                }
                projectListenersMap.get(projectId).add(callable);
            }

            // put in User listener map
            if (!userListenerMap.containsKey(username))
                userListenerMap.put(username, new ArrayList<UpdateCallable>());
            userListenerMap.get(username).add(callable);
        } else {
            callable.send();
        }

        return promise;
    }

    private void callListenersForChangeInProject(String projectId) {
        if (projectListenersMap.containsKey(projectId)) {
            final List<UpdateCallable> callableList = projectListenersMap.get(projectId);
            while (callableList.size() > 0) {
                final UpdateCallable callable = callableList.get(0);
                if (!callable.hasBeenCalled())
                    callable.send();

                callableList.remove(callable);
            }
        }
    }

    private void callListenerForChangeForUser(String username) {
        if (userListenerMap.containsKey(username)) {
            final List<UpdateCallable> callableList = userListenerMap.get(username);
            while (callableList.size() > 0) {
                final UpdateCallable callable = callableList.get(0);
                if (!callable.hasBeenCalled())
                    callable.send();

                callableList.remove(callable);
            }
        }
    }

    @Override
    public VersionDeltaResponse versionDelta(String projectId, Long cursor) throws IOException {
        final Project project = fileIndexStore.findProjectById(projectId);
        final Long currentRevision = project.getRevision();
        final Map<String, FileMetaData> resources = new HashMap<String, FileMetaData>();
        Changes changes = null;
        if (project.getRevision() > cursor) {
            changes = fileIndexStore.getProjectChangesSinceRevision(projectId, cursor);
        } else {
            changes = new Changes(new ArrayList<String>());
        }

        for (String resource : changes.getChangedPaths()) {
            final FileMetaData metadata = fileIndexStore.getMetaData(projectId, resource);
            resources.put(resource, metadata);
        }

        return new VersionDeltaResponse(currentRevision, resources);
    }

    @Override
    public boolean userBelongsToProject(String username, String projectId) {
        return fileIndexStore.userBelongsToProject(username, projectId);
    }

    @Override
    public FileMetaData delete(String projectId, String path) throws IOException {
        path = normalizePath(path);
        Logger.debug("HashBasedProjectService => projectId: " + projectId + "; path: " + path);

        final FileMetaData oldMetadata = fileIndexStore.getMetaData(projectId, path);

        if (oldMetadata == null) {
            throw new NotFoundException("File is not present");
        }

        // check if already deleted
        if (oldMetadata.isDeleted())
            return oldMetadata;

        FileMetaData metadata = null;
        if (oldMetadata.isDir())
            metadata = FileMetaData.folder(path, true);
        else
            metadata = FileMetaData.file(path, "", 0L, true);

        fileIndexStore.upsertFile(projectId, metadata);

        callListenersForChangeInProject(projectId);

        return metadata;
    }

    @Override
    public String toString() {
        return "HashBasedProjectService{" + "fileStore=" + fileStore + '}';
    }

    private <A> List<A> convertEntityCursorToList(EntityCursor<A> cursor) throws IOException {
        try {
            final List<A> list = new ArrayList<A>();
            final Iterator<A> it = cursor.iterator();

            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        } finally {
            cursor.close();
        }
    }

    /**
     * applies url decode and adds leading slash
     *
     * @param path
     * @return
     */
    private String normalizePath(String path) {
        String normalizedPath = "";

        try {
            normalizedPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("Problem with UTF-8");
        }

        if (!normalizedPath.startsWith("/"))
            return "/" + normalizedPath;

        return normalizedPath;
    }

    private final static class PutFileResult {
        private final String fileHash;
        private final long bytes;

        private PutFileResult(String fileHash, long bytes) {
            this.fileHash = fileHash;
            this.bytes = bytes;
        }

        private String getFileHash() {
            return fileHash;
        }

        private long getBytes() {
            return bytes;
        }
    }
}
