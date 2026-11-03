package expt.db.finance;
import java.nio.file.Path;
public final class launch {
    public static void main(String[] args) throws Exception {
        if(args.length!=1) throw new IllegalArgumentException("Usage: launch config/db.properties");
        try(DBUtils db=DBUtils.interactive(Path.of(args[0]))) { new FlowControl(db).run(); }
    }
}
