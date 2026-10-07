package com.example.employeeservice.controller;

import com.example.employeeservice.domain.Employee;
import com.example.employeeservice.repository.EmployeeRepository;
import com.example.employeeservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "services.notification.url=http://localhost:8083",
        "services.department.url=http://localhost:8082",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/employee_db_test",
        "spring.datasource.username=admin",
        "spring.datasource.password=admin",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.config.import="
})
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for pure performance throughput testing
class EmployeeApiPerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CacheManager cacheManager;

    private Long testEmployeeId;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("employees").clear();
        employeeRepository.deleteAll();

        Employee employee = new Employee();
        employee.setFirstName("LoadTest");
        employee.setLastName("User");
        employee.setEmail("loadtest@example.com");
        employee.setPassword("password123");
        employee.setDepartmentId(1L);
        Employee savedEmployee = employeeRepository.save(employee);

        testEmployeeId = savedEmployee.getId();
    }

    @Test
    void givenHighLoad_whenGetEmployeeApi_thenHandle100ConcurrentRequestsQuickly() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(get("/api/v1/employees/" + testEmployeeId))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("API Request failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long totalExecutionTime = endTime - startTime;

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);

        System.out.println("🔥 DAY 5 PERFORMANCE TEST RESULT: 100 concurrent requests processed in " + totalExecutionTime + " ms");
        assertThat(totalExecutionTime).isLessThan(2000);
    }
}