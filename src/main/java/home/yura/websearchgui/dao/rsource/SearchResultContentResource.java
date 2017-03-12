package home.yura.websearchgui.dao.rsource;

import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;

/**
 * @author yuriy.dunko on 12.03.17.
 */
public interface SearchResultContentResource extends AbstractResource<SearchResult, SearchResultContent>{

    int BATCH_SIZE = 100;
}
