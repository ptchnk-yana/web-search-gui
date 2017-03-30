package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.dao.AbstractDao;
import home.yura.websearchgui.model.AbstractModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.skife.jdbi.v2.DBI;

import java.util.Objects;

/**
 * @author yuriy.dunko on 08.03.17.
 */
public abstract class AbstractJdbiDao<T extends AbstractModel> implements AbstractDao<T> {

    protected final Log log = LogFactory.getLog(getClass());
    protected final DBI dbi;

    AbstractJdbiDao(final DBI dbi) {
        this.dbi = Objects.requireNonNull(dbi, "DBI");
    }
}
