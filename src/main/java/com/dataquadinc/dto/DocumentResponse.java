package com.dataquadinc.dto;

import com.dataquadinc.model.UserProfileDocument;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponse {
    private Long id;
    private String documentType;
    private String fileName;
    private String fileType;
    private byte[] documentData;
    private LocalDateTime uploadedAt;

    public DocumentResponse(UserProfileDocument doc) {
        this.id = doc.getId();
        this.documentType = doc.getDocumentType();
        this.fileName = doc.getFileName();
        this.fileType = doc.getFileType();
        this.documentData = doc.getDocumentData();
        this.uploadedAt = doc.getUploadedAt();
    }
}
