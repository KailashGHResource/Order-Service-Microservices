package com.example.departmentservice.integrationtest;

import com.example.departmentservice.dto.DepartmentRequestDto;
import com.example.departmentservice.dto.DepartmentResponseDto;
import com.example.departmentservice.infrastructure.entity.DepartmentEntity;
import com.example.departmentservice.infrastructure.repository.DepartmentJpaRepository;
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
    private DepartmentJpaRepository departmentRepository;

    private DepartmentEntity savedDepartment;

    @BeforeEach
    void setUp() {
        // Clear the database before every test to ensure a clean slate
        departmentRepository.deleteAll();

        // Save a default department we can use for GET, PUT, and DELETE tests
        DepartmentEntity department = new DepartmentEntity();
        department.setName("Engineering");
        department.setDescription("Software Development Team");
        department.setCode("ENG");
        savedDepartment = departmentRepository.save(department);
    }

    @Test
    void testGetStatus() {
        // Updated path to match /api/v1/departments/status
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/departments/status", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Updated status message to match controller response
        assertEquals("Department Service V1 is up and running on port 8082!", response.getBody());
    }

    @Test
    void testCreateDepartment() {
        // 1. Prepare data (Using Request DTO)
        DepartmentRequestDto newDept = new DepartmentRequestDto();
        newDept.setName("Human Resources");
        newDept.setDescription("HR Team");
        newDept.setCode("HR");

        // 2. Execute POST request expecting Response DTO
        ResponseEntity<DepartmentResponseDto> response = restTemplate.postForEntity(
                "/api/v1/departments",
                newDept,
                DepartmentResponseDto.class
        );

        // 3. Assert Response
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId()); // Verifies the DB generated an ID
        assertEquals("Human Resources", response.getBody().getName());
    }

    @Test
    void testGetDepartmentById() {
        // Execute GET request using the ID and expecting Response DTO
        ResponseEntity<DepartmentResponseDto> response = restTemplate.getForEntity(
                "/api/v1/departments/" + savedDepartment.getId(),
                DepartmentResponseDto.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Engineering", response.getBody().getName());
    }

    @Test
    void testUpdateDepartment() {
        // 1. Prepare updated details (Using Request DTO)
        DepartmentRequestDto updatedDetails = new DepartmentRequestDto();
        updatedDetails.setName("Engineering V2");
        updatedDetails.setDescription("Updated Engineering Description");
        updatedDetails.setCode("ENG-V2");

        HttpEntity<DepartmentRequestDto> requestEntity = new HttpEntity<>(updatedDetails);

        // 2. Execute PUT request expecting Response DTO
        ResponseEntity<DepartmentResponseDto> response = restTemplate.exchange(
                "/api/v1/departments/" + savedDepartment.getId(),
                HttpMethod.PUT,
                requestEntity,
                DepartmentResponseDto.class
        );

        // 3. Assert Response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Engineering V2", response.getBody().getName());

        // 4. Verify the database was actually updated
        DepartmentEntity dbDept = departmentRepository.findById(savedDepartment.getId()).get();
        assertEquals("Engineering V2", dbDept.getName());
    }

    @Test
    void testDeleteDepartment() {
        // 1. Execute DELETE request
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/departments/" + savedDepartment.getId(),
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