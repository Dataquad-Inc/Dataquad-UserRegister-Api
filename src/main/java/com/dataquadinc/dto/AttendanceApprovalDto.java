package com.dataquadinc.dto;

import lombok.Data;

@Data
public class AttendanceApprovalDto {

    private Integer month;

    private Integer year;

    /*
      NULL -> Entire Month
      1-5  -> Week
     */
    private Integer weekNumber;

    private String entity;

    private String actionBy;

}