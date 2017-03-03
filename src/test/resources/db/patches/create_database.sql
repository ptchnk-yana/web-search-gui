CREATE TABLE search (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1024) NOT NULL DEFAULT '',
    url VARCHAR(256) NOT NULL
);

CREATE TABLE value_evaluation_definition (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    processing_type VARCHAR(24),
    result_processing_engine VARCHAR(24),
    expression VARCHAR(256),

    CHECK (
        processing_type IN ('EXTRACT_CONTENT', 'DELETE_CONTENT_PART')
        AND result_processing_engine IN ('REG_EXP', 'CSS_QUERY_SEARCH'))
);

CREATE TABLE value_evaluation_in_search_mapping (
    destination  VARCHAR(64) NOT NULL,
    position TINYINT UNSIGNED NOT NULL,
    value_evaluation_definition_id INT NOT NULL REFERENCES value_evaluation_definition (id),
    search_id INT NOT NULL REFERENCES search (id),

    UNIQUE KEY (destination, position, value_evaluation_definition_id, search_id)
);

CREATE TABLE filter (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1024) NOT NULL DEFAULT '',
    search_id INT NOT NULL REFERENCES search (id)
);

CREATE TABLE filter_item (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    filter_id INT NOT NULL REFERENCES filter (id),
    filter_location VARCHAR(16) NOT NULL,
    filter_engine VARCHAR(16) NOT NULL,
    filter_pre_formatting VARCHAR(16) NOT NULL,
    expression VARCHAR(256) NOT NULL,

    CHECK (
        filter_location IN ('URL', 'CONTENT')
        AND filter_engine IN ('REG_EXP', 'STRING_SEARCH')
        AND filter_pre_formatting IN ('NO', 'ESCAPE_HTML', 'ESCAPE_URL'))
);

CREATE TABLE result_entry_definition (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    search_id INT NOT NULL REFERENCES search (id),
    entry_block_location VARCHAR(256) NOT NULL
);

CREATE TABLE value_evaluation_in_result_entry_mapping (
    destination  VARCHAR(64) NOT NULL,
    position TINYINT UNSIGNED NOT NULL,
    value_evaluation_definition_id INT NOT NULL REFERENCES value_evaluation_definition (id),
    result_entry_definition_id INT NOT NULL REFERENCES result_entry_definition (id),

    UNIQUE KEY (destination, position, value_evaluation_definition_id, result_entry_definition_id)
);

CREATE TABLE search_result (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(256) NOT NULL DEFAULT '',
    result_entry_definition_id INT NOT NULL REFERENCES result_entry_definition (id),
    filter_item_id INT REFERENCES filter_item (id),
    internal_id BIGINT NOT NULL,
    viewed TINYINT(1) DEFAULT 0,

    UNIQUE KEY (internal_id),
    CHECK (viewed IN (0, 1))
);
