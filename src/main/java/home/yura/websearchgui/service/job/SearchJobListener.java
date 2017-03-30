package home.yura.websearchgui.service.job;

import com.google.common.collect.ImmutableMap;
import home.yura.websearchgui.dao.LocalJobDao;
import home.yura.websearchgui.model.LocalJob;
import home.yura.websearchgui.model.Search;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easybatch.core.job.JobParameters;
import org.easybatch.core.job.JobReport;
import org.easybatch.core.job.JobStatus;
import org.easybatch.core.listener.JobListener;

import java.util.Map;
import java.util.Optional;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 16.03.17.
 */
public class SearchJobListener implements JobListener {

    private static final Log LOG = LogFactory.getLog(SearchJobListener.class);
    public static final String JOB_ID_METRIC = "job_id";

    private static final Map<JobStatus, LocalJob.Status> STATUS_MAP = ImmutableMap.of(
//            JobStatus.STARTING, LocalJob.Status.STARTED,
//            JobStatus.STARTED, LocalJob.Status.RUNNING,
//            JobStatus.STOPPING, LocalJob.Status.STARTED,
            JobStatus.COMPLETED, LocalJob.Status.FINISHED,
            JobStatus.FAILED, LocalJob.Status.FAILED
    );

    private final LocalJobDao jobDao;
    private final Search search;
    private final String jobName;
    private final Long requiredStep;

    private Integer jobId = null;

    public SearchJobListener(final LocalJobDao jobDao, final Search search, final String jobBaseName) {
        this.jobDao = requireNonNull(jobDao, "jobDao");
        this.search = requireNonNull(search, "search");
        this.jobName = getJobName(jobBaseName);
        this.requiredStep = Optional
                .ofNullable(jobDao.findLastRun(this.jobName, requireNonNull(this.search.getId(), "search.id")))
                .map(LocalJob::getFirstStep)
                .orElse(null);
    }

    @Override
    public void beforeJobStart(final JobParameters jobParameters) {
        final LocalJob job = this.jobDao.add(LocalJob.create(this.jobName, this.requiredStep, this.search.getId()));
        LOG.info("Starting job [" + job + "]");
        this.jobId = requireNonNull(job.getId(),"id");
    }

    @Override
    public void afterJobEnd(final JobReport jobReport) {
        LOG.info("Finishing job with id [" + this.jobId + "] with status [" + jobReport.getStatus() + "]");
        jobReport.getMetrics().addMetric(JOB_ID_METRIC, jobId);
        this.jobDao.getAndUpdateLocalJob(this.jobId,
                job -> requireNonNull(job, "job").copyWithStatus(STATUS_MAP.get(jobReport.getStatus())));
    }

    public Integer getJobId() {
        return this.jobId;
    }

    public Long getRequiredStep() {
        return this.requiredStep;
    }

    public String getJobName() {
        return this.jobName;
    }

    private static String getJobName(final String jobBaseName) {
        return (Search.class.getSimpleName() + "-" + requireNonNull(jobBaseName, "jobBaseName")).toLowerCase();
    }

}
