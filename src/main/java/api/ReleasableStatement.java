package api;

import java.sql.SQLException;

public interface ReleasableStatement {
    void release() throws SQLException;
    long getLastTimeUsed();
}
