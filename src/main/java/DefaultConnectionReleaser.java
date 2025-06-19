

import api.EntityReleaser;

import java.sql.SQLException;
import java.util.logging.Logger;

public class DefaultConnectionReleaser implements EntityReleaser<PoolConnection> {
    private static final Logger log = Logger.getLogger(DefaultConnectionReleaser.class.getName());

    public DefaultConnectionReleaser() {
    }

    @Override
    public void release(PoolConnection connection) {
        try {
            connection.uncheckGetWrappedConnection().close();
        }catch (SQLException var3){
            log.info(var3.getMessage());
            log.warning(String.format("unexpected expception occurred on attempt to close jdbc connection '%s' ",connection));
        }
    }
}
