package com.dmsBackend.ArchiveCodes;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivedDeleteRequest {
    private String objectName;
    private String collectionName;
    private String deletedReason;
}
