package org.example.service;

import org.example.config.DocumentChunkConfig;
import org.example.dto.DocumentChunk;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

class DocumentChunkServiceTest {

    private DocumentChunkService documentChunkService;

    @BeforeEach
    void setUp() {
        DocumentChunkConfig chunkConfig = new DocumentChunkConfig();
        chunkConfig.setMaxSize(30);
        chunkConfig.setOverlap(5);

        documentChunkService = new DocumentChunkService();
        ReflectionTestUtils.setField(documentChunkService, "chunkConfig", chunkConfig);
    }

    @Test
    void shouldReturnEmptyChunks_whenContentIsBlank() {
        List<DocumentChunk> chunks = documentChunkService.chunkDocument("   ", "blank.md");

        Assertions.assertTrue(chunks.isEmpty(), "空白文档不应生成分片");
    }

    @Test
    void shouldSplitMarkdownByHeadingAndKeepTitle() {
        String content = "# 第一章\n短内容\n\n## 第二章\n另一段内容";

        List<DocumentChunk> chunks = documentChunkService.chunkDocument(content, "doc.md");

        Assertions.assertEquals(2, chunks.size(), "两个 Markdown 标题应拆成两个章节分片");
        Assertions.assertEquals("第一章", chunks.get(0).getTitle(), "分片应保留所属标题");
        Assertions.assertTrue(chunks.get(1).getContent().contains("第二章"), "标题行应保留在对应章节内容中");
    }

    @Test
    void shouldSplitLongSectionAndApplyOverlap() {
        String content = "第一段内容超过阈值需要分片。\n\n第二段继续补充更多内容。\n\n第三段作为最后内容。";

        List<DocumentChunk> chunks = documentChunkService.chunkDocument(content, "long.md");

        Assertions.assertTrue(chunks.size() >= 2, "长章节应被拆分为多个分片");
        Assertions.assertEquals(0, chunks.get(0).getChunkIndex(), "分片序号应从 0 开始");
        Assertions.assertEquals(1, chunks.get(1).getChunkIndex(), "后续分片序号应递增");
        Assertions.assertTrue(chunks.get(1).getStartIndex() <= chunks.get(0).getEndIndex(),
                "启用 overlap 后，后一个分片起始位置不应晚于前一个结束位置");
    }
}
