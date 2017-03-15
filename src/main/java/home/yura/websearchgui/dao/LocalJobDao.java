package home.yura.websearchgui.dao;

import home.yura.websearchgui.model.LocalJob;

import java.util.List;
import java.util.function.Function;

/**
 * @author yuriy.dunko on 14.03.17.
 */
public interface LocalJobDao {

    /**
     * Try to add job into the data storage. In case if data cannot be added {@link IllegalStateException} will be thrown
     *
     * @param job the job which should be added
     * @return the same job with updated {@code id}
     * @throws IllegalStateException in case if job cannot be added
     */
    LocalJob add(LocalJob job);

    /**
     * Update the job
     *
     * @param jobId          the id of the job which should be updated
     * @param modifyFunction function for making updates in database object
     * @return {@code true} if the job was updated and {@code false} otherwise
     */
    boolean getAndUpdateLocalJob(int jobId, Function<LocalJob, LocalJob> modifyFunction);

    boolean updateLocalJobStatus(int id, LocalJob.Status status);

    /**
     * Looking for the last {@link home.yura.websearchgui.model.LocalJob.Status#FINISHED} job with provided parameters
     *
     * @param name          the name of the job
     * @param destinationId the destination id of the job
     * @return the most recent job if it present and {@code null} otherwise
     */
    LocalJob findLastRun(String name, int destinationId);

    /**
     * Finds all jobs
     *
     * @return all jobs
     */
    List<LocalJob> findAll();
}
