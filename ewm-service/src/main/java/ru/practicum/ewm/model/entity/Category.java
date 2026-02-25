package ru.practicum.ewm.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category", schema = "public")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;
}
