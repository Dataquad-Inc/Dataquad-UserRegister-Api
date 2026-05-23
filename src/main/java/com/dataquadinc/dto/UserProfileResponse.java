package com.dataquadinc.dto;

import com.dataquadinc.model.UserDetails;
import com.dataquadinc.model.UserProfileDocument;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class UserProfileResponse {
    private String userId;
    private String userName;
    private String email;
    private String personal_email;
    private LocalDate joining_date;
    private String phoneNumber;
    private String emergencyContactNo;
    private String currentAddress;
    private String permanentAddress;
    private String linkedinUrl;
    private String pan;
    private String adhar;
    private byte[] profilePhoto;
    private List<DocumentResponse> documents;

    public UserProfileResponse(UserDetails user, List<UserProfileDocument> docs) {
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
        this.personal_email = user.getPersonalemail();
        this.joining_date = user.getJoiningDate();
        this.phoneNumber = user.getPhoneNumber();
        this.emergencyContactNo = user.getEmergencyContactNumber();
        this.currentAddress = user.getCurrentAddress();
        this.permanentAddress = user.getPermanentAddress();
        this.linkedinUrl = user.getLinkedinUrl();
        this.pan = user.getPan();
        this.adhar = user.getAdhar();
        this.profilePhoto = user.getProfilePhoto();
        this.documents = docs.stream().map(DocumentResponse::new).collect(Collectors.toList());
    }
}
