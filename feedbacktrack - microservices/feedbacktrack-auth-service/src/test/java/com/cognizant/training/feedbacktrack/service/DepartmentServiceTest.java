package com.cognizant.training.feedbacktrack.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.cognizant.training.feedbacktrack.model.Department;
import com.cognizant.training.feedbacktrack.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository; 

    @InjectMocks
    private DepartmentService departmentService; 

    @Test
    void testCreateDepartment() {
       
        Department dept = new Department();
        dept.setDepartmentName("Engineering");
        
        Department savedDept = new Department();
        savedDept.setDepartmentId(1L);
        savedDept.setDepartmentName("Engineering");

        
        when(departmentRepository.save(dept)).thenReturn(savedDept);

        
        Department result = departmentService.createDepartment(dept);

      
        assertNotNull(result);
        assertEquals(1L, result.getDepartmentId());
        assertEquals("Engineering", result.getDepartmentName());
        
        
        verify(departmentRepository, times(1)).save(dept);
    }

    @Test
    void testGetAllDepartments() {
        
        List<Department> mockList = Arrays.asList(
            new Department(1L, "Sales"),
            new Department(2L, "Marketing")
        );
        when(departmentRepository.findAll()).thenReturn(mockList);

       
        List<Department> result = departmentService.getAllDepartments();

        
        assertEquals(2, result.size());
        assertEquals("Sales", result.get(0).getDepartmentName());
        
      
        verify(departmentRepository, times(1)).findAll();
    }
}