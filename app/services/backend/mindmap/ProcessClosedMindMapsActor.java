package services.backend.mindmap;

import org.docear.messages.Messages.MapClosedMessage;
import org.docear.messages.models.MapIdentifier;

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
			
			projectService.putFile(projectId, path, bytes, false, 0L, true);
			metaDataCrudService.delete(projectId, path);
		}
	}
}