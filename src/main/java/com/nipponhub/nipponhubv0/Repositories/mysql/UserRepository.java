package com.nipponhub.nipponhubv0.Repositories.mysql;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nipponhub.nipponhubv0.Models.OurUsers;
import com.nipponhub.nipponhubv0.Models.Enum.UserRole;

/**
 * The interface User repository.
 */
public interface UserRepository extends JpaRepository<OurUsers, Long> {

    /**
     * Find by email optional.
     *
     * @param email the email
     * @return the optional
     */
    Optional<OurUsers> findByEmail(String email);

    /**
     * Find by username optional.
     *
     * @param username the username
     * @return the optional
     */    
    Optional<OurUsers> findByName(String name);

    /**
     * Find by role list.
     *
     * @param role the role
     * @return the list
     */
    List<OurUsers> findByRole(UserRole role);
}