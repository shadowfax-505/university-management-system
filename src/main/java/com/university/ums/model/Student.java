package com.university.ums.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student extends AppUser {

    // University roll / registration number, e.g. "2007001"
    @Column(nullable = false, unique = true, length = 20)
    private String studentId;

    @Column(nullable = false, length = 100)
    private String department;

    // Admission year, e.g. 2021
    @Column(nullable = false)
    private Integer batch;

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getBatch() { return batch; }
    public void setBatch(Integer batch) { this.batch = batch; }
}
