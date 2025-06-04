package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.entity.User;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.UserNotFoundException;
import com.example.matchup.matchupbackend.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailServiceTest {

    @InjectMocks
    private UserDetailService userDetailService;

    @Mock
    private UserRepository userRepository;

    @Test
    void testLoadUserByUsername_Success() {
        String email = "test@example.com";
        User user = mock(User.class);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User result = userDetailService.loadUserByUsername(email);
        assertNotNull(result);
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void testLoadUserByUsername_UserNotFound_Exception() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userDetailService.loadUserByUsername(email));
    }
}