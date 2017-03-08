package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;
import home.yura.websearchgui.util.LocalCollections;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.ImmutableMap.copyOf;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * @author yuriy.dunko on 26.02.17.
 */
@AutoValue
public abstract class ResultEntryDefinition implements AbstractModel {

    public static ResultEntryDefinition create(final Integer id,
                                               final Integer searchId,
                                               final String entryBlockLocation,
                                               final Map<Integer, ValueEvaluationDefinition> nameExtractionChain,
                                               final Map<Integer, ValueEvaluationDefinition> contentLinkExtractionChain,
                                               final Map<Integer, ValueEvaluationDefinition> internalIdExtractionChain,
                                               final Map<Integer, ValueEvaluationDefinition> contentExtractionChain) {
        return new AutoValue_ResultEntryDefinition(id, searchId, entryBlockLocation,
                copyOf(firstNonNull(nameExtractionChain, new HashMap<>())),
                copyOf(firstNonNull(contentLinkExtractionChain, new HashMap<>())),
                copyOf(firstNonNull(internalIdExtractionChain, new HashMap<>())),
                copyOf(firstNonNull(contentExtractionChain, new HashMap<>())));
    }

    public static ResultEntryDefinition merge(final ResultEntryDefinition left, final ResultEntryDefinition right) {
        checkArgument(Objects.equals(left.getId(), right.getId()), "id should be equal");
        checkArgument(Objects.equals(left.getSearchId(), right.getSearchId()), "searchId should be equal");
        checkArgument(Objects.equals(left.getEntryBlockLocation(), right.getEntryBlockLocation()), "entryBlockLocation should be equal");
        return create(left.getId(),
                left.getSearchId(),
                left.getEntryBlockLocation(),
                LocalCollections.merge(left.getNameExtractionChain(), right.getNameExtractionChain()),
                LocalCollections.merge(left.getContentLinkExtractionChain(), right.getContentLinkExtractionChain()),
                LocalCollections.merge(left.getInternalIdExtractionChain(), right.getInternalIdExtractionChain()),
                LocalCollections.merge(left.getContentExtractionChain(), right.getContentExtractionChain()));
    }

    public abstract Integer getSearchId();

    /**
     * Define a CssQuery with multiple result where each entry is an information block.
     * All extractions are applied on this block.
     * This value can be used as a preview for {@link SearchResult#getDescription()}
     *
     * @return CssQuery with multiple result where each entry is an information block
     * @see org.jsoup.nodes.Element
     */
    public abstract String getEntryBlockLocation();

    public abstract Map<Integer, ValueEvaluationDefinition> getNameExtractionChain();

    public abstract Map<Integer, ValueEvaluationDefinition> getContentLinkExtractionChain();

    public abstract Map<Integer, ValueEvaluationDefinition> getInternalIdExtractionChain();

    /**
     * When you followed content link you get some HTML, this chain describe how to optimise (make smaller) it
     */
    public abstract Map<Integer, ValueEvaluationDefinition> getContentExtractionChain();

}
