package com.university.ums.config;

import com.university.ums.model.Admin;
import com.university.ums.model.Student;
import com.university.ums.model.Teacher;
import com.university.ums.repository.AdminRepository;
import com.university.ums.repository.StudentRepository;
import com.university.ums.repository.TeacherRepository;
import com.university.ums.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Only admins can create admins, so an empty database would lock everyone out.
 * On startup, if there is no admin yet, create one from app.admin.* settings
 * (set APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD / APP_ADMIN_EMAIL in Docker).
 * If app.demo-data is true, also add demo users for the lab demo.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserAccountService accounts;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.password}")
    private String password;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.demo-data:false}")
    private boolean demoData;

    public DataInitializer(AdminRepository adminRepository, TeacherRepository teacherRepository,
                           StudentRepository studentRepository, UserAccountService accounts) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.accounts = accounts;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Runs only on an empty database, so restarts never create duplicates
        if (adminRepository.count() > 0) {
            return;
        }
        Admin admin = new Admin();
        accounts.applyCommonFields(admin, "System Administrator", email, null, username, password);
        adminRepository.save(admin);
        log.info("Created initial admin account '{}'", admin.getUsername());

        if (demoData) {
            createDemoUsers();
        }
    }

    /**
     * Inserted in this order, so on a fresh database the ids are sequential:
     * admins 1-2, teachers 3-5, students 6-8 (all roles share the users table's id counter).
     */
    private void createDemoUsers() {
        admin("Nusrat Jahan", "admin2@university.edu", "admin2", "admin123");

        teacher("Dr. Abdul Karim", "karim.cse@university.edu", "teacher1", "Computer Science and Engineering", "Professor");
        teacher("Farhana Akter", "farhana.eee@university.edu", "teacher2", "Electrical and Electronic Engineering", "Associate Professor");
        teacher("Tanvir Hasan", "tanvir.me@university.edu", "teacher3", "Mechanical Engineering", "Lecturer");

        student("Rahim Uddin", "rahim.2107001@university.edu", "student1", "2107001", "Computer Science and Engineering", 2021);
        student("Sadia Islam", "sadia.2107002@university.edu", "student2", "2107002", "Computer Science and Engineering", 2021);
        student("Arif Hossain", "arif.2203015@university.edu", "student3", "2203015", "Electrical and Electronic Engineering", 2022);

        log.info("Created demo users: admin2, teacher1-3 (password teacher123), student1-3 (password student123)");
    }

    private void admin(String name, String email, String username, String rawPassword) {
        Admin admin = new Admin();
        accounts.applyCommonFields(admin, name, email, null, username, rawPassword);
        adminRepository.save(admin);
    }

    private void teacher(String name, String email, String username, String department, String designation) {
        Teacher teacher = new Teacher();
        accounts.applyCommonFields(teacher, name, email, null, username, "teacher123");
        teacher.setDepartment(department);
        teacher.setDesignation(designation);
        teacherRepository.save(teacher);
    }

    private void student(String name, String email, String username, String studentId, String department, int batch) {
        Student student = new Student();
        accounts.applyCommonFields(student, name, email, null, username, "student123");
        student.setStudentId(studentId);
        student.setDepartment(department);
        student.setBatch(batch);
        studentRepository.save(student);
    }
}
