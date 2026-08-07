package com.dmsBackend.ArchiveCodes;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteApiRequest {
    private String objectName;
    private String collectionName;
    private Integer instance;
    private Integer priority;
}
