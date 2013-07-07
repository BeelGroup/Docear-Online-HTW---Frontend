package services.backend.mindmap;

import org.docear.messages.Messages.MapClosedMessage;
import org.docear.messages.models.MapIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.Logger;
import services.backend.project.ProjectService;
import akka.actor.UntypedActor;

final class ProcessClosedMindMapsActor extends UntypedActor {

	private final MetaDataCrudService metaDataCrudService;
	private final ProjectService projectService;

	public ProcessClosedMindMapsActor(MetaDataCrudService metaDataCrudService, ProjectService projectService) {
		super();
		this.metaDataCrudService = metaDataCrudService;
		this.projectService = projectService;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		Logger.debug("ProcessClosedMindMapsActor.onReceive => " + message.getClass().getSimpleName() + " received.");
		if (message instanceof MapClosedMessage) {

			final MapClosedMessage request = (MapClosedMessage) message;
			final MapIdentifier mapIdentifier = request.getMapIdentifier();
			final byte[] bytes = request.getFileBytes();
			final String projectId = mapIdentifier.getProjectId();
			final String path = mapIdentifier.getMapId();
			Logger.debug("ProcessClosedMindMapsActor.onReceive => byte count: "+ bytes.length);
			Logger.debug("ProcessClosedMindMapsActor.onReceive => projectId: "+ projectId);
			Logger.debug("ProcessClosedMindMapsActor.onReceive => path: "+ path);
			Logger.debug("ProcessClosedMindMapsActor.onReceive => projectService null? "+ (projectService == null));
			projectService.putFile(projectId, path, bytes, false, 0L, true);
			metaDataCrudService.delete(projectId, path);
		}
	}
}