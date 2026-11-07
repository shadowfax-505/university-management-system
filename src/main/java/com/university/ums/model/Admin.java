package com.university.ums.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends AppUser {

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
