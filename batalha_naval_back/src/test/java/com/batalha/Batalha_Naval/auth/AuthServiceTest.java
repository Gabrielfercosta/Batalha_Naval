package com.batalha.Batalha_Naval.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthService authService;
    private UsuarioRepository usuarioRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = mock(JwtService.class);
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    void cadastrarComSucesso() {
        when(usuarioRepository.existsByUsername("jogador1")).thenReturn(false);
        when(jwtService.gerarToken("jogador1")).thenReturn("token123");

        String token = authService.cadastrar("jogador1", "senha123");

        assertEquals("token123", token);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void cadastrarUsernameInvalidoCurto() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrar("ab", "senha123"));
    }

    @Test
    void cadastrarUsernameComCaracteresEspeciais() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrar("jog@dor!", "senha123"));
    }

    @Test
    void cadastrarSenhaCurta() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrar("jogador1", "ab1"));
    }

    @Test
    void cadastrarSenhaSemNumero() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrar("jogador1", "apenasletras"));
    }

    @Test
    void cadastrarSenhaSemLetra() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrar("jogador1", "123456"));
    }

    @Test
    void cadastrarSenhaComEspaco() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrar("jogador1", "senha 123"));
    }

    @Test
    void cadastrarUsernameJaExistente() {
        when(usuarioRepository.existsByUsername("jogador1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrar("jogador1", "senha123"));
    }

    @Test
    void loginComSucesso() {
        String senhaCripto = passwordEncoder.encode("senha123");
        Usuario usuario = new Usuario("jogador1", senhaCripto);
        when(usuarioRepository.findByUsername("jogador1")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken("jogador1")).thenReturn("token456");

        String token = authService.login("jogador1", "senha123");

        assertEquals("token456", token);
    }

    @Test
    void loginUsuarioInexistente() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.login("fantasma", "senha123"));
    }

    @Test
    void loginSenhaErrada() {
        String senhaCripto = passwordEncoder.encode("senha123");
        Usuario usuario = new Usuario("jogador1", senhaCripto);
        when(usuarioRepository.findByUsername("jogador1")).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> authService.login("jogador1", "senhaErrada1"));
    }
}
