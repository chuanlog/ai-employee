package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RebuildVectorStoreResponse {

    private int totalDocuments;

    private int successCount;

    private int failCount;

    private List<String> failedDocuments = new ArrayList<>();
}
