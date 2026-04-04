package com.nipponhub.nipponhubv0.Models;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.nipponhub.nipponhubv0.Models.Enum.UserRole;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * The type Our users.
 */
@Entity
@Table(name = "ourusers")
@Getter
@Setter 
@AllArgsConstructor 
@NoArgsConstructor
public class OurUsers implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long userid;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    @Email
    private String email;

    @Column(name = "telephone_num")
    private String telephone;

    @Column(name = "password")
    private String password;

    /**
     * The Images.
     */
    @Column(name = "images")
    public String images;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = true)
    private UserRole role;

    @OneToMany(mappedBy = "ouruser" , cascade = CascadeType.ALL)
    private List<Product> products;

    public OurUsers(Long id,
                    String name,
                    String email,
                    String telephone,
                    String password, String images, UserRole role) {

        this.userid = id;
        this.name = name;
        this.email = email;
        this.telephone = telephone;
        this.password = password;
        this.images = images;
        this.role = role;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }
    @Column(updatable = false)
    private Date createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = new Date();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
