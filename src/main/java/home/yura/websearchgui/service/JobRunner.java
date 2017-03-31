package home.yura.websearchgui.service;

import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import org.apache.commons.lang3.tuple.Pair;
import org.easybatch.core.job.Job;

/**
 * @author yuriy.dunko on 16.03.17.
 */
public interface JobRunner {

    Job createSearchJob(Pair<Search, ResultEntryDefinition> searchTuple, int readLimit);

    Job createFilterJob(Pair<Search, ResultEntryDefinition> searchTuple);
}
