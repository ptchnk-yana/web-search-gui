package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.model.AbstractModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
import org.skife.jdbi.v2.DBI;

import java.util.List;
import java.util.function.Function;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

public abstract class AbstractJdbiResourceDao<T extends AbstractModel, S extends AbstractJdbiResourceDao.SqlObjectType<T>>
        extends AbstractJdbiDao<T> {

    protected final Class<T> beanClass;
    protected final Class<S> sqlObjectType;

    AbstractJdbiResourceDao(final DBI dbi, final Class<T> beanClass, final Class<S> sqlObjectType) {
        super(dbi);
        this.beanClass = requireNonNull(beanClass, "beanClass");
        this.sqlObjectType = requireNonNull(sqlObjectType, "sqlObjectType");
    }

    @Override
    public int delete(final T t) {
        this.log.debug("Deleting [" + t + "]");
        return inTransaction(s -> s.delete(requireNonNull(requireNonNull(t).getId())));
    }

    @Override
    public T get(final int id) {
        this.log.debug("Getting by id [" + id + "]");
        return inTransaction(s -> s.findById(id));
    }

    @Override
    public List<T> list() {
        this.log.debug("Getting all");
        return inTransaction(SqlObjectType::findAll);
    }

    @Override
    public Class<T> getGenericType() {
        return this.beanClass;
    }

    <K> K inTransaction(final Function<S, K> function){
        return this.dbi.inTransaction((conn, status) -> function.apply(conn.attach(this.sqlObjectType)));
    }

    public interface SqlObjectType<T extends AbstractModel> {

        int delete(int id);

        T findById(int id);

        List<T> findAll();
    }

}
