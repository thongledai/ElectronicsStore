//package com.example.store.config;
//
//import java.util.List;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import com.example.store.entity.Role;
//import com.example.store.entity.User;
//import com.example.store.repository.RoleRepository;
//import com.example.store.repository.UserRepository;
//
//@Configuration
//public class DataInitializer {
//
//    @Bean
//    public CommandLineRunner initDatabase(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
//        return args -> {
//            List<String> roleNames = List.of("CUSTOMER", "MANAGER", "EMPLOYEE", "SHIPPER");
//            for (String roleName : roleNames) {
//                if (!roleRepository.existsByName(roleName)) {
//                    roleRepository.save(Role.builder().name(roleName).build());
//                }
//            }
//
//            // Seed demo accounts if not exist
//            Role managerRole = roleRepository.findByName("MANAGER").orElse(null);
//            Role customerRole = roleRepository.findByName("CUSTOMER").orElse(null);
//            Role shipperRole = roleRepository.findByName("SHIPPER").orElse(null);
//            Role employeeRole = roleRepository.findByName("EMPLOYEE").orElse(null);
//
//            if (managerRole != null && !userRepository.existsByEmail("manager@technova.com")) {
//                userRepository.save(User.builder()
//                        .fullName("Manager TechNova")
//                        .email("manager@technova.com")
//                        .phone("0987654321")
//                        .isEmailActive(true)
//                        .isPhoneActive(true)
//                        .hashedPassword(passwordEncoder.encode("Manager@123"))
//                        .role(managerRole)
//                        .build());
//            }
//
//            if (customerRole != null && !userRepository.existsByEmail("demo@technova.com")) {
//                userRepository.save(User.builder()
//                        .fullName("Demo Customer")
//                        .email("demo@technova.com")
//                        .phone("0912345678")
//                        .isEmailActive(true)
//                        .isPhoneActive(true)
//                        .hashedPassword(passwordEncoder.encode("Demo@123"))
//                        .role(customerRole)
//                        .build());
//            }
//            if(shipperRole != null && !userRepository.existsByEmail("shipper@gmail.com")) {
//				userRepository.save(User.builder()
//						.fullName("Anh shipper may mắn")
//						.email("shipper@gmail.com")
//						.phone("0912345679")
//						.isEmailActive(true)
//						.isPhoneActive(true)
//						.hashedPassword(passwordEncoder.encode("Shipper@123"))
//						.role(shipperRole)
//						.build());
//				}
//            if(employeeRole != null && !userRepository.existsByEmail("employee@gmail.com")){
//            			userRepository.save(User.builder()
//						.fullName("Anh nhân viên xui xẻo")
//						.email("employee@gmail.com")
//						.phone("0912345685")
//						.isEmailActive(true)
//						.isPhoneActive(true)
//						.hashedPassword(passwordEncoder.encode("Employee@123"))
//						.role(employeeRole)
//						.build());
//            }
//        };
//    }
//}
