package services.backend.project;

import com.google.common.collect.Lists;
import configuration.SpringConfiguration;
import play.*;
import services.backend.project.persistance.Project;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import models.project.exceptions.InvalidFileNameException;

import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.FileUtils.readFileToByteArray;

/**
 * This plugin is loads files from a local folder into the file store and the index db.
 * It only loads if the setting embed.mongo.enabled is true and the array application.projects.fixtures
 * contains a path to files. Example: application.projects.fixtures=["test/resources/projects/Freeplane", "/home/username"]
 * For performance reasons application.conf should not contain a lot of files to load.
 * Warning: this plugin is only for development and testing, it should not used in production.
 */
public final class ProjectFixturesPlugin extends Plugin {
    private final Application application;

    public ProjectFixturesPlugin(Application application) {
        this.application = application;
    }

    public boolean enabled() {
        return application.configuration().getBoolean("embed.mongo.enabled");
    }

    public void onStart() {
        final Configuration conf = application.configuration();
        final List<String> fixtureFilePaths = conf.getStringList("application.projects.fixtures", Lists.<String>newArrayList());
        for (final String path : fixtureFilePaths) {
            try {
                addProject(path);
            } catch (IOException e) {
                Logger.error("can't setup fixtures", e);
            }
        }
    }

    private void addProject(String path) throws IOException {
        final ProjectService service = SpringConfiguration.getBean(ProjectService.class);
        final File projectFolder = new File(path);
        final Project project = service.createProject("Michael", projectFolder.getName());
        final List<String> allowedUsers = Arrays.asList("Julius", "Alex", "Florian", "Paul", "alschwank", "online-demo", "showtime1", "showtime2", "showtime3", "showtime4");
        for (final String user: allowedUsers) {
            service.addUserToProject(project.getId(), user);
        }
        final Iterator<File> fileIterator = iterateFiles(projectFolder, null, true);
        while (fileIterator.hasNext()) {
            final File file = fileIterator.next();
            final String pathForDb = file.getAbsolutePath().replace(projectFolder.getAbsolutePath(), "").replace('\\','/');
            try {
				service.putFile(project.getId(), pathForDb, readFileToByteArray(file), false, -1L, true);
			} catch (InvalidFileNameException e) {
				throw new IOException("Tried to add file with impossible filename: "+pathForDb+"! ", e);
			}
        }
    }
}
