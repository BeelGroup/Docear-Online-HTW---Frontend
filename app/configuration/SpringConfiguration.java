package configuration;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;

import play.Configuration;
import play.Logger;
import play.Play;
import controllers.MindMap;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * 
 * Configuration for Spring Dependency Injection.
 * (Switch between mock and prod implementations)
 */
@ComponentScan({"controllers", "services", "services.backend.mindmap", "services.backend.project", "services.backend.project.filestore", "services.backend.project.filestore.hadoop", "models.project.persistance"})//add here packages containing @Component annotated classes
public class SpringConfiguration {


    private static AnnotationConfigApplicationContext APPLICATION_CONTEXT;

    public static <T> T getBean(Class<T> beanClass) {
        if (APPLICATION_CONTEXT == null) {
            throw new IllegalStateException("application context is not initialized");
        }
        T bean = null;
        if (APPLICATION_CONTEXT.getBeansOfType(beanClass).size() > 0) {
            bean = APPLICATION_CONTEXT.getBean(beanClass);
        }
        return bean;
    }

    public static void initializeContext(AnnotationConfigApplicationContext applicationContext) {
        APPLICATION_CONTEXT = applicationContext;
        APPLICATION_CONTEXT.register(SpringConfiguration.class);
        final ConfigurableEnvironment environment = APPLICATION_CONTEXT.getEnvironment();
        final Configuration conf = Play.application().configuration();
        final Configuration profileConfiguration = conf.getConfig("spring.activeProfiles");
        for (String key : profileConfiguration.keys()) {
            environment.addActiveProfile(profileConfiguration.getString(key));
        }
        APPLICATION_CONTEXT.refresh();
        Logger.info("active spring profiles: " + StringUtils.join(environment.getActiveProfiles(), ", "));
    }

    @Bean
    public MindMap mindMap() {
        return new MindMap();
    }

    @Bean
    public FileSystem fileSystem() throws IOException {
        final FileSystem fileSystem = new RawLocalFileSystem();
        final URI uri = new File("hadoop-fs").toURI();//TODO not suitable for prod, writes directly in working directory
        fileSystem.initialize(uri, new org.apache.hadoop.conf.Configuration());
        fileSystem.setWorkingDirectory(new Path(uri));
        return fileSystem;
    }
}