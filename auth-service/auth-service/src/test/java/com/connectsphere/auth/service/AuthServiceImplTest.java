package com.connectsphere.auth.service;

import com.connectsphere.auth.client.SearchClient;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.jms.DeactivationProducer;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SearchClient searchClient;

    @Mock
    private DeactivationProducer deactivationProducer;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerEncodesPasswordSetsDefaultsAndIndexesUser() {
        User input = new User();
        input.setUsername("alice");
        input.setEmail("alice@example.com");
        input.setPasswordHash("plain-password");
        input.setRole("");
        input.setProvider(null);

        User saved = new User();
        saved.setUserId(42);
        saved.setUsername("alice");
        saved.setEmail("alice@example.com");
        saved.setPasswordHash("encoded-password");
        saved.setRole("USER");
        saved.setProvider("LOCAL");
        saved.setActive(true);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(input)).thenReturn(saved);

        User result = authService.register(input);

        assertThat(result).isSameAs(saved);
        assertThat(input.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(input.getRole()).isEqualTo("USER");
        assertThat(input.getProvider()).isEqualTo("LOCAL");
        assertThat(input.isActive()).isTrue();
        verify(searchClient).indexUser(42, "alice", null, null, null);
    }

    @Test
    void registerRejectsDuplicateEmailBeforeEncodingOrSaving() {
        User input = new User();
        input.setUsername("alice");
        input.setEmail("alice@example.com");
        input.setPasswordHash("plain-password");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsJwtForActiveUserWithMatchingPassword() {
        User user = new User();
        user.setUserId(5);
        user.setUsername("bob");
        user.setEmail("bob@example.com");
        user.setPasswordHash("encoded");
        user.setRole("ADMIN");
        user.setActive(true);

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(5, "bob", "ADMIN")).thenReturn("jwt-token");

        String token = authService.login("bob@example.com", "secret");

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void changeUserRoleRejectsUnsupportedRole() {
        assertThatThrownBy(() -> authService.changeUserRole(5, "OWNER"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid role");

        verify(userRepository, never()).save(any());
    }
}
