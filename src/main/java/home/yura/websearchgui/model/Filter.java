package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * @author yura.dunko on 26.02.17.
 */
@AutoValue
public abstract class Filter implements AbstractNamedModel {

    public static Builder builder() {
        return new AutoValue_Filter.Builder();
    }

    public Builder buildNew() {
        return new AutoValue_Filter.Builder(this)
                .setFilterItems(new ArrayList<>(Optional.ofNullable(getFilterItems()).orElse(Collections.emptyList())));
    }

    public abstract Integer getSearchId();

    @Nullable
    public abstract List<FilterItem> getFilterItems();

    @AutoValue.Builder
    public static abstract class Builder {

        public abstract Builder setId(Integer id);

        public abstract Builder setName(String name);

        public abstract Builder setDescription(String description);

        public abstract Builder setSearchId(Integer searchId);

        @Nullable
        public abstract Builder setFilterItems(List<FilterItem> filterItems);

        public Builder addFilterItem(FilterItem filterItem) {
            List<FilterItem> items = getFilterItems();
            if (items == null) {
                setFilterItems(items = new ArrayList<>());
            }
            items.add(checkNotNull(filterItem, "item cannot be null"));
            return this;
        }

        public Filter build() {
            Filter source = buildInternal();
            return new AutoValue_Filter.Builder(source)
                    .setFilterItems(copyOf(firstNonNull(source.getFilterItems(), of())))
                    .buildInternal();
        }

        abstract Filter buildInternal();

        @Nullable
        abstract List<FilterItem> getFilterItems();
    }
}
