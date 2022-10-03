package ru.immagixe.CloudFileStorage.security.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class Person implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotEmpty(message = "Username must not be empty")
    @Size(min=2, max=100, message ="Username length must be between 2 and 100 characters")
    @Column(name = "username")
    private String username;

    @NotEmpty(message = "Password must not be empty")
    @Column(name = "password")
    private String password;

    @Column(name = "role")
    private String role;

    public Person(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
