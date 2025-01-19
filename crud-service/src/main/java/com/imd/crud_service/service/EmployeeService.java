package com.imd.crud_service.service;

import com.imd.crud_service.client.DbServiceClient;
import com.imd.crud_service.dto.EmployeeDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {
    
    private final DbServiceClient dbServiceClient;

    public EmployeeService(DbServiceClient dbServiceClient) {
        this.dbServiceClient = dbServiceClient;
    }

    // Criar um novo Employee
    public EmployeeDTO createEmployee(EmployeeDTO employee) {
        return dbServiceClient.createEmployee(employee);
    }

    // Obter todos os Employees
    public List<EmployeeDTO> getAllEmployees() {
        return dbServiceClient.getAllEmployees();
    }

    // Obter Employee por ID
    public Optional<EmployeeDTO> getEmployeeById(Long id) {
        try {
            EmployeeDTO employee = dbServiceClient.getEmployeeById(id);
            return Optional.ofNullable(employee);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Atualizar Employee
    public Optional<EmployeeDTO> updateEmployee(Long id, EmployeeDTO employeeDetails) {
        try {
            EmployeeDTO updated = dbServiceClient.updateEmployee(id, employeeDetails);
            return Optional.ofNullable(updated);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Deletar Employee
    public void deleteEmployee(Long id) {
        dbServiceClient.deleteEmployee(id);
    }
}
