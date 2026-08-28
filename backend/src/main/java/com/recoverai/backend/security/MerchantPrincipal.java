package com.recoverai.backend.security;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class MerchantPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String name;
    private final String password;
    private final MerchantStatus status;
    private final Collection<? extends GrantedAuthority> authorities;

    public MerchantPrincipal(UUID id, String email, String name, String password, MerchantStatus status,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.password = password;
        this.status = status;
        this.authorities = authorities != null ? authorities : Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT"));
    }

    public static MerchantPrincipal fromEntity(Merchant merchant) {
        return new MerchantPrincipal(
                merchant.getId(),
                merchant.getEmail(),
                merchant.getName(),
                merchant.getPasswordHash(),
                merchant.getStatus(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT"))
        );
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public MerchantStatus getStatus() {
        return status;
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
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != MerchantStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == MerchantStatus.ACTIVE;
    }
}
