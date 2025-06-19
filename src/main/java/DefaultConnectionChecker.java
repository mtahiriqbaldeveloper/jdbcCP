

import api.EntityChecker;

import java.sql.Connection;
import java.util.logging.Logger;

public class DefaultConnectionChecker implements EntityChecker<Connection> {
    private static final Logger log = Logger.getLogger(DefaultConnectionChecker.class.getName());
    private static final String DB_CONNECTION_ERROR = "DB CONNECTION ERROR: TEST FAILED";
    private static final String CONNECTION_CHECK_METRIC_NAME = "CONNECTION.FAULTS";
    private static final String CONNECTION_CHECK_METRIC_DESC = "The number of failed JDBC connection checks";
    private int timeout;

    public DefaultConnectionChecker() {
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean check(Connection connection) {
        try {
            boolean valid = connection.isValid(this.timeout);
            if(!valid){
                this.onError((Throwable)null);
            }
            return valid;
        }catch (Throwable var3){
            Throwable t = var3;
            this.onError(t);
            return false;
        }
    }

    private void onError(Throwable throwable) {

    }

}
