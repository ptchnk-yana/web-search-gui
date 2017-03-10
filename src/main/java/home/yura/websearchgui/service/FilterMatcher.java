package home.yura.websearchgui.service;

import home.yura.websearchgui.model.Filter;
import org.jsoup.nodes.Document;

/**
 * @author yuriy.dunko on 10.03.17.
 */
public interface FilterMatcher {

    Integer getMatchedItemId(Filter filter, Document document);

}
