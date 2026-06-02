package com.cognizant.training.feedbacktrack.repository;

import com.cognizant.training.feedbacktrack.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}