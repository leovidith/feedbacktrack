package com.cognizant.training.feedbacktrack.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.cognizant.training.feedbacktrack.model.Department;
import com.cognizant.training.feedbacktrack.service.DepartmentService;

import lombok.extern.slf4j.Slf4j; 
import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
// @CrossOrigin("*")
@Slf4j 
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping("/add")
    public Department addDepartment(@RequestBody Department department) {
        log.info("REST request to add a new department: {}", department.getDepartmentName());
        Department savedDept = departmentService.createDepartment(department);
        log.info("Department successfully created with ID: {}", savedDept.getDepartmentId());
        return savedDept;
    }

    @GetMapping("/all")
    public List<Department> getAll() {
        log.info("REST request to fetch all departments");
        List<Department> departments = departmentService.getAllDepartments();
        log.debug("Returning {} departments to the requester", departments.size());
        return departments;
    }
}