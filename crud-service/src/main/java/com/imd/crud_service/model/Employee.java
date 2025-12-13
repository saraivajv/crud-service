package com.imd.crud_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;

@Table("employee") // Tabela no banco do CRUD
public class Employee implements Persistable<UUID> {

    @Id
    private UUID id;
    private String name;
    private String position;
    private Double salary;

    // Campos de Controle da Saga
    private String status;
    @Column("reason_msg")
    private String reasonMsg;

    @Transient
    private boolean newEmployee = false;

    public Employee() {}

    public Employee(UUID id, String name, String position, Double salary, String status) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.salary = salary;
        this.status = status;
        this.newEmployee = true; // Força o INSERT
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return newEmployee; }

    // Getters e Setters
    public void setStatus(String status) { this.status = status; }
    public void setReasonMsg(String reasonMsg) { this.reasonMsg = reasonMsg; }
    public String getName() { return name; }
    public String getPosition() { return position; }
    public Double getSalary() { return salary; }
}