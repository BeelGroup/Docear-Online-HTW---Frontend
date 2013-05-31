package services.backend.project;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import models.project.persistance.FileIndexStore;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class UpdateCallable implements Callable<JsonNode> {
	private final FileIndexStore fileIndexStore;
	private final Map<String, Long> projectRevisionMap;
	private Semaphore semaphore = new Semaphore(0);

	private boolean hasBeenCalled = false;

	public UpdateCallable(FileIndexStore fileIndexStore, Map<String, Long> projectRevisionMap) {
		this.fileIndexStore = fileIndexStore;
		this.projectRevisionMap = projectRevisionMap;
	}

	public void send() {
		semaphore.release();
	}

	public boolean hasBeenCalled() {
		return hasBeenCalled;
	}

	@Override
	public JsonNode call() throws Exception {
		semaphore.acquireUninterruptibly();

		final Map<String, Long> newProjectRevisionMap = new HashMap<String, Long>();
		for (String projectId : projectRevisionMap.keySet()) {
			newProjectRevisionMap.put(projectId, fileIndexStore.findProjectById(projectId).getRevision());
		}

		hasBeenCalled = true;
		return new ObjectMapper().valueToTree(newProjectRevisionMap);
	}

}