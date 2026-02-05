package requirementsAssistantAI.infrastructure.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
@Order(1)
public class DatabaseMigrationConfig implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "requirement_history", "requirement_id");
            if (columns.next()) {
                String isNullable = columns.getString("IS_NULLABLE");
                if ("NO".equals(isNullable)) {
                    try (var statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE requirement_history ALTER COLUMN requirement_id DROP NOT NULL");
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
    }
}
