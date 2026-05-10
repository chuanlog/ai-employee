package org.example.mapper;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.TestTableEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class TestTableMapperTest {

    @Autowired
    private TestTableMapper testTableMapper;

    @Test
    void shouldInsertRecordIntoTestTable() {
        String uniqueName = "mapper-test-" + System.currentTimeMillis();
        TestTableEntity entity = new TestTableEntity();
        entity.setName(uniqueName);

        Integer insertedId = null;
        try {
            int insertedRows = testTableMapper.insert(entity);
            insertedId = entity.getId();

            Assertions.assertEquals(1, insertedRows, "应成功插入 1 条记录");
            Assertions.assertNotNull(insertedId, "插入后应回填自增主键");

            TestTableEntity saved = testTableMapper.selectById(insertedId);
            Assertions.assertNotNull(saved, "应能根据主键查询到记录");
            Assertions.assertEquals(uniqueName, saved.getName(), "查询结果名称应与插入值一致");
        } finally {
            log.info("测试数据， ID: {}", insertedId);
        }
    }
}
