//package com.cognizant.training.feedbacktrack.service;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import com.cognizant.training.feedbacktrack.model.Department;
//import com.cognizant.training.feedbacktrack.model.User;
//import com.cognizant.training.feedbacktrack.repository.UserRepository;
//
//@ExtendWith(MockitoExtension.class)
//public class UserServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @Mock
//
//    @InjectMocks
//    private UserService userService;
//
//    private User inputUser;
//    private Department dept;
//
//    @BeforeEach
//    void setUp() {
//        dept = new Department();
//        dept.setDepartmentId(1L);
//        dept.setDepartmentName("Engineering");
//
//        inputUser = new User();
//        inputUser.setEmail("test@cognizant.com");
//        inputUser.setPassword("plain123");
//        inputUser.setDepartment(dept);
//    }
//
//    @Test
//    void testRegisterEmployee_Success() {
//
//        when(passwordEncoder.encode(anyString())).thenReturn("hashed123");
//
//        when(departmentClient.getDepartmentById(1L)).thenReturn(dept);
//
//        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
//
//
//        User savedUser = userService.registerEmployee(inputUser);
//
//
//        assertNotNull(savedUser);
//        assertEquals("hashed123", savedUser.getPassword());
//        assertEquals("Engineering", savedUser.getDepartment().getDepartmentName());
//
//
//        verify(departmentClient, times(1)).getDepartmentById(1L);
//        verify(userRepository).save(any(User.class));
//    }
//}