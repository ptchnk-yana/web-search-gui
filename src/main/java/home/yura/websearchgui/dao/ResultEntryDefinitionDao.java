package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.ResultEntryDefinition;

import java.util.List;

/**
 * @author yuriy.dunko on 27.02.17.
 */
public interface ResultEntryDefinitionDao extends AbstractDao<ResultEntryDefinition> {

    ResultEntryDefinition findBySearchId(final int searchId);
}
