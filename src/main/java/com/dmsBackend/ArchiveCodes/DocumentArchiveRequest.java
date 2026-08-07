package com.dmsBackend.ArchiveCodes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class  DocumentArchiveRequest {

    private String collectionName;   // CategoryMaster.name
    private String comments;         // DocumentHeader.title
    private List<String> components; // DocumentDetails.fileName list
    private String filePathRoot;     // from properties
    private String media;            // from properties
    private String objectName;       // DocumentHeader.fileNo
    private String options;          // from properties
    private int priority;            // from properties
    private int qos;                 // from properties
    private String sourceServer;     // from properties
}
