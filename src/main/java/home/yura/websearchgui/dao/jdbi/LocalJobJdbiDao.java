package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.dao.LocalJobDao;
import home.yura.websearchgui.model.LocalJob;
import home.yura.websearchgui.util.LocalJdbis;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * @author yuriy.dunko on 14.03.17.
 */
public class LocalJobJdbiDao implements LocalJobDao {
    private final DBI dbi;

    public LocalJobJdbiDao(final DBI dbi) {
        this.dbi = Objects.requireNonNull(dbi, "DBI");
    }

    @Override
    public LocalJob add(final LocalJob job) {
        return ofNullable(this.dbi.inTransaction((conn, s) -> conn.attach(SearchResultJdbiResource.class).insert(job)))
                .map(job::copyWithId)
                .orElseThrow(() -> new IllegalStateException("Cannot add " + job));
    }

    @Override
    public boolean getAndUpdateLocalJob(final int jobId, final Function<LocalJob, LocalJob> modifyFunction) {
        return this.dbi.inTransaction(((conn, s) -> {
            final SearchResultJdbiResource resource = conn.attach(SearchResultJdbiResource.class);
            return resource.update(modifyFunction.apply(resource.selectForUpdate(jobId)));
        })) > 0;
    }

    @Override
    public boolean updateLocalJobStatus(final int id, final LocalJob.Status status) {
        return this.dbi.inTransaction((conn, s) -> conn.attach(SearchResultJdbiResource.class).updateStatus(id, status)) > 0;
    }

    @Override
    public LocalJob findLastRun(final String name, final int destinationId) {
        return this.dbi.withHandle(handle -> handle.attach(SearchResultJdbiResource.class).findLastRun(name, destinationId));
    }

    @Override
    public List<LocalJob> findAll() {
        return this.dbi.withHandle(handle -> handle.attach(SearchResultJdbiResource.class).findAll());
    }

    @RegisterMapper(LocalJobJdbiMapper.class)
    public interface SearchResultJdbiResource {

        @SqlUpdate("INSERT INTO local_job (                                             " +
                "   name, first_step, last_step, required_step, destination_id, status) " +
                "SELECT * FROM (                                                        " +
                "   SELECT                                                              " +
                "       CAST(:j.name AS CHAR(40)) AS name,                              " +
                "       CAST(:j.firstStep AS SIGNED) AS first_step,                     " +
                "       CAST(:j.lastStep AS SIGNED) AS last_step,                       " +
                "       CAST(:j.requiredStep AS SIGNED) AS required_step,               " +
                "       CAST(:j.destinationId AS SIGNED) AS destination_id,             " +
                "       CAST(:j.status AS CHAR(8)) AS status) AS tmp                    " +
                "WHERE NOT EXISTS (                                                     " +
                "   SELECT *                                                            " +
                "   FROM local_job                                                      " +
                "   WHERE name = :j.name                                                " +
                "       AND destination_id = :j.destinationId                           " +
                "       AND (required_step = :j.requiredStep                            " +
                "           OR status IN ('STARTED', 'RUNNING')))                       ")
        @GetGeneratedKeys
        Integer insert(@BindBean("j") LocalJob j);

        @SqlUpdate("UPDATE local_job                    " +
                "SET                                    " +
                "   name = :j.name,                     " +
                "   first_step = :j.firstStep,          " +
                "   last_step = :j.lastStep,            " +
                "   required_step = :j.requiredStep,    " +
                "   destination_id = :j.destinationId,  " +
                "   status = :j.status                  " +
                "WHERE                                  " +
                "   id = :j.id                          ")
        int update(@BindBean("j") LocalJob j);

        @SqlUpdate("UPDATE local_job SET status = :status WHERE id = :id AND status != :status")
        int updateStatus(@Bind("id") int id, @Bind("status") LocalJob.Status status);

        @SqlQuery("SELECT                                                                   " +
                "   id, name, first_step, last_step, required_step, destination_id, status  " +
                "FROM                                                                       " +
                "   local_job                                                               " +
                "WHERE                                                                      " +
                "   name = :name                                                            " +
                "   AND destination_id = :destinationId                                     " +
                "   AND status IN ('FINISHED', 'FAILED')                                    " +
                "ORDER BY first_step ASC                                                    " +
                "LIMIT 1                                                                    ")
        LocalJob findLastRun(@Bind("name") String name, @Bind("destinationId") int destinationId);

        @SqlQuery("SELECT                                                                   " +
                "   id, name, first_step, last_step, required_step, destination_id, status  " +
                "FROM                                                                       " +
                "   local_job                                                               ")
        List<LocalJob> findAll();

        @SqlQuery("SELECT                                                                   " +
                "   id, name, first_step, last_step, required_step, destination_id, status  " +
                "FROM                                                                       " +
                "   local_job                                                               " +
                "WHERE                                                                      " +
                "   id = :id                                                                " +
                "FOR UPDATE                                                                 ")
        LocalJob selectForUpdate(@Bind("id") int id);
    }

    public static class LocalJobJdbiMapper implements ResultSetMapper<LocalJob> {
        @Override
        public LocalJob map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException {
            return LocalJob.create(
                    r.getInt("id"),
                    r.getString("name"),
                    LocalJdbis.extractLongValue(r, "first_step", null),
                    LocalJdbis.extractLongValue(r, "last_step", null),
                    LocalJdbis.extractLongValue(r, "required_step", null),
                    r.getInt("destination_id"),
                    Enum.valueOf(LocalJob.Status.class, r.getString("status")));
        }
    }
}
