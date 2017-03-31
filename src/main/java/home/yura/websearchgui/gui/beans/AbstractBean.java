package home.yura.websearchgui.gui.beans;

import home.yura.websearchgui.model.AbstractModel;

/**
 * @author yuriy.dunko on 31.03.17.
 */
public interface AbstractBean<T extends AbstractModel> {
    T toModel();
}
