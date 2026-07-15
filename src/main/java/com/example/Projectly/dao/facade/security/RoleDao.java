package com.example.Projectly.dao.facade.security;

import com.example.Projectly.bean.core.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleDao extends JpaRepository<Role, String> {

    Optional<Role> findByName(String name);
}
