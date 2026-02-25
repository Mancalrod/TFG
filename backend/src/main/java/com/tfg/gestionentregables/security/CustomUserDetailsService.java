package com.tfg.gestionentregables.security;

import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.EstudianteRepository;
import com.tfg.gestionentregables.repository.ProfesorRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correoElectronico) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con correo: " + correoElectronico));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (Boolean.TRUE.equals(usuario.getEsAdmin())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        if (profesorRepository.existsByUsuarioId(usuario.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESOR"));
        }

        if (estudianteRepository.existsByUsuarioId(usuario.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"));
        }

        return new User(
                usuario.getCorreoElectronico(),
                usuario.getContrasena(),
                authorities
        );
    }
}
