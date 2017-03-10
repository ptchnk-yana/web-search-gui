package home.yura.websearchgui.dao.file;

import com.google.common.primitives.Ints;
import home.yura.websearchgui.dao.SearchResultContentDao;
import home.yura.websearchgui.model.SearchResult;
import home.yura.websearchgui.model.SearchResultContent;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Preconditions.checkState;
import static home.yura.websearchgui.util.LocalFunctions.process;
import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;

/**
 * @author yura
 */
public class SearchResultContentFileDao implements SearchResultContentDao {

    private final String encoding;
    private final File rootFolder;
    private final ExecutorService fileExecutor;
    private final Function<InputStream, InputStream> coverInput;
    private final Function<OutputStream, OutputStream> coverOutput;

    public static SearchResultContentFileDao plainFile(final File rootFolder,
                                                       final ExecutorService fileExecutor,
                                                       final String encoding) {
        return new SearchResultContentFileDao(rootFolder, fileExecutor, encoding, is -> is, os -> os);
    }

    public static SearchResultContentFileDao gzipFile(final File rootFolder,
                                                      final ExecutorService fileExecutor,
                                                      final String encoding) {
        return new SearchResultContentFileDao(rootFolder,
                fileExecutor,
                encoding,
                is -> process(() -> new GZIPInputStream(is), RuntimeException::new),
                os -> process(() -> new GZIPOutputStream(os), RuntimeException::new));
    }

    private SearchResultContentFileDao(final File rootFolder,
                                       final ExecutorService fileExecutor,
                                       final String encoding,
                                       final Function<InputStream, InputStream> coverInput,
                                       final Function<OutputStream, OutputStream> coverOutput) {
        this.rootFolder = requireNonNull(rootFolder, "rootFolder");
        this.fileExecutor = requireNonNull(fileExecutor, "fileExecutor");
        this.encoding = requireNonNull(encoding, "encoding");
        this.coverInput = coverInput;
        this.coverOutput = coverOutput;

        if (rootFolder.exists()) {
            checkState(rootFolder.isDirectory(), "root folder [%s] should be directory", rootFolder);
        } else {
            checkState(rootFolder.mkdirs(), "root folder [%s] cannot be created", rootFolder);
        }
    }

    @Override
    public SearchResultContent add(final SearchResultContent content) {
        final Integer id = requireNonNull(content.getId(), "Content id");
        final Future<?> submit = this.fileExecutor.submit(() -> {
            try (OutputStream out = this.coverOutput.apply(new FileOutputStream(getContentFile(id)))) {
                IOUtils.write(content.getContent(), out, this.encoding);
            } catch (final IOException e) {
                throw new RuntimeException("Content " + id + " cannot be stored", e);
            }
        });

        return new SearchResultContent() {
            @Override
            public int getSearchResultId() {
                process(submit::get, RuntimeException::new);
                return id;
            }

            @Override
            public String getContent() {
                process(submit::get, RuntimeException::new);
                return content.getContent();
            }
        };
    }

    @Override
    public int delete(final SearchResultContent content) {
        final Future<Boolean> submit = this.fileExecutor.submit(() -> {
            final File file = getContentFile(content.getId());
            checkState(file.delete(), "File " + file.getAbsolutePath() + " cannot be deleted");
            return true;
        });
        return process(submit::get, RuntimeException::new) ? 1 : 0;
    }

    @Override
    public Supplier<Integer> batchDelete(final Function<Integer, List<Integer>> idSupplier) {
        final AtomicInteger count = new AtomicInteger();
        final List<Future<Boolean>> results = new ArrayList<>();
        int readNumber = 0;
        do {
            final List<Integer> list = idSupplier.apply(BATCH_SIZE);
            readNumber = list.size();
            results.add(this.fileExecutor.submit(() -> {
                list.forEach(i -> {
                    count.incrementAndGet();
                    final File file = getContentFile(i);
                    checkState(file.delete(), "File " + file.getAbsolutePath() + " cannot be deleted");
                });
                return true;
            }));
        } while (readNumber == BATCH_SIZE);

        return () -> {
            results.forEach(submit -> process(submit::get, RuntimeException::new));
            return count.get();
        };
    }

    @Override
    public SearchResultContent get(final SearchResult definition) {
        return get(requireNonNull(requireNonNull(definition, "SearchResult").getId()));
    }

    @Override
    public SearchResultContent get(final int id) {
        final Future<String> submit = this.fileExecutor.submit(() -> readFile(getContentFile(id)));
        return new SearchResultContent() {
            @Override
            public int getSearchResultId() {
                return id;
            }

            @Override
            public String getContent() {
                return process(submit::get, RuntimeException::new);
            }
        };
    }

    @Override
    public List<SearchResultContent> list() {
        return Arrays.stream(requireNonNull(this.rootFolder.listFiles()))
                .filter(file -> Ints.tryParse(file.getName()) != null)
                .map(file -> new SearchResultContent() {
                    String content = null;

                    @Override
                    public int getSearchResultId() {
                        return Integer.parseInt(file.getName());
                    }

                    @Override
                    public String getContent() {
                        return content != null
                                ? content
                                : (content = process(() -> fileExecutor.submit(() -> readFile(file)).get(), RuntimeException::new));
                    }
                }).collect(Collectors.toList());
    }

    @Override
    public Class<SearchResultContent> getGenericType() {
        return SearchResultContent.class;
    }

    private File getContentFile(final Integer id) {
        return new File(this.rootFolder, String.valueOf(requireNonNull(id)));
    }

    private String readFile(final File file) {
        try (InputStream is = this.coverInput.apply(new FileInputStream(file))) {
            return IOUtils.toString(is, this.encoding);
        } catch (final IOException e) {
            throw new RuntimeException("Content " + file.getName() + " cannot be read", e);
        }
    }
}
