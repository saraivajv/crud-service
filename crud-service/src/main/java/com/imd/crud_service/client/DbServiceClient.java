package com.imd.crud_service.client;

import com.imd.crud_service.dto.EmployeeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "db-service")
public interface DbServiceClient {

    @PostMapping("/db/employees")
    EmployeeDTO createEmployee(@RequestBody EmployeeDTO employee);

    @GetMapping("/db/employees")
    List<EmployeeDTO> getAllEmployees();

    @GetMapping("/db/employees/{id}")
    EmployeeDTO getEmployeeById(@PathVariable("id") Long id);

    @PutMapping("/db/employees/{id}")
    EmployeeDTO updateEmployee(@PathVariable("id") Long id, @RequestBody EmployeeDTO employee);

    @DeleteMapping("/db/employees/{id}")
    void deleteEmployee(@PathVariable("id") Long id);
}
