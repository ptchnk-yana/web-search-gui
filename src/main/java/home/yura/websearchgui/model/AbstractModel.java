package home.yura.websearchgui.model;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Created by yura on 26.02.17.
 */
public interface AbstractModel extends Serializable {

    @Nullable
    Integer getId();
}
