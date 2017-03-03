package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.AbstractModel;

import java.util.List;

/**
 * @author yura on 26.02.17.
 */
public interface AbstractDao<T extends AbstractModel> {

    T add(T t);

    int delete(T t);

    T get(int id);

    List<T> list();

    Class<T> getGenericType();
}
