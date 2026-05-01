package com.proveedores.mapper;

import com.proveedores.entity.CategoriaObjeto;
import com.proveedores.entity.Depositante;
import com.proveedores.entity.Exhibicion;
import com.proveedores.entity.ObjetoMuseo;
import com.proveedores.entity.Ubicacion;
import com.proveedores.entity.Usuario;
import com.proveedores.entity.Veterano;

final class MapperReferences {
    private MapperReferences() {
    }

    static ObjetoMuseo objetoMuseo(Long id) {
        if (id == null) {
            return null;
        }
        ObjetoMuseo entity = new ObjetoMuseo();
        entity.setId(id);
        return entity;
    }

    static CategoriaObjeto categoriaObjeto(Long id) {
        if (id == null) {
            return null;
        }
        CategoriaObjeto entity = new CategoriaObjeto();
        entity.setId(id);
        return entity;
    }

    static Depositante depositante(Long id) {
        if (id == null) {
            return null;
        }
        Depositante entity = new Depositante();
        entity.setId(id);
        return entity;
    }

    static Veterano veterano(Long id) {
        if (id == null) {
            return null;
        }
        Veterano entity = new Veterano();
        entity.setId(id);
        return entity;
    }

    static Ubicacion ubicacion(Long id) {
        if (id == null) {
            return null;
        }
        Ubicacion entity = new Ubicacion();
        entity.setId(id);
        return entity;
    }

    static Usuario usuario(Long id) {
        if (id == null) {
            return null;
        }
        Usuario entity = new Usuario();
        entity.setId(id);
        return entity;
    }

    static Exhibicion exhibicion(Long id) {
        if (id == null) {
            return null;
        }
        Exhibicion entity = new Exhibicion();
        entity.setId(id);
        return entity;
    }
}
