package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.dao.AbstractDao;
import home.yura.websearchgui.model.AbstractModel;
import org.skife.jdbi.v2.DBI;

import java.util.Objects;

/**
 * @author yuriy.dunko on 08.03.17.
 */
public abstract class AbstractJdbiDao<T extends AbstractModel> implements AbstractDao<T> {
    protected final DBI dbi;

    AbstractJdbiDao(final DBI dbi) {
        this.dbi = Objects.requireNonNull(dbi, "DBI cannot be null");
    }
}
