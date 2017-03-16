package home.yura.websearchgui.model;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * @author yuriy.dunko on 26.02.17.
 */
public interface AbstractModel extends Serializable {

    long NULL_SIMPLE_VALUE = 0L;

    @Nullable
    Integer getId();
}
