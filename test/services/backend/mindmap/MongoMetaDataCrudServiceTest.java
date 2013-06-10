package services.backend.mindmap;

import models.project.persistance.EntityCursor;
import mongo.MongoTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static com.google.common.collect.Lists.newArrayList;

public class MongoMetaDataCrudServiceTest extends MongoTest {
    public static final MetaData META_DATA_1 = new MetaData("demo-project-id", "mindMapResource-demo", 400L, 899999L);
    public static final MetaData META_DATA_1_CHANGED = new MetaData(META_DATA_1.getProjectId(), META_DATA_1.getMindMapResource(), 500L, 1000000000L);
    public static final MetaData META_DATA_2 = new MetaData("demo-project-id2", "mindMapResource-demo", 400L, 899999L);
    public static final MetaData META_DATA_3 = new MetaData("demo-project-id", "mindMapResource-demo2", 400L, 899999L);
    public static final MetaData META_DATA_4 = new MetaData("demo-project-id2", "mindMapResource-demo2", 400L, 899999L);
    private MetaDataCrudService service;

    @Before
    public void setUp() throws Exception {
        service = new MongoMetaDataCrudService();
    }

    @After
    public void tearDown() throws Exception {
        service = null;
    }

    @Test
    public void testInsert() throws Exception {
        assertThat(findMetaData1()).isNull();
        service.upsert(META_DATA_1);
        final MetaData persisted = findMetaData1();
        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualTo(META_DATA_1);

    }

    private MetaData findMetaData1() throws IOException {
        return service.find(META_DATA_1.getProjectId(), META_DATA_1.getMindMapResource());
    }

    @Test
    public void testUpdate() throws Exception {
        service.upsert(META_DATA_1);
        assertThat(findMetaData1()).isEqualTo(META_DATA_1);
        service.upsert(META_DATA_1_CHANGED);
        assertThat(findMetaData1()).isEqualTo(META_DATA_1_CHANGED);
    }

    @Test
    public void testDelete() throws Exception {
        service.upsert(META_DATA_1);
        assertThat(findMetaData1()).isEqualTo(META_DATA_1);
        service.delete(META_DATA_1.getProjectId(), META_DATA_1.getMindMapResource());
        assertThat(findMetaData1()).isNull();
    }

    @Test
    public void testFindAllMultipleValues() throws Exception {
        service.upsert(META_DATA_1);
        service.upsert(META_DATA_2);
        service.upsert(META_DATA_3);
        service.upsert(META_DATA_4);
        final EntityCursor<MetaData> all = service.findAll();
        final List<MetaData> metaDatas = newArrayList(all);
        all.close();
        assertThat(metaDatas).hasSize(4);
    }

    @Test
    public void testFindAllEmpty() throws Exception {
        final EntityCursor<MetaData> all = service.findAll();
        assertThat(all).isNotNull();
        final List<MetaData> metaDatas = newArrayList(all);
        all.close();
        assertThat(metaDatas).hasSize(0);
    }
}
