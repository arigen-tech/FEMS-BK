package com.dmsBackend.ArchiveCodes;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteApiResponse {
    private String statusDescription;
    private Long requestId;
    private String statusName;
    private Integer statusCode;
}
