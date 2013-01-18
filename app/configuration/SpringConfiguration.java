package configuration;

import controllers.MindMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;
import play.Configuration;
import play.Logger;
import play.Play;

import static org.apache.commons.lang.BooleanUtils.isTrue;

@ComponentScan({"controllers", "services", "services.backend.mindmap"})//add here packages containing @Component annotated classes
public class SpringConfiguration {


    private static AnnotationConfigApplicationContext APPLICATION_CONTEXT;

    public static <T> T getBean(Class<T> beanClass) {
        if (APPLICATION_CONTEXT == null) {
            throw new IllegalStateException("application context is not initialized");
        }
        return APPLICATION_CONTEXT.getBean(beanClass);
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
}