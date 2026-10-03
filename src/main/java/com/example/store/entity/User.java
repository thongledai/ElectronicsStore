package com.example.store.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity(name = "user")
public class User {
    @Id()
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

}
