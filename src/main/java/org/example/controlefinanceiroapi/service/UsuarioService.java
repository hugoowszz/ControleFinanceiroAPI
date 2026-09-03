package org.example.controlefinanceiroapi.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.controlefinanceiroapi.dto.LoginRequest;
import org.example.controlefinanceiroapi.dto.LoginResponse;
import org.example.controlefinanceiroapi.entity.Usuario;
import org.example.controlefinanceiroapi.repository.UsuarioRepository;
import org.example.controlefinanceiroapi.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    public Usuario criarUsuario(String nome, String email, String senha) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado: " + email);
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(nome);
        novoUsuario.setEmail(email);
        novoUsuario.setSenha(passwordEncoder.encode(senha));

        return usuarioRepository.save(novoUsuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario listarPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    }

    public Usuario editar(Long id, Usuario novoUsuario) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        usuario.setNome(novoUsuario.getNome());
        usuario.setEmail(novoUsuario.getEmail());
        if (novoUsuario.getSenha() != null && !novoUsuario.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(novoUsuario.getSenha()));
        }
        return usuarioRepository.save(usuario);
    }

    public void excluir(Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
        } else {
            throw new EntityNotFoundException();
        }
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getSenha())
        );

        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        String token = jwtTokenService.generateToken(usuario.getEmail(), usuario.getId());
        return new LoginResponse(token, usuario.getId(), usuario.getEmail(), usuario.getNome());
    }
}
