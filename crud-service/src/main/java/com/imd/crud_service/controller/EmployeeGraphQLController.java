package com.imd.crud_service.controller;

import com.imd.crud_service.dto.EmployeeDTO;
import com.imd.crud_service.service.EmployeeService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class EmployeeGraphQLController {

    private final EmployeeService employeeService;

    public EmployeeGraphQLController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @QueryMapping
    public Flux<EmployeeDTO> employees() {
        return employeeService.getAllEmployees();
    }

    @QueryMapping
    public Mono<EmployeeDTO> employeeById(@Argument Long id) {
        return employeeService.getEmployeeById(id);
    }

    @MutationMapping
    public Mono<EmployeeDTO> createEmployee(@Argument EmployeeInput input) {
        EmployeeDTO newEmployee = new EmployeeDTO();
        newEmployee.setName(input.name());
        newEmployee.setPosition(input.position());
        newEmployee.setSalary(Double.valueOf(input.salary()));

        return employeeService.createEmployee(newEmployee);
    }

    @MutationMapping
    public Mono<EmployeeDTO> generateAndSaveReview(@Argument Long employeeId) {
        return employeeService.generateAndSaveReview(employeeId);
    }
}

record EmployeeInput(String name, String position, Float salary) {}