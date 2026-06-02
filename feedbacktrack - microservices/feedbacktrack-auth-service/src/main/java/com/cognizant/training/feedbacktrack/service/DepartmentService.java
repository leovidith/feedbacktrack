package com.cognizant.training.feedbacktrack.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cognizant.training.feedbacktrack.model.Department;
import com.cognizant.training.feedbacktrack.repository.DepartmentRepository;
import com.cognizant.training.feedbacktrack.exception.ResourceNotFoundException; // Ensure this is imported

import lombok.extern.slf4j.Slf4j; 

@Service
@Slf4j
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Creates a new department. 
     * Used primarily by Admins to set up the organization structure.
     */
    public Department createDepartment(Department dept) {
        log.info("Creating a new department: {}", dept.getDepartmentName());
        Department savedDept = departmentRepository.save(dept);
        log.info("Successfully saved department '{}' with ID: {}", savedDept.getDepartmentName(), savedDept.getDepartmentId());
        return savedDept;
    }

    /**
     * Retrieves all departments.
     * Often called by the DepartmentClient's getAllDepartments() method.
     */
    public List<Department> getAllDepartments() {
        log.info("Request received to fetch all departments");
        List<Department> departments = departmentRepository.findAll();
        log.debug("Total departments found in database: {}", departments.size());
        return departments;
    }

    /**
     * NEW: Finds a specific department by ID.
     * This is the "target" method for DepartmentClient.getDepartmentById(Long id).
     */
    public Department getDepartmentById(Long id) {
        log.debug("Fetching department details for ID: {}", id);
        return departmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Department retrieval failed: ID {} not found", id);
                    return new ResourceNotFoundException("Department not found with ID: " + id);
                });
    }
}