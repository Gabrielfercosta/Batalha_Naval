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

    public String cadastrar(String username, String senha) {
        if (username == null || !username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("O nome de usuário deve ter de 3 a 20 caracteres, usando apenas letras, números e _ (underline).");
        }
        if (senha == null || senha.length() < 6 || senha.length() > 30) {
            throw new IllegalArgumentException("A senha deve ter de 6 a 30 caracteres.");
        }
        if (senha.contains(" ")) {
            throw new IllegalArgumentException("A senha não pode conter espaços.");
        }
        if (!senha.matches(".*[a-zA-Z].*") || !senha.matches(".*\\d.*")) {
            throw new IllegalArgumentException("A senha deve conter ao menos uma letra e um número.");
        }
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Esse nome já está em uso.");
        }

        String senhaCriptografada = passwordEncoder.encode(senha);
        Usuario usuario = new Usuario(username, senhaCriptografada);
        usuarioRepository.save(usuario);

        return jwtService.gerarToken(username);
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
