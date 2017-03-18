package home.yura.websearchgui.service.job;

import com.google.common.collect.Streams;
import home.yura.websearchgui.dao.LocalJobDao;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import home.yura.websearchgui.util.bean.BiTuple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easybatch.core.listener.BatchListener;
import org.easybatch.core.record.Batch;
import org.easybatch.core.record.Record;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.of;
import static home.yura.websearchgui.model.LocalJob.Status.FAILED;
import static home.yura.websearchgui.model.LocalJob.Status.FINISHED;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 16.03.17.
 */
public class SearchBatchListener implements BatchListener {
    private static final Log LOG = LogFactory.getLog(SearchBatchListener.class);

    private final LocalJobDao jobDao;
    private final Supplier<Integer> jobIdSupplier;

    public SearchBatchListener(final LocalJobDao jobDao, final Supplier<Integer> jobIdSupplier) {
        this.jobDao = requireNonNull(jobDao, "jobDao");
        this.jobIdSupplier = requireNonNull(jobIdSupplier, "jobIdSupplier");
    }

    @Override
    public void beforeBatchReading() {
        LOG.info("Starting processing a batch");
    }

    @Override
    public void afterBatchProcessing(final Batch batch) {
        LOG.info("A batch [" + batch.size() + "] was processed");
    }

    @Override
    public void afterBatchWriting(final Batch batch) {
        LOG.info("Batch [" + batch.size() + "] was written");
        @SuppressWarnings("unchecked")
        final long[] steps = Streams.stream(batch.iterator())
                .flatMap(listRecord -> ((Record<List<Future<BiTuple<SearchResult, SearchResultContent>>>>) listRecord).getPayload().stream())
                .map(biTupleFuture -> process(biTupleFuture::get).getFirst().getInternalId())
                .collect(Collector.of(() -> new long[]{0L, 0L},
                        (array, internalId) -> {
                            if (array[0] > internalId) {
                                array[0] = internalId;
                            }
                            if (array[1] < internalId) {
                                array[1] = internalId;
                            }
                        },
                        (array1, array2) -> {
                            Arrays.setAll(array1, i -> array1[i] + array2[i]);
                            return array1;
                        }));
        this.jobDao.getAndUpdateLocalJob(this.jobIdSupplier.get(), job -> {
            checkState(!of(FINISHED, FAILED).contains(requireNonNull(job, "job").getStatus()),
                    "Job [%s] was terminated", job.getName());
            return job.copyWithRunningStatus(
                    Optional.ofNullable(job.getFirstStep()).orElse(steps[0]),
                    Optional.ofNullable(job.getLastStep()).orElse(steps[1]));
        });
    }

    @Override
    public void onBatchWritingException(final Batch batch, final Throwable throwable) {
    }
}
