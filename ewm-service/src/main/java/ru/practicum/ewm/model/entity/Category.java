package ru.practicum.ewm.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category", schema = "public")
//@Table(name = "category", uniqueConstraints = {
//        @UniqueConstraint(name = "UQ_CATEGORY_NAME", columnNames = "name")})
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;
}
