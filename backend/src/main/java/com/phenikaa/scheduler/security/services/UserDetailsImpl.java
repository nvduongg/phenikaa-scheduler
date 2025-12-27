package com.phenikaa.scheduler.security.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.phenikaa.scheduler.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String fullName;
    
    // --- CÁC TRƯỜNG PHÂN QUYỀN DỮ LIỆU ---
    private Long facultyId; // Dành cho ADMIN_FACULTY
    private Long schoolId;  // Dành cho ADMIN_SCHOOL (Mới thêm)

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    // Hàm build: Chuyển từ Entity User -> UserDetailsImpl
    public static UserDetailsImpl build(User user) {
        // Chuyển role string (VD: "ADMIN_SCHOOL") thành Authority
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole())
        );

        // Lấy ID Khoa (nếu có)
        Long facultyId = (user.getFaculty() != null) ? user.getFaculty().getId() : null;
        
        // Lấy ID Trường (nếu có) - CẬP NHẬT MỚI
        Long schoolId = (user.getSchool() != null) ? user.getSchool().getId() : null;

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                facultyId,
                schoolId, // <--- Nhớ truyền schoolId vào Constructor ở vị trí này
                user.getPassword(),
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // Các hàm check mặc định trả về true
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}