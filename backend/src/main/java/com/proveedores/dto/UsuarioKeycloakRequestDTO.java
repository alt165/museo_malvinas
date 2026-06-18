package com.proveedores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UsuarioKeycloakRequestDTO(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(max = 100, message = "El nombre de usuario no puede superar 100 caracteres")
        String username,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email debe tener un formato valido")
        @Size(max = 160, message = "El email no puede superar 160 caracteres")
        String email,

        @NotBlank(message = "El DNI es obligatorio")
        @Size(min = 7, max = 20, message = "El DNI debe tener entre 7 y 20 caracteres")
        @Pattern(regexp = "^[0-9]+$", message = "El DNI solo puede contener digitos")
        String dni,

        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 100, message = "El apellido no puede superar 100 caracteres")
        String apellido,

        Boolean habilitado,

        @Size(min = 8, max = 120, message = "La contrasena debe tener entre 8 y 120 caracteres")
        String contrasenaInicial,

        Set<String> roles
) {
}
