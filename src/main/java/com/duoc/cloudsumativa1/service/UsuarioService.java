package com.duoc.cloudsumativa1.service;

import com.duoc.cloudsumativa1.model.Usuario;
import com.duoc.cloudsumativa1.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public Usuario crear(Usuario usuario) {
        String correo = usuario.getCorreo().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(usuario.getNombre());
        nuevo.setCorreo(correo);
        return repository.save(nuevo);
    }

    public List<Usuario> listar() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
