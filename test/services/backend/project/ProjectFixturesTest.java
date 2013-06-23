package services.backend.project;

import configuration.SpringConfiguration;
import mongo.MongoTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.WithApplication;
import services.backend.project.persistance.Project;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.FileUtils.readFileToByteArray;

public class ProjectFixturesTest extends MongoTest {
    private ProjectService service;

    @Override
    public void setUpApplication() throws Exception {
        super.setUpApplication();
        service = SpringConfiguration.getBean(ProjectService.class);
    }

    @Test
    public void testFixtures() throws Exception {

    }
}
