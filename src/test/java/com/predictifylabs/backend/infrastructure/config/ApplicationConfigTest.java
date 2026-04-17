package com.predictifylabs.backend.infrastructure.config;

import com.predictifylabs.backend.domain.model.Role;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.UserEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationConfig applicationConfig;

    @Test
    void shouldMapAdminRoleToRoleAdminAuthority() {
        when(userRepository.findByEmail("admin@predictify.dev"))
                .thenReturn(Optional.of(userWithRole(Role.ADMIN)));

        UserDetails userDetails = applicationConfig.userDetailsService().loadUserByUsername("admin@predictify.dev");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldMapNonAdminRolesToRoleUserAuthority() {
        when(userRepository.findByEmail("attendee@predictify.dev"))
                .thenReturn(Optional.of(userWithRole(Role.ATTENDEE)));

        UserDetails userDetails = applicationConfig.userDetailsService().loadUserByUsername("attendee@predictify.dev");

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void shouldFailClosedWhenRoleIsNull() {
        when(userRepository.findByEmail("broken@predictify.dev"))
                .thenReturn(Optional.of(userWithRole(null)));

        assertThatThrownBy(() -> applicationConfig.userDetailsService().loadUserByUsername("broken@predictify.dev"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User role is invalid");
    }

    private UserEntity userWithRole(Role role) {
        UserEntity user = new UserEntity();
        user.setEmail("user@predictify.dev");
        user.setPassword("hashed-password");
        user.setRole(role);
        return user;
    }
}
