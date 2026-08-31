package com.example.departmentservice.integrationtest;

import com.example.departmentservice.Entity.Department;
import com.example.departmentservice.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // Uses your application-test.properties
class DepartmentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department savedDepartment;

    @BeforeEach
    void setUp() {
        // Clear the database before every test to ensure a clean slate
        departmentRepository.deleteAll();

        // Save a default department we can use for GET, PUT, and DELETE tests
        Department department = new Department();
        department.setName("Engineering");
        department.setDescription("Software Development Team");
        savedDepartment = departmentRepository.save(department);
    }

    @Test
    void testGetStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/departments/status", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Department Service is up and running on port 8082!", response.getBody());
    }

    @Test
    void testCreateDepartment() {
        // 1. Prepare data
        Department newDept = new Department();
        newDept.setName("Human Resources");
        newDept.setDescription("HR Team");

        // 2. Execute POST request
        ResponseEntity<Department> response = restTemplate.postForEntity("/api/departments", newDept, Department.class);

        // 3. Assert Response
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId()); // Verifies the DB generated an ID
        assertEquals("Human Resources", response.getBody().getName());
    }

    @Test
    void testGetDepartmentById() {
        // Execute GET request using the ID of the department we saved in setUp()
        ResponseEntity<Department> response = restTemplate.getForEntity(
                "/api/departments/" + savedDepartment.getId(),
                Department.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Engineering", response.getBody().getName());
    }

    @Test
    void testUpdateDepartment() {
        // 1. Prepare updated details
        Department updatedDetails = new Department();
        updatedDetails.setName("Engineering V2");
        updatedDetails.setDescription("Updated Engineering Description");

        HttpEntity<Department> requestEntity = new HttpEntity<>(updatedDetails);

        // 2. Execute PUT request
        ResponseEntity<Department> response = restTemplate.exchange(
                "/api/departments/" + savedDepartment.getId(),
                HttpMethod.PUT,
                requestEntity,
                Department.class
        );

        // 3. Assert Response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Engineering V2", response.getBody().getName());

        // 4. Verify the database was actually updated
        Department dbDept = departmentRepository.findById(savedDepartment.getId()).get();
        assertEquals("Engineering V2", dbDept.getName());
    }

    @Test
    void testDeleteDepartment() {
        // 1. Execute DELETE request
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/departments/" + savedDepartment.getId(),
                HttpMethod.DELETE,
                null,
                String.class
        );

        // 2. Assert Response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("deleted successfully"));

        // 3. Verify it is actually gone from the database
        assertEquals(0, departmentRepository.count());
    }
}