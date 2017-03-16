package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static home.yura.websearchgui.util.LocalFunctions.requireNonNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
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
                .setFilterItems(new ArrayList<>(Optional.ofNullable(getFilterItems()).orElse(emptyList())));
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

        public Builder addFilterItem(final FilterItem filterItem) {
            List<FilterItem> items = getFilterItems();
            if (items == null) {
                setFilterItems(items = new ArrayList<>());
            }
            items.add(requireNonNull(filterItem, "item"));
            return this;
        }

        public Filter build() {
            final Filter source = buildInternal();
            return new AutoValue_Filter.Builder(source)
                    .setFilterItems(unmodifiableList(firstNonNull(source.getFilterItems(), emptyList())))
                    .buildInternal();
        }

        abstract Filter buildInternal();

        @Nullable
        abstract List<FilterItem> getFilterItems();
    }
}
