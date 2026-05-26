package Techalert.TechAlert.config;

import java.util.List;
import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConstraintInitializer {

    @Bean
    CommandLineRunner alignUsuarioPerfilConstraint(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        return args -> {
            try (var connection = dataSource.getConnection()) {
                String productName = connection.getMetaData().getDatabaseProductName();
                if (productName == null || !productName.toLowerCase().contains("postgresql")) {
                    return;
                }
            }

            List<String> perfisInvalidos = jdbcTemplate.query(
                    "select perfil from usuario where perfil not in ('ADMINISTRADOR', 'MORADOR')",
                    (rs, rowNum) -> rs.getString(1)
            );

            if (!perfisInvalidos.isEmpty()) {
                jdbcTemplate.update("""
                        update usuario
                        set perfil = case
                            when role = 'ADM' then 'ADMINISTRADOR'
                            else 'MORADOR'
                        end
                        where perfil not in ('ADMINISTRADOR', 'MORADOR')
                        """);
            }

            String constraintDefinition = jdbcTemplate.query(
                    """
                    select pg_get_constraintdef(c.oid)
                    from pg_constraint c
                    join pg_class t on t.oid = c.conrelid
                    where t.relname = 'usuario' and c.conname = 'chk_usuario_perfil'
                    """,
                    rs -> rs.next() ? rs.getString(1) : null
            );

            if (constraintDefinition != null && constraintDefinition.contains("OPERADOR")) {
                jdbcTemplate.execute("alter table usuario drop constraint if exists chk_usuario_perfil");
                jdbcTemplate.execute("""
                        alter table usuario
                        add constraint chk_usuario_perfil
                        check (perfil in ('ADMINISTRADOR', 'MORADOR'))
                        """);
            }
        };
    }
}
