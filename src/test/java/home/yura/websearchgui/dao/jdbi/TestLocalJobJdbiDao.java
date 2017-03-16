package home.yura.websearchgui.dao.jdbi;

import home.yura.websearchgui.model.LocalJob;
import org.junit.Test;

import java.util.stream.IntStream;

import static home.yura.websearchgui.TestUtils.*;
import static home.yura.websearchgui.model.LocalJob.Status.FINISHED;
import static home.yura.websearchgui.model.LocalJob.Status.STARTED;
import static home.yura.websearchgui.model.LocalJob.create;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Test for {@ling home.yura.websearchgui.dao.jdbi.LocalJobJdbiDao}
 * @author yuriy.dunko on 15.03.17.
 */
public class TestLocalJobJdbiDao extends AbstractJdbiTest {

    @Test
    public void testAdd() throws Exception {
        final LocalJob source = create(randomString(), null, randomInt());
        final LocalJob added = this.localJobJdbiDao.add(source);
        assertThat(added, notNullValue());
        assertThat(added.getId(), notNullValue());
        assertThat(added, equalTo(source.copyWithId(added.getId())));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddDuplicate() throws Exception {
        final LocalJob source = create(randomString(), 1L, randomInt());
        this.localJobJdbiDao.add(source);
        this.localJobJdbiDao.add(source);
    }

    @Test(expected = IllegalStateException.class)
    public void testAddForNotFinished() throws Exception {
        final String name = randomString();
        final int destinationId = randomInt();
        final long requiredStep = 1L;
        this.localJobJdbiDao.add(create(name, requiredStep, destinationId).copyWithRunningStatus(null, null));
        this.localJobJdbiDao.add(create(name, 2L, destinationId));
    }

    @Test
    public void testGetAndUpdateLocalJob() throws Exception {
        final LocalJob added = this.localJobJdbiDao.add(create(randomString(), null, randomInt()));
        final boolean updateResult = this.localJobJdbiDao.getAndUpdateLocalJob(
                requireNonNull(added.getId()),
                job -> job.copyWithRunningStatus(randomLong(), randomLong()));
        assertThat(updateResult, is(true));
    }

    @Test
    public void testUpdateLocalJobStatus() throws Exception {
        final LocalJob added = this.localJobJdbiDao.add(create(randomString(), null, randomInt()));
        assertThat(added.getStatus(), not(equalTo(FINISHED)));

        final boolean updated = this.localJobJdbiDao.updateLocalJobStatus(requireNonNull(added.getId()), FINISHED);
        assertThat(updated, is(true));
        assertThat(added.copyWithStatus(FINISHED), equalTo(this.localJobJdbiDao.findLastRun(added.getName(), added.getDestinationId())));
        assertThat(this.localJobJdbiDao.updateLocalJobStatus(requireNonNull(added.getId()), FINISHED), is(false));
    }

    @Test
    public void testFindLastRun() throws Exception {
        final String name = randomString().substring(0, 36);
        final int destinationId = randomInt();

        final LocalJob first = this.localJobJdbiDao.add(create(null, name, 3L, 4L, null, destinationId, FINISHED));
        final LocalJob second = this.localJobJdbiDao.add(create(null, name, 2L, 3L, 3L, destinationId, FINISHED));
        final LocalJob third = this.localJobJdbiDao.add(create(null, name, 1L, 2L, 2L, destinationId, STARTED));

        assertThat(this.localJobJdbiDao.findLastRun(name, destinationId), equalTo(second));
    }

    @Test
    public void testFindNullLastRun() throws Exception {
        final String name = randomString().substring(0, 36);
        final int destinationId = randomInt();

        assertThat(this.localJobJdbiDao.findLastRun(name, destinationId), nullValue());
    }

    @Test
    public void testFindAll() throws Exception {
        final LocalJob[] array = IntStream.range(0, 3)
                .mapToObj(i -> this.localJobJdbiDao.add(create(randomString(), null, i)))
                .toArray(LocalJob[]::new);
        assertThat(this.localJobJdbiDao.findAll(), containsInAnyOrder(array));
    }

}