package com.dataquadinc.dto;

import com.dataquadinc.model.UserProfileDocument;
import lombok.Data;

@Data
public class DocumentResponse {
    private String documentType;
    private String fileType;
    private byte[] documentData;

    public DocumentResponse(UserProfileDocument doc) {
        this.documentType = doc.getDocumentType();
        this.fileType = doc.getFileType();
        this.documentData = doc.getDocumentData();
    }
}
