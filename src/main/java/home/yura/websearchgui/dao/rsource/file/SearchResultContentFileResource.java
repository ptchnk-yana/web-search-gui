package home.yura.websearchgui.dao.rsource.file;

import home.yura.websearchgui.dao.rsource.SearchResultContentResource;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Preconditions.checkState;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yuriy.dunko on 12.03.17.
 */
public class SearchResultContentFileResource implements SearchResultContentResource {

    private final String encoding;
    private final File rootFolder;
    private final ExecutorService fileExecutor;
    private final Function<InputStream, InputStream> coverInput;
    private final Function<OutputStream, OutputStream> coverOutput;

    public static SearchResultContentFileResource plainFiles(final String encoding,
                                                             final File rootFolder,
                                                             final ExecutorService fileExecutor) {
        return new SearchResultContentFileResource(encoding, rootFolder, fileExecutor, is -> is, os -> os);
    }

    public static SearchResultContentFileResource gzipFiles(final String encoding,
                                                            final File rootFolder,
                                                            final ExecutorService fileExecutor) {
        return new SearchResultContentFileResource(encoding, rootFolder, fileExecutor,
                is -> process(() -> new GZIPInputStream(is)), os -> process(() -> new GZIPOutputStream(os)));
    }

    private SearchResultContentFileResource(final String encoding,
                                            final File rootFolder,
                                            final ExecutorService fileExecutor,
                                            final Function<InputStream, InputStream> coverInput,
                                            final Function<OutputStream, OutputStream> coverOutput) {
        this.encoding = requireNonNull(encoding, "encoding");
        this.rootFolder = requireNonNull(rootFolder, "rootFolder");
        this.fileExecutor = requireNonNull(fileExecutor, "fileExecutor");
        this.coverInput = requireNonNull(coverInput);
        this.coverOutput = requireNonNull(coverOutput);

        if (rootFolder.exists()) {
            checkState(rootFolder.isDirectory(), "root folder [%s] should be directory", rootFolder);
        } else {
            checkState(rootFolder.mkdirs(), "root folder [%s] cannot be created", rootFolder);
        }
    }

    @Override
    public Future<Void> add(final SearchResult searchResult, final SearchResultContent content) {
        return this.fileExecutor.submit(() -> {
            final File contentFile = getContentFile(searchResult);
            try (final OutputStream out = outputStream(contentFile)) {
                IOUtils.write(content.getContent(), out, this.encoding);
                return null;
            } catch (final IOException e) {
                throw new RuntimeException("Content " + contentFile + " cannot be stored", e);
            }
        });
    }

    @Override
    public Future<Void> addBatch(final Collection<Pair<SearchResult, SearchResultContent>> batch) {
        return new Future<Void>() {
            final List<Future<Void>> futures = requireNonNull(batch, "batch")
                    .stream()
                    .map(tuple -> add(tuple.getLeft(), tuple.getRight()))
                    .collect(Collectors.toList());

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return this.futures.stream().map(future -> future.cancel(mayInterruptIfRunning)).reduce((b1, b2) -> b1 && b2).orElse(true);
            }

            @Override
            public boolean isCancelled() {
                return this.futures.stream().map(Future::isCancelled).reduce((b1, b2) -> b1 && b2).orElse(false);
            }

            @Override
            public boolean isDone() {
                return this.futures.stream().map(Future::isDone).reduce((b1, b2) -> b1 && b2).orElse(true);
            }

            @Override
            public Void get() throws InterruptedException, ExecutionException {
                this.futures.forEach(f -> process(f::get));
                return null;
            }

            @Override
            public Void get(final long timeout, @SuppressWarnings("NullableProblems") final TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return this.futures.stream().map(f -> process(() -> f.get(timeout, unit))).reduce((v1, v2) -> v1).orElse(null);
            }
        };
    }

    @Override
    public Future<Boolean> delete(final SearchResult m) {
        return this.fileExecutor.submit(() -> getContentFile(m).delete());
    }

    @Override
    public Future<Optional<SearchResultContent>> get(final SearchResult searchResult) {
        final File contentFile = getContentFile(searchResult);
        return this.fileExecutor.submit(() -> {
            try (final InputStream is = this.coverInput.apply(new FileInputStream(contentFile))) {
                return Optional.of(SearchResultContent.create(
                        ObjectUtils.firstNonNull(searchResult.getId(), 0),
                        IOUtils.toString(is, this.encoding)));
            } catch (final FileNotFoundException ef) {
                return Optional.empty();
            } catch (final IOException e) {
                throw new RuntimeException("Content " + contentFile.getName() + " cannot be read", e);
            }
        });
    }

    private File getContentFile(final SearchResult searchResult) {
        return new File(this.rootFolder, requireNonNull(requireNonNull(searchResult).getResultEntryDefinitionId()) +
                "_" + requireNonNull(searchResult.getInternalId()));
    }

    private OutputStream outputStream(final File file) throws FileNotFoundException {
        return this.coverOutput.apply(new FileOutputStream(file));
    }
}
