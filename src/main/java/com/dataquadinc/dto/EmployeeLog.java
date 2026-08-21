package com.dataquadinc.dto;

import java.util.List;

public class EmployeeLog {
        private String empCode;
        private String empName;
        private String department;
        private List<String> rawLogs;
        private String loginTime;
        private String logoutTime;

        public EmployeeLog() {}
        public EmployeeLog(String empCode, String empName, String department, List<String> rawLogs, String loginTime, String logoutTime) {
            this.empCode = empCode;
            this.empName = empName;
            this.department = department;
            this.rawLogs = rawLogs;
            this.loginTime = loginTime;
            this.logoutTime = logoutTime;
        }
        // Getters & Setters
        public String getEmpCode() { return empCode; }
        public void setEmpCode(String empCode) { this.empCode = empCode; }

        public String getEmpName() { return empName; }
        public void setEmpName(String empName) { this.empName = empName; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public List<String> getRawLogs() { return rawLogs; }
        public void setRawLogs(List<String> rawLogs) { this.rawLogs = rawLogs; }

        public String getLoginTime() { return loginTime; }
        public void setLoginTime(String loginTime) { this.loginTime = loginTime; }

        public String getLogoutTime() { return logoutTime; }
        public void setLogoutTime(String logoutTime) { this.logoutTime = logoutTime; }

        @Override
        public String toString() {
            return "EmployeeLog{" +
                    "empCode='" + empCode + '\'' +
                    ", empName='" + empName + '\'' +
                    ", loginTime='" + loginTime + '\'' +
                    ", logoutTime='" + logoutTime + '\'' +
                    '}';
        }
    }

