package com.dataquadinc.dto;

import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserProfileDocument;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class UserProfileResponse {
    private String userId;
    private String userName;
    private String email;
    private String phoneNumber;
    private String emergencyContactNo;
    private String currentAddress;
    private String permanentAddress;
    private String linkedinUrl;
    private byte[] profilePhoto;
    private List<DocumentResponse> documents;

    public UserProfileResponse(UserDetails user, List<UserProfileDocument> docs) {
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.emergencyContactNo = user.getEmergencyContactNumber();
        this.currentAddress = user.getCurrentAddress();
        this.permanentAddress = user.getPermanentAddress();
        this.linkedinUrl = user.getLinkedinUrl();
        this.profilePhoto = user.getProfilePhoto();
        this.documents = docs.stream().map(DocumentResponse::new).collect(Collectors.toList());
    }
}
