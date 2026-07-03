package com.batalha.Batalha_Naval.auth;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void cadastrar(String username, String senha) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username é obrigatório.");
        }
        if (senha == null || senha.length() < 4) {
            throw new IllegalArgumentException("A senha precisa ter ao menos 4 caracteres.");
        }
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Esse username já está em uso.");
        }

        String senhaCriptografada = passwordEncoder.encode(senha);
        Usuario usuario = new Usuario(username, senhaCriptografada);
        usuarioRepository.save(usuario);
    }

    public String login(String username, String senha) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha inválidos."));

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            throw new IllegalArgumentException("Usuário ou senha inválidos.");
        }

        return jwtService.gerarToken(username);
    }
}
