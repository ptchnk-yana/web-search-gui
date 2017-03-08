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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static home.yura.websearchgui.util.LocalFunctions.process;

/**
 * @author yura
 * @deprecated it use to much space :(
 */
public class SearchResultContentFileDao implements SearchResultContentDao {

    private final String encoding;
    private final File rootFolder;
    private final ExecutorService fileExecutor;
    private final Function<InputStream, InputStream> coverInput;
    private final Function<OutputStream, OutputStream> coverOutput;

    public static SearchResultContentFileDao plainFile(File rootFolder, ExecutorService fileExecutor, String encoding) {
        return new SearchResultContentFileDao(rootFolder, fileExecutor, encoding, is -> is, os -> os);
    }

    public static SearchResultContentFileDao gzipFile(File rootFolder, ExecutorService fileExecutor, String encoding) {
        return new SearchResultContentFileDao(rootFolder,
                fileExecutor,
                encoding,
                is -> process(() -> new GZIPInputStream(is), RuntimeException::new),
                os -> process(() -> new GZIPOutputStream(os), RuntimeException::new));
    }

    private SearchResultContentFileDao(File rootFolder,
                                      ExecutorService fileExecutor,
                                      String encoding,
                                      Function<InputStream, InputStream> coverInput,
                                      Function<OutputStream, OutputStream> coverOutput) {
        this.rootFolder = checkNotNull(rootFolder, "rootFolder cannot be null");
        this.fileExecutor = checkNotNull(fileExecutor, "fileExecutor cannot be null");
        this.encoding = checkNotNull(encoding, "encoding cannot be null");
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
        final Integer id = checkNotNull(content.getId(), "Content id cannot be null");
        final Future<?> submit = this.fileExecutor.submit(() -> {
            try (OutputStream out = this.coverOutput.apply(new FileOutputStream(getContentFile(id)))) {
                IOUtils.write(content.getContent(), out, this.encoding);
            } catch (IOException e) {
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
    public int delete(SearchResultContent content) {
        Future<Boolean> submit = this.fileExecutor.submit(() -> {
            File file = getContentFile(content.getId());
            checkState(file.delete(), "File " + file.getAbsolutePath() + " cannot be deleted");
            return true;
        });
        return process(submit::get, RuntimeException::new) ? 1 : 0;
    }

    @Override
    public Supplier<Integer> batchDelete(Function<Integer, List<Integer>> idSupplier) {
        final AtomicInteger count = new AtomicInteger();
        final List<Future<Boolean>> results = new ArrayList<>();
        int readNumber = 0;
        do {
            final List<Integer> list = idSupplier.apply(BATCH_SIZE);
            readNumber = list.size();
            results.add(this.fileExecutor.submit(() -> {
                list.forEach(i -> {
                    count.incrementAndGet();
                    File file = getContentFile(i);
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
    public SearchResultContent get(SearchResult definition) {
        return get(checkNotNull(checkNotNull(definition, "SearchResult is null").getId()));
    }

    @Override
    public SearchResultContent get(final int id) {
        Future<String> submit = this.fileExecutor.submit(() -> readFile(getContentFile(id)));
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
        return Arrays.stream(checkNotNull(this.rootFolder.listFiles()))
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

    private File getContentFile(Integer id) {
        return new File(this.rootFolder, String.valueOf(checkNotNull(id)));
    }

    private String readFile(File file) {
        try (InputStream is = this.coverInput.apply(new FileInputStream(file))) {
            return IOUtils.toString(is, this.encoding);
        } catch (IOException e) {
            throw new RuntimeException("Content " + file.getName() + " cannot be read", e);
        }
    }
}
