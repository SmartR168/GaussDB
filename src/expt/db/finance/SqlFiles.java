package expt.db.finance;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
public final class SqlFiles {
    private SqlFiles() {}
    // These supplied schema/seed files contain no procedural bodies or semicolons inside literals.
    public static void execute(Connection c,String path) throws Exception {
        String sql=Files.readString(Path.of(path),StandardCharsets.UTF_8);
        for(String part:sql.split(";")) if(!part.trim().isEmpty())
            try(Statement s=c.createStatement()) { s.setQueryTimeout(45); s.execute(part); }
    }
}
