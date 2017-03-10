package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.model.AbstractModel;
import org.skife.jdbi.v2.DBI;

import java.util.List;
import java.util.function.Function;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

public abstract class AbstractJdbiResourceDao<T extends AbstractModel, S extends AbstractJdbiResourceDao.SqlObjectType<T>>
        extends AbstractJdbiDao<T> {

    private final Class<T> beanClass;
    private final Class<S> sqlObjectType;

    AbstractJdbiResourceDao(final DBI dbi, final Class<T> beanClass, final Class<S> sqlObjectType) {
        super(dbi);
        this.beanClass = requireNonNull(beanClass, "beanClass");
        this.sqlObjectType = requireNonNull(sqlObjectType, "sqlObjectType");
    }

    @Override
    public int delete(final T t) {
        return handle(s -> s.delete(requireNonNull(requireNonNull(t).getId())));
    }

    @Override
    public T get(final int id) {
        return handle(s -> s.findById(id));
    }

    @Override
    public List<T> list() {
        return handle(SqlObjectType::findAll);
    }

    @Override
    public Class<T> getGenericType() {
        return this.beanClass;
    }

    <K> K handle(final Function<S, K> function){
        return this.dbi.inTransaction((conn, status) -> function.apply(conn.attach(this.sqlObjectType)));
    }

    public interface SqlObjectType<T extends AbstractModel> {

        int delete(int id);

        T findById(int id);

        List<T> findAll();
    }

}
