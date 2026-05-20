package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "文件上传结果")
public class FileUploadRes {

    @Schema(description = "原始文件名", example = "knowledge-base.md")
    private String fileName;

    @Schema(description = "知识库对象标识", example = "knowledge-base/2026/05/20/uuid-knowledge-base.md")
    private String filePath;

    @Schema(description = "文件大小，单位字节", example = "2048")
    private Long fileSize;

    public FileUploadRes() {
    }

    public FileUploadRes(String fileName, String filePath, Long fileSize) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

}
