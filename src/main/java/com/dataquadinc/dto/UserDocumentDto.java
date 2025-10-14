package com.dataquadinc.dto;

import org.springframework.web.multipart.MultipartFile;

public class UserDocumentDto {
    private String documentType; // e.g., "PAN CARD"
    private MultipartFile file;

    public String getDocumentType() {
        return documentType;
    }
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    public MultipartFile getFile() {
        return file;
    }
    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
