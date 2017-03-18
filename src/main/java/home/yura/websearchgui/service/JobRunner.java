package home.yura.websearchgui.service;

import home.yura.websearchgui.model.ResultEntryDefinition;
import home.yura.websearchgui.model.Search;
import home.yura.websearchgui.util.bean.BiTuple;
import org.easybatch.core.job.Job;

/**
 * @author yuriy.dunko on 16.03.17.
 */
public interface JobRunner {

    Job createSearchJob(BiTuple<Search, ResultEntryDefinition> searchTuple, int readLimit);

    Job createFilterJob(BiTuple<Search, ResultEntryDefinition> searchTuple);
}
