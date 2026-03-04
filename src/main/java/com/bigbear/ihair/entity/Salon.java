package com.bigbear.ihair.entity;

import com.bigbear.ihair.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "salons")
public class Salon extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    private String email;

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees;

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HairService> services;
}
